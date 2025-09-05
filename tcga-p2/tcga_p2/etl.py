from __future__ import annotations
import io, gzip, pandas as pd
from .storage import S3Storage
from .config import mongo_db

GENES_CANON = ["C6orf150","CCL5","CXCL10","TMEM173","CXCL9","CXCL11",
               "NFKB1","IKBKE","IRF3","TREX1","ATM","IL6","CXCL8"]
ALIASES = {"IL8": "CXCL8"}

def _read_tsv_bytes(b: bytes) -> pd.DataFrame:
    if b[:2] == b"\x1f\x8b":
        b = gzip.decompress(b)
    return pd.read_csv(io.BytesIO(b), sep="\t", dtype=str, low_memory=False)

def _patient_id(sample: str) -> str:
    parts = str(sample).split("-")
    return "-".join(parts[:3]) if len(parts) >= 3 else str(sample)

def _normalize(df: pd.DataFrame) -> pd.DataFrame:
    df.columns = [str(c).strip() for c in df.columns]

    if df.empty or df.shape[1] == 0:
        raise ValueError("empty TSV (no rows/columns)")

    first = df.columns[0]
    if "gene" not in first.lower() and "symbol" not in first.lower():
        df = df.set_index(first).T.reset_index()

    df.rename(columns={df.columns[0]: "GENE"}, inplace=True)

    df["GENE"] = df["GENE"].map(lambda g: ALIASES.get(g, g))

    sub = df[df["GENE"].isin(GENES_CANON)].set_index("GENE")
    if sub.empty:
        raise ValueError(f"no target genes found; sample headers={list(df.columns)[:5]}")

    m = sub.reset_index().melt(id_vars=["GENE"], var_name="sample", value_name="expr")
    m["patient_id"] = m["sample"].map(_patient_id)
    m["expr"] = pd.to_numeric(m["expr"], errors="coerce")

    pivot = (
        m.pivot_table(index="patient_id", columns="GENE", values="expr", aggfunc="mean")
         .reset_index()
    )
    return pivot

def ingest_key(store: S3Storage, key: str, cohort: str, db) -> int:
    b = store.get_bytes(key)
    df = _read_tsv_bytes(b)
    pivot = _normalize(df)

    coll = db["gene_expression"]
    coll.create_index([("patient_id",1),("cancer_cohort",1)], unique=True)

    n = 0
    pivot = pivot.astype(object).where(pd.notna(pivot), None)
    rows = pivot.to_dict(orient="records")
    for r in rows:
        pid = r.pop("patient_id")
        doc = {"patient_id": pid, "cancer_cohort": cohort, "genes": r, "source_key": key}
        coll.update_one({"patient_id": pid, "cancer_cohort": cohort},
                        {"$set": doc, "$setOnInsert": {"created_at": pd.Timestamp.utcnow()}},
                        upsert=True)
        n += 1
    return n
