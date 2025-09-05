import io, gzip, pandas as pd, re
from typing import Dict, List
from .storage import S3Storage
from .config import Settings, mongo_db

GENES = ["C6orf150","CCL5","CXCL10","TMEM173","CXCL9","CXCL11","NFKB1","IKBKE","IRF3","TREX1","ATM","IL6","CXCL8"]
ALIASES: Dict[str, List[str]] = {
    "CXCL8": ["IL8"],
    "C6orf150": ["MB21D1"],  # cGAS new symbol
}

def _pid(barcode: str) -> str:
    parts = str(barcode).split("-")
    return "-".join(parts[:3]) if len(parts) >= 3 else str(barcode)

def _to_float(v):
    try:
        if v is None: return None
        s = str(v).strip()
        if s == "" or s.upper() in {"NA","NAN"}: return None
        return float(s)
    except Exception:
        return None

def _is_tcga_barcode(s: str) -> bool:
    return isinstance(s, str) and s.startswith("TCGA-") and len(s) >= 12

def _cohort_from_key(key: str) -> str:
    # gene_expression/TCGA-BRCA/gene_expression.tsv.gz  -> TCGA-BRCA
    parts = key.split("/")
    folder = parts[1] if len(parts) > 1 else "UNKNOWN"
    return folder

def _detect_orientation(df: pd.DataFrame) -> str:
    # Heuristic: if 2nd column header looks like TCGA barcode -> rows=GENES, cols=SAMPLES
    col1 = str(df.columns[1]) if len(df.columns) > 1 else ""
    if _is_tcga_barcode(col1):
        return "rows_genes"
    # Otherwise look at first-column VALUES
    first_vals = [str(v) for v in df.iloc[:20, 0].tolist()]
    tcga_in_first_col = sum(_is_tcga_barcode(x) for x in first_vals)
    return "rows_samples" if tcga_in_first_col > 10 else "rows_genes"

def ingest_s3_key(store: S3Storage, key: str, db):
    raw = store.get_bytes(key)
    if key.endswith(".gz"): raw = gzip.decompress(raw)
    df = pd.read_csv(io.BytesIO(raw), sep="\t", dtype=str, low_memory=False)

    orient = _detect_orientation(df)
    coll = db["gene_expression"]
    cohort = _cohort_from_key(key)
    upserts = 0

    if orient == "rows_genes":
        # first column = gene symbol; columns after = TCGA barcodes
        df = df.set_index(df.columns[0])
        idx = {r.upper(): r for r in df.index}
        def row_for(g: str):
            u = g.upper()
            if u in idx: return idx[u]
            for a in ALIASES.get(g, []):
                au = a.upper()
                if au in idx: return idx[au]
            return None
        map_rows = {g: row_for(g) for g in GENES}

        for col in df.columns:
            patient = _pid(col)
            genes = {}
            for g in GENES:
                r = map_rows[g]
                v = df.at[r, col] if r else None
                genes[g] = _to_float(v)
            res = coll.update_one(
                {"patient_id": patient, "cancer_cohort": cohort},
                {"$set": {"patient_id": patient, "cancer_cohort": cohort, "genes": genes}},
                upsert=True
            )
            upserts += int(bool(res.upserted_id))

    else:  # rows = samples, columns = genes
        df = df.set_index(df.columns[0])  # index = TCGA barcode
        cols = {c.upper(): c for c in df.columns}
        def col_for(g: str):
            u = g.upper()
            if u in cols: return cols[u]
            for a in ALIASES.get(g, []):
                au = a.upper()
                if au in cols: return cols[au]
            return None
        map_cols = {g: col_for(g) for g in GENES}

        for sample, row in df.iterrows():
            patient = _pid(sample)
            genes = {}
            for g in GENES:
                c = map_cols[g]
                v = row[c] if c else None
                genes[g] = _to_float(v)
            res = coll.update_one(
                {"patient_id": patient, "cancer_cohort": cohort},
                {"$set": {"patient_id": patient, "cancer_cohort": cohort, "genes": genes}},
                upsert=True
            )
            upserts += int(bool(res.upserted_id))

    return upserts

def ingest_all_from_s3():
    s = Settings(); s.validate()
    db = mongo_db(s)
    store = S3Storage(s)
    keys = [k for k in store.list("gene_expression/") if k.endswith(".tsv.gz")]
    total = 0
    for k in keys:
        print(f"== Ingest { _cohort_from_key(k) } :: {k}")
        total += ingest_s3_key(store, k, db)
    db["gene_expression"].create_index([("patient_id", 1), ("cancer_cohort", 1)], unique=True)
    print(f"TOTAL upserts: {total}")
    return total