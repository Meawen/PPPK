from __future__ import annotations
import os, io, gzip, pandas as pd, requests
from .storage import S3Storage
from .config import mongo_db, Settings

def _patient_id(sample: str) -> str:
    parts = str(sample).split("-")
    return "-".join(parts[:3]) if len(parts) >= 3 else str(sample)

def _read_tsv_bytes(b: bytes) -> pd.DataFrame:
    if b[:2] == b"\x1f\x8b": b = gzip.decompress(b)
    return pd.read_csv(io.BytesIO(b), sep="\t", dtype=str, low_memory=False)

def _read_http(url: str) -> pd.DataFrame:
    r = requests.get(url, timeout=180); r.raise_for_status()
    return _read_tsv_bytes(r.content)

def _normalize_clinical(df: pd.DataFrame) -> pd.DataFrame:
    df.columns = [str(c).strip() for c in df.columns]

    def _pick(names):
        lc = {c.lower(): c for c in df.columns}
        for n in names:
            if n.lower() in lc:
                return lc[n.lower()]
        return None

    barcode = _pick(["bcr_patient_barcode","submitter_id","patient_id","patient","case_submitter_id"])
    os_col  = _pick(["OS","OS_STATUS","OS.event","OS_Event","OS.event.status"])
    dss_col = _pick(["DSS","DSS_STATUS","DSS.event","DSS_Event","DSS.event.status"])
    stage   = _pick(["clinical_stage","ajcc_pathologic_stage","pathologic_stage","ajcc_stage"])

    if not barcode:
        raise ValueError(f"clinical TSV missing patient id column; have: {list(df.columns)[:12]}")

    slim = pd.DataFrame()
    slim["patient_id"] = df[barcode].map(_patient_id)

    def _to01(v):
        if v is None or str(v).strip() in ("", "NA", "NaN", "None"):
            return None
        s = str(v).strip().lower()
        if ":" in s:
            s = s.split(":", 1)[0]
        if s in ("1","1.0","yes","true","deceased","dead","recurred/progressed","recurrence"): return 1
        if s in ("0","0.0","no","false","living","alive","diseasefree","disease free"): return 0
        try:
            x = int(float(s))
            return 1 if x == 1 else 0 if x == 0 else None
        except:
            return None

    slim["OS"]  = df[os_col].map(_to01)  if os_col  else None
    slim["DSS"] = df[dss_col].map(_to01) if dss_col else None
    slim["clinical_stage"] = df[stage].replace(["", "NA", "NaN"], None) if stage else None

    agg = {
        "OS": "max",
        "DSS": "max",
        "clinical_stage": "last",
    }
    slim = slim.groupby("patient_id", as_index=False).agg({k: v for k, v in agg.items() if k in slim.columns})
    return slim[["patient_id","DSS","OS","clinical_stage"]]

