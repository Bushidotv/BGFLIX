-- ============================================================
-- BGFLIX Lisans Sistemi — Supabase Tablo Şeması
-- Supabase Dashboard → SQL Editor'a kopyalayıp çalıştır
-- ============================================================

-- Mevcut tabloyu güncelle ya da sıfırdan oluştur
CREATE TABLE IF NOT EXISTS licenses (
    id              BIGSERIAL PRIMARY KEY,
    license_key     TEXT        NOT NULL UNIQUE,
    status          TEXT        NOT NULL DEFAULT 'active',   -- active | expired | suspended
    expire_date     TIMESTAMPTZ,                              -- NULL = sonsuz
    device_ids      TEXT[]      NOT NULL DEFAULT '{}',       -- HWID listesi
    is_trial        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Mevcut tabloya yeni sütunlar ekle (zaten varsa hata vermez)
ALTER TABLE licenses ADD COLUMN IF NOT EXISTS is_trial    BOOLEAN     NOT NULL DEFAULT FALSE;
ALTER TABLE licenses ADD COLUMN IF NOT EXISTS device_ids  TEXT[]      NOT NULL DEFAULT '{}';
ALTER TABLE licenses ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- İndeksler (hız için)
CREATE INDEX IF NOT EXISTS idx_licenses_key        ON licenses (license_key);
CREATE INDEX IF NOT EXISTS idx_licenses_status     ON licenses (status);
CREATE INDEX IF NOT EXISTS idx_licenses_device_ids ON licenses USING GIN (device_ids);
CREATE INDEX IF NOT EXISTS idx_licenses_expire     ON licenses (expire_date);
CREATE INDEX IF NOT EXISTS idx_licenses_is_trial   ON licenses (is_trial);

-- updated_at otomatik güncelleme trigger'ı
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_licenses_updated_at ON licenses;
CREATE TRIGGER trg_licenses_updated_at
    BEFORE UPDATE ON licenses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ============================================================
-- Row Level Security (RLS) — Supabase güvenliği
-- ============================================================
ALTER TABLE licenses ENABLE ROW LEVEL SECURITY;

-- Service role (backend API) her şeyi yapabilir
DROP POLICY IF EXISTS "Service role full access" ON licenses;
CREATE POLICY "Service role full access"
    ON licenses FOR ALL
    USING (auth.role() = 'service_role');

-- Anon (CloudStream) sadece okuyabilir — API üzerinden yapar zaten
DROP POLICY IF EXISTS "Anon read" ON licenses;
CREATE POLICY "Anon read"
    ON licenses FOR SELECT
    USING (TRUE);

-- ============================================================
-- Test verisi (isteğe bağlı — sil istersen)
-- ============================================================
-- INSERT INTO licenses (license_key, status, expire_date, device_ids, is_trial)
-- VALUES ('TEST-123456', 'active', NOW() + INTERVAL '365 days', '{}', FALSE);

-- ============================================================
-- Faydalı sorgular
-- ============================================================

-- Tüm aktif trial'ları gör:
-- SELECT * FROM licenses WHERE is_trial = TRUE AND status = 'active';

-- Süresi dolmuş trial'ları toplu güncelle:
-- UPDATE licenses SET status = 'expired'
-- WHERE is_trial = TRUE AND expire_date < NOW() AND status = 'active';

-- Belirli bir HWID'yi bul:
-- SELECT * FROM licenses WHERE device_ids @> ARRAY['HWID_BURAYA'];
