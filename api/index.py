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
SUPABASE_KEY: str = os.environ.get("SUPABASE_SERVICE_KEY") or os.environ.get("SUPABASE_KEY", "")


def get_supabase_client() -> Client:
    if not SUPABASE_URL or not SUPABASE_KEY:
        raise ValueError("SUPABASE_URL veya SUPABASE_KEY ortam değişkeni eksik!")
    return create_client(SUPABASE_URL, SUPABASE_KEY)


def get_now_utc() -> datetime:
    return datetime.now(timezone.utc)


def generate_trial_key(hwid: str) -> str:
    """TRIAL-<ilk 6 karakter hwid>-<rastgele 4 karakter>"""
    prefix = hwid[:6].upper().replace("-", "")
    suffix = uuid.uuid4().hex[:4].upper()
    return f"TRIAL-{prefix}-{suffix}"


# ---------------------------------------------------------------------------
# ANA DOĞRULAMA ENDPOINTİ
# ---------------------------------------------------------------------------
@app.post("/api/validate")
@app.get("/api/validate")
@app.post("/validate")
@app.get("/validate")
@app.post("/validate_license")
@app.get("/validate_license")
async def validate_license(request: Request):
    params = dict(request.query_params)
    try:
        body = await request.json()
        if isinstance(body, dict):
            params.update(body)
    except Exception:
        pass

    hwid: str = (params.get("hwid") or params.get("device_id") or "").strip()
    license_key: str = (params.get("license_key") or "").strip()

    if not hwid:
        return JSONResponse(
            status_code=422,
            content={"valid": False, "reason": "missing_hwid", "message": "HWID gerekli."}
        )

    try:
        sb = get_supabase_client()
    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={"valid": False, "reason": "config_error", "message": str(e)}
        )

    now = get_now_utc()

    # 1. Dolu bir license_key varsa önce onunla ara
    if license_key:
        try:
            res = sb.table("licenses").select("*").eq("license_key", license_key).limit(1).execute()
            rows = res.data or []
        except Exception as e:
            return JSONResponse(
                status_code=500,
                content={"valid": False, "reason": "db_error", "message": f"Supabase hatası: {str(e)}"}
            )

        if rows:
            record = rows[0]
            status = (record.get("status") or "").strip().lower()
            expire_raw = record.get("expire_date")
            raw_devices = record.get("device_ids") or []
            device_ids = [str(d).strip() for d in raw_devices]
            max_devices = record.get("max_devices") or 1

            if status != "active":
                return JSONResponse(content={
                    "valid": False,
                    "reason": "invalid_status",
                    "message": f"Lisans durumu: {status}",
                    "status": status
                })

            days_left = None
            if expire_raw:
                try:
                    exp_dt = datetime.fromisoformat(expire_raw.replace("Z", "+00:00"))
                    if exp_dt.tzinfo is None:
                        exp_dt = exp_dt.replace(tzinfo=timezone.utc)
                    if now > exp_dt:
                        return JSONResponse(content={
                            "valid": False,
                            "reason": "license_expired",
                            "message": "Lisans süreniz dolmuştur.",
                            "status": "expired"
                        })
                    days_left = (exp_dt - now).days
                except Exception:
                    pass

            # Cihaz kayıtlı mı?
            if hwid in device_ids:
                return JSONResponse(content={
                    "valid": True,
                    "reason": "device_registered",
                    "license_key": license_key,
                    "days_left": days_left,
                    "status": "active",
                    "message": "Lisans geçerli."
                })

            if len(device_ids) < max_devices:
                device_ids.append(hwid)
                sb.table("licenses").update({"device_ids": device_ids}).eq("id", record["id"]).execute()
                return JSONResponse(content={
                    "valid": True,
                    "reason": "device_bound_successfully",
                    "license_key": license_key,
                    "days_left": days_left,
                    "status": "active",
                    "message": "Cihaz lisansa bağlandı."
                })
            else:
                return JSONResponse(content={
                    "valid": False,
                    "reason": "device_limit_reached",
                    "message": "Lisans cihaz limiti doldu."
                })

    # 2. Key boşsa veya bulunamadıysa -> HWID bazlı Trial kontrolü
    try:
        res_hwid = sb.table("licenses").select("*").contains("device_ids", [hwid]).limit(1).execute()
        existing = res_hwid.data[0] if res_hwid.data else None
    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={"valid": False, "reason": "db_error", "message": f"Supabase sorgu hatası: {str(e)}"}
        )

    if existing:
        status = (existing.get("status") or "").strip().lower()
        expire_raw = existing.get("expire_date")
        key = existing.get("license_key", "")
        days_left = None

        if status != "active":
            return JSONResponse(content={
                "valid": False,
                "reason": "trial_expired",
                "message": "Deneme süreniz dolmuştur.",
                "status": status,
                "license_key": key
            })

        if expire_raw:
            try:
                exp_dt = datetime.fromisoformat(expire_raw.replace("Z", "+00:00"))
                if exp_dt.tzinfo is None:
                    exp_dt = exp_dt.replace(tzinfo=timezone.utc)
                if now > exp_dt:
                    return JSONResponse(content={
                        "valid": False,
                        "reason": "trial_expired",
                        "message": "30 günlük deneme süreniz dolmuştur.",
                        "status": "expired",
                        "license_key": key
                    })
                days_left = (exp_dt - now).days
            except Exception:
                pass

        return JSONResponse(content={
            "valid": True,
            "reason": "trial_active",
            "message": f"Deneme süresi aktif. {days_left} gün kaldı.",
            "license_key": key,
            "days_left": days_left,
            "status": "active"
        })

    # 3. HWID hiç kayıtlı değil -> 30 günlük Trial kaydı oluştur
    expire = now + timedelta(days=30)
    trial_key = generate_trial_key(hwid)
    guest_email = f"trial_{hwid[:8].lower()}@bgflix.tv"

    record = {
        "email": guest_email,
        "license_key": trial_key,
        "status": "active",
        "expire_date": expire.isoformat(),
        "device_ids": [hwid],
        "is_trial": True,
        "created_at": now.isoformat(),
    }

    try:
        sb.table("licenses").insert(record).execute()
    except Exception as e:
        # is_trial veya created_at sütunu tabloda yoksa sade kayıt dene
        try:
            minimal = {
                "email": guest_email,
                "license_key": trial_key,
                "status": "active",
                "expire_date": expire.isoformat(),
                "device_ids": [hwid]
            }
            sb.table("licenses").insert(minimal).execute()
        except Exception as e2:
            return JSONResponse(
                status_code=500,
                content={"valid": False, "reason": "db_insert_error", "message": f"Kayıt oluşturulamadı: {str(e2)}"}
            )

    return JSONResponse(content={
        "valid": True,
        "reason": "trial_created",
        "message": "30 günlük deneme süresi başlatıldı.",
        "license_key": trial_key,
        "days_left": 30,
        "expire_date": expire.isoformat(),
        "status": "active"
    })



# ---------------------------------------------------------------------------
# SİTEDEN SİPARİŞ / LİSANS ÜRETME ENDPOİNTİ (plashtane.html)
# ---------------------------------------------------------------------------
@app.post("/api/create_order")
@app.post("/create_order")
async def create_order(request: Request):
    try:
        body = await request.json()
    except Exception:
        body = {}

    plan = body.get("plan", "3-months")
    contact = (body.get("contact") or "").strip()
    txid = (body.get("txid") or "").strip()
    hwid = (body.get("hwid") or "").strip()

    if not contact:
        rnd = uuid.uuid4().hex[:6]
        contact = f"guest_{rnd}@bgflix.tv"

    now = get_now_utc()
    if plan == "1-month":
        days = 30
        max_dev = 1
    elif plan == "12-months":
        days = 365
        max_dev = 3
    else:  # 3-months default
        days = 90
        max_dev = 2

    expire = now + timedelta(days=days)
    k1 = uuid.uuid4().hex[:4].upper()
    k2 = uuid.uuid4().hex[:4].upper()
    new_license_key = f"BGFLIX-{k1}-{k2}"

    record = {
        "email": contact,
        "license_key": new_license_key,
        "status": "active",
        "expire_date": expire.isoformat(),
        "device_ids": [hwid] if hwid else [],
        "is_trial": False,
        "created_at": now.isoformat(),
    }

    try:
        sb = get_supabase_client()
        sb.table("licenses").insert(record).execute()
    except Exception as e:
        try:
            minimal = {
                "email": contact,
                "license_key": new_license_key,
                "status": "active",
                "expire_date": expire.isoformat(),
                "device_ids": [hwid] if hwid else []
            }
            sb.table("licenses").insert(minimal).execute()
        except Exception as e2:
            return JSONResponse(status_code=500, content={"success": False, "error": str(e2)})

    return JSONResponse(content={
        "success": True,
        "license_key": new_license_key,
        "expire_date": expire.isoformat(),
        "days": days,
        "plan": plan
    })


@app.get("/api/health")
@app.get("/health")
async def health():
    return {"status": "ok", "timestamp": get_now_utc().isoformat()}


from mangum import Mangum
handler = Mangum(app)
