from __future__ import annotations
import io, os, gzip, pandas as pd, requests
from pymongo.database import Database
from .storage import S3Storage
from .config import Settings, mongo_db

CLINICAL_KEEP = ["bcr_patient_barcode", "OS", "DSS", "clinical_stage"]

def _patient_id(barcode: str) -> str:
   
    parts = str(barcode).split("-")
    return "-".join(parts[:3]) if len(parts) >= 3 else str(barcode)

def _read_tsv_bytes(b: bytes) -> pd.DataFrame:
    if b[:2] == b"\x1f\x8b":
        b = gzip.decompress(b)
    return pd.read_csv(io.BytesIO(b), sep="\t", dtype=str, low_memory=False)

def _read_http(url: str) -> pd.DataFrame:
    r = requests.get(url, timeout=180)
    r.raise_for_status()
    return _read_tsv_bytes(r.content)

def _slim(df: pd.DataFrame) -> pd.DataFrame:
    cols = {c.lower(): c for c in df.columns}
    bc = cols.get("bcr_patient_barcode")
    os_col = cols.get("os")
    dss_col = cols.get("dss")
    stage = cols.get("clinical_stage")

    slim = pd.DataFrame()
    slim["patient_id"] = df[bc].map(_patient_id)

    def _to01(x: str | float | int | None):
        if pd.isna(x): return None
        x = str(x).strip()
        if x in {"1", "0"}: return int(x)
        if x.lower() in {"na", "nan", ""}: return None
        return 1 if x.lower() in {"true", "yes"} else 0 if x.lower() in {"false", "no"} else None

    if os_col:  slim["OS"]  = df[os_col].map(_to01)
    if dss_col: slim["DSS"] = df[dss_col].map(_to01)
    if stage:   slim["clinical_stage"] = df[stage].replace(["", "NA", "NaN"], None)

    agg = {"OS": "max", "DSS": "max", "clinical_stage": "last"}
    slim = slim.groupby("patient_id", as_index=False).agg({k: v for k, v in agg.items() if k in slim.columns})

    for k in ("OS", "DSS", "clinical_stage"):
        if k not in slim.columns: slim[k] = None
    return slim[["patient_id", "DSS", "OS", "clinical_stage"]]

def _merge_into_mongo(slim: pd.DataFrame, db: Database) -> int:

    coll = db["gene_expression"]
    total = 0
    for row in slim.itertuples(index=False):
        clinical = {"DSS": getattr(row, "DSS"), "OS": getattr(row, "OS"), "clinical_stage": getattr(row, "clinical_stage")}
        res = coll.update_many({"patient_id": getattr(row, "patient_id")},
                               {"$set": {"clinical": clinical}})
        total += res.matched_count
    return total


def ingest_clinical_from_local(path: str, db: Database) -> int:
    with open(path, "rb") as f:
        df = _read_tsv_bytes(f.read())
    return _merge_into_mongo(_slim(df), db)

def ingest_clinical_from_s3(store: S3Storage, key: str, db: Database) -> int:
    body = store.get_bytes(key)
    df = _read_tsv_bytes(body)
    return _merge_into_mongo(_slim(df), db)

def ingest_clinical_from_http(url: str, db: Database) -> int:
    df = _read_http(url)
    return _merge_into_mongo(_slim(df), db)
