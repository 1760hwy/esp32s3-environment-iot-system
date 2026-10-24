import logging
import os
import time
from datetime import datetime
from zoneinfo import ZoneInfo

import psycopg
import requests

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
LOG = logging.getLogger("nle-sync")

API = "https://api.nlecloud.com"
TZ = ZoneInfo("Asia/Shanghai")
TAGS = ("Temperature", "Humidity", "Light")
DEVICE_ID = os.environ["DEVICE_ID"]
DEVICE_NAME = os.environ.get("DEVICE_NAME", "esp32s3_001")
POLL_SECONDS = max(5, int(os.environ.get("POLL_SECONDS", "10")))


def login(session):
    response = session.post(
        API + "/Users/Login",
        json={
            "Account": os.environ["NLE_ACCOUNT"],
            "Password": os.environ["NLE_PASSWORD"],
            "IsRememberMe": False,
        },
        timeout=20,
    )
    response.raise_for_status()
    data = response.json()
    if data.get("Status") != 0:
        raise RuntimeError(f"NLE login failed: code={data.get('StatusCode')} msg={data.get('Msg')}")
    token = (data.get("ResultObj") or {}).get("AccessToken")
    if not token:
        raise RuntimeError("NLE login response did not include AccessToken")
    LOG.info("NLE login successful")
    return token


def fetch(session, token):
    response = session.get(
        f"{API}/devices/{DEVICE_ID}/datas",
        headers={"AccessToken": token},
        timeout=20,
    )
    response.raise_for_status()
    data = response.json()
    if data.get("Status") != 0:
        raise RuntimeError(f"NLE data query failed: code={data.get('StatusCode')} msg={data.get('Msg')}")
    return (data.get("ResultObj") or {}).get("DataPoints") or []


def parse_rows(points):
    values = {}
    for sensor in points:
        tag = sensor.get("ApiTag")
        if tag not in TAGS:
            continue
        values[tag] = {
            p["RecordTime"]: p["Value"]
            for p in sensor.get("PointDTO") or []
            if p.get("RecordTime") and p.get("Value") is not None
        }
    if not all(k in values for k in TAGS):
        return []
    timestamps = set.intersection(*(set(values[k]) for k in TAGS))
    rows = []
    for timestamp in sorted(timestamps):
        dt = datetime.fromisoformat(timestamp)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=TZ)
        rows.append((
            DEVICE_NAME,
            float(values["Temperature"][timestamp]),
            float(values["Humidity"][timestamp]),
            float(values["Light"][timestamp]),
            dt,
        ))
    return rows


def write_rows(rows):
    if not rows:
        return 0
    with psycopg.connect(
        host=os.environ["DB_HOST"],
        port=int(os.environ.get("DB_PORT", "15432")),
        dbname=os.environ.get("DB_NAME", "iot_db"),
        user=os.environ.get("DB_USER", "iot_user"),
        password=os.environ["DB_PASSWORD"],
        connect_timeout=10,
    ) as conn:
        with conn.cursor() as cur:
            inserted = 0
            for row in rows:
                cur.execute(
                    """
                    INSERT INTO public.sensor_data
                        (device_id, temperature, humidity, light_lux, recorded_at)
                    VALUES (%s, %s, %s, %s, %s)
                    ON CONFLICT (device_id, recorded_at) DO NOTHING
                    """,
                    row,
                )
                inserted += cur.rowcount
            return inserted


def main():
    session = requests.Session()
    token = None
    token_issued_at = 0.0
    while True:
        try:
            if token is None or time.monotonic() - token_issued_at > 1800:
                token = login(session)
                token_issued_at = time.monotonic()
            points = fetch(session, token)
            rows = parse_rows(points)
            inserted = write_rows(rows)
            LOG.info("complete_rows=%d new_rows=%d", len(rows), inserted)
        except requests.RequestException as exc:
            LOG.error("HTTP request error: %s", exc)
        except psycopg.Error as exc:
            LOG.error("PostgreSQL error: %s", exc)
        except Exception as exc:
            LOG.error("Sync error: %s", exc)
            # Query may have failed because token expired. Re-login next cycle.
            token = None
        time.sleep(POLL_SECONDS)


if __name__ == "__main__":
    main()
