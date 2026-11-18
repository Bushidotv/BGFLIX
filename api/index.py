from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from datetime import datetime, timedelta, timezone
import os
import uuid

from supabase import create_client, Client

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

SUPABASE_URL: str = os.environ.get("SUPABASE_URL", "")
SUPABASE_KEY: str = os.environ.get("SUPABASE_KEY", "")

supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)


def get_now_utc() -> datetime:
    return datetime.now(timezone.utc)


def generate_trial_key(hwid: str) -> str:
    """TRIAL-<ilk 6 karakter hwid> formatında benzersiz anahtar üret."""
    prefix = hwid[:6].upper().replace("-", "")
    suffix = uuid.uuid4().hex[:4].upper()
    return f"TRIAL-{prefix}-{suffix}"


# ---------------------------------------------------------------------------
# HELPER: lisans kaydını hwid ile ara
# ---------------------------------------------------------------------------
def find_license_by_hwid(hwid: str):
    """licenses tablosunda device_ids dizisinde hwid geçen kaydı bul."""
    result = supabase.table("licenses").select("*").contains("device_ids", [hwid]).execute()
    if result.data:
        return result.data[0]
    return None


def find_license_by_key(key: str):
    result = supabase.table("licenses").select("*").eq("license_key", key).execute()
    if result.data:
        return result.data[0]
    return None


# ---------------------------------------------------------------------------
# HELPER: deneme süresi oluştur
# ---------------------------------------------------------------------------
def create_trial(hwid: str) -> dict:
    now = get_now_utc()
    expire = now + timedelta(days=30)
    trial_key = generate_trial_key(hwid)

    record = {
        "license_key": trial_key,
        "status": "active",
        "expire_date": expire.isoformat(),
        "device_ids": [hwid],
        "is_trial": True,
        "created_at": now.isoformat(),
    }

    supabase.table("licenses").insert(record).execute()
    return record


# ---------------------------------------------------------------------------
# ANA DOĞRULAMA ENDPOINTİ
# ---------------------------------------------------------------------------
@app.post("/api/validate")
@app.get("/api/validate")
async def validate_license(request: Request):
    """
    CloudStream eklentisinden gelen lisans doğrulama isteği.

    Beklenen parametreler (query string veya JSON body):
      - license_key : str  (boş/geçersiz olabilir)
      - hwid        : str  (cihaz kimliği — zorunlu)
    """
    # Parametreleri hem query string hem body'den al
    params = dict(request.query_params)
    try:
        body = await request.json()
        params.update(body)
    except Exception:
        pass

    hwid: str = params.get("hwid", "").strip()
    license_key: str = params.get("license_key", "").strip()

    if not hwid:
        return JSONResponse(
            status_code=400,
            content={"valid": False, "message": "HWID gerekli."}
        )

    now = get_now_utc()

    # ------------------------------------------------------------------
    # 1. Geçerli bir license_key geldiyse önce key ile ara
    # ------------------------------------------------------------------
    if license_key and not license_key.startswith("TRIAL-"):
        record = find_license_by_key(license_key)
        if record:
            # HWID bu kayıtta kayıtlı değilse ekle (yeni cihaz ekleme)
            device_ids: list = record.get("device_ids") or []
            if hwid not in device_ids:
                device_ids.append(hwid)
                supabase.table("licenses").update({"device_ids": device_ids}).eq("license_key", license_key).execute()

            status = record.get("status", "")
            expire_raw = record.get("expire_date")

            if status != "active":
                return JSONResponse(content={
                    "valid": False,
                    "message": f"Lisans durumu: {status}",
                    "status": status
                })

            if expire_raw:
                expire_dt = datetime.fromisoformat(expire_raw.replace("Z", "+00:00"))
                if expire_dt.tzinfo is None:
                    expire_dt = expire_dt.replace(tzinfo=timezone.utc)
                if now > expire_dt:
                    # Expire olmuş — durumu güncelle
                    supabase.table("licenses").update({"status": "expired"}).eq("license_key", license_key).execute()
                    return JSONResponse(content={
                        "valid": False,
                        "message": "Lisans süresi dolmuş.",
                        "status": "expired"
                    })

            return JSONResponse(content={
                "valid": True,
                "message": "Lisans geçerli.",
                "license_key": license_key,
                "is_trial": record.get("is_trial", False),
                "expire_date": expire_raw,
                "status": "active"
            })

    # ------------------------------------------------------------------
    # 2. Boş/geçersiz key ya da TRIAL key → HWID ile ara
    # ------------------------------------------------------------------
    record = find_license_by_hwid(hwid)

    if record:
        # HWID kayıtlı — trial veya normal lisans
        status = record.get("status", "")
        expire_raw = record.get("expire_date")
        is_trial = record.get("is_trial", False)
        key = record.get("license_key", "")

        if status != "active":
            return JSONResponse(content={
                "valid": False,
                "is_trial": is_trial,
                "message": "Deneme süreniz dolmuştur. Lisans satın alın." if is_trial else f"Lisans durumu: {status}",
                "status": status,
                "license_key": key
            })

        if expire_raw:
            expire_dt = datetime.fromisoformat(expire_raw.replace("Z", "+00:00"))
            if expire_dt.tzinfo is None:
                expire_dt = expire_dt.replace(tzinfo=timezone.utc)
            if now > expire_dt:
                supabase.table("licenses").update({"status": "expired"}).eq("license_key", key).execute()
                return JSONResponse(content={
                    "valid": False,
                    "is_trial": is_trial,
                    "message": "Deneme süreniz dolmuştur. Lisans satın alın." if is_trial else "Lisans süresi dolmuş.",
                    "status": "expired",
                    "license_key": key
                })

        days_left = None
        if expire_raw:
            expire_dt = datetime.fromisoformat(expire_raw.replace("Z", "+00:00"))
            if expire_dt.tzinfo is None:
                expire_dt = expire_dt.replace(tzinfo=timezone.utc)
            days_left = (expire_dt - now).days

        return JSONResponse(content={
            "valid": True,
            "message": f"Deneme süresi aktif. {days_left} gün kaldı." if is_trial else "Lisans geçerli.",
            "license_key": key,
            "is_trial": is_trial,
            "expire_date": expire_raw,
            "days_left": days_left,
            "status": "active"
        })

    # ------------------------------------------------------------------
    # 3. HWID hiç kayıtlı değil → 30 günlük deneme oluştur
    # ------------------------------------------------------------------
    trial = create_trial(hwid)
    expire_dt = datetime.fromisoformat(trial["expire_date"].replace("Z", "+00:00"))
    if expire_dt.tzinfo is None:
        expire_dt = expire_dt.replace(tzinfo=timezone.utc)
    days_left = (expire_dt - now).days

    return JSONResponse(content={
        "valid": True,
        "message": f"30 günlük deneme süresi başlatıldı. {days_left} gün kaldı.",
        "license_key": trial["license_key"],
        "is_trial": True,
        "expire_date": trial["expire_date"],
        "days_left": days_left,
        "status": "active"
    })


# ---------------------------------------------------------------------------
# SAĞLIK KONTROLÜ
# ---------------------------------------------------------------------------
@app.get("/api/health")
async def health():
    return {"status": "ok", "timestamp": get_now_utc().isoformat()}


# ---------------------------------------------------------------------------
# Vercel için handler
# ---------------------------------------------------------------------------
from mangum import Mangum
handler = Mangum(app)
