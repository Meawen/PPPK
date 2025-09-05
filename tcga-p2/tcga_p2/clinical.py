from __future__ import annotations
import os, io, gzip, pandas as pd, requests
from .storage import S3Storage
from .config import mongo_db, Settings

# same patient id cutter as ETL
def _patient_id(sample: str) -> str:
    parts = str(sample).split("-")
    return "-".join(parts[:3]) if len(parts) >= 3 else str(sample)

def _read_tsv_bytes(b: bytes) -> pd.DataFrame:
    # clinical TSV isn't gz, but handle gz just in case
    if b[:2] == b"\x1f\x8b": b = gzip.decompress(b)
    return pd.read_csv(io.BytesIO(b), sep="\t", dtype=str, low_memory=False)

def _read_http(url: str) -> pd.DataFrame:
    r = requests.get(url, timeout=180); r.raise_for_status()
    return _read_tsv_bytes(r.content)

def _normalize_clinical(df: pd.DataFrame) -> pd.DataFrame:
    df.columns = [str(c).strip() for c in df.columns]
    # expected cols
    need = {"bcr_patient_barcode","DSS","OS","clinical_stage"}
    missing = [c for c in need if c not in df.columns]
    if missing: raise ValueError(f"clinical TSV missing columns: {missing}")

    slim = df[list(need)].copy()
    slim["patient_id"] = slim["bcr_patient_barcode"].map(_patient_id)

    def _num01(v):
        if v in (None, "", "NA", "NaN"): return None
        try:
            x = int(float(v))
            return 1 if x == 1 else 0 if x == 0 else None
        except: return None

    slim["DSS"] = slim["DSS"].map(_num01)
    slim["OS"] = slim["OS"].map(_num01)
    slim["clinical_stage"] = slim["clinical_stage"].replace(["NA","NaN",""], None)
    # deduplicate by patient_id (keep last non-null values)
    slim = slim.groupby("patient_id", as_index=False).agg({
        "DSS":"max","OS":"max","clinical_stage":"last"
    })
    return slim[["patient_id","DSS","OS","clinical_stage"]]

def ingest_clinical_from_s3(store: S3Storage, key: str, db) -> int:
    df = _read_tsv_bytes(store.get_bytes(key))
    return _upsert(df, db)

def ingest_clinical_from_http(url: str, db) -> int:
    df = _read_http(url)
    return _upsert(df, db)

def ingest_clinical_from_local(path: str, db) -> int:
    with open(path, "rb") as f: b = f.read()
    df = _read_tsv_bytes(b)
    return _upsert(df, db)

def _upsert(df: pd.DataFrame, db) -> int:
    slim = _normalize_clinical(df)
    col = db["gene_expression"]
    col.create_index("patient_id")
    n = 0
    for r in slim.to_dict(orient="records"):
        pid = r["patient_id"]
        clinical = {k: r[k] for k in ("DSS","OS","clinical_stage")}
        # update ALL docs for that patient_id (in case multiple cohorts)
        res = col.update_many({"patient_id": pid},
            {"$set": {"clinical": clinical}})
        # optional: create stub if not exists
        if res.matched_count == 0:
            col.update_one({"patient_id": pid, "cancer_cohort": "UNKNOWN"},
                {"$setOnInsert":{"genes":{},"source_key":"clinical_only"},
                 "$set":{"clinical": clinical}},
                upsert=True)
        n += 1
    return n
