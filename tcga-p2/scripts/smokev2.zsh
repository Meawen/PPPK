python - <<'PY'
import os, io, gzip, json
import boto3, pandas as pd, pymongo

AWS_REGION=os.getenv("AWS_REGION")
BUCKET=os.getenv("S3_BUCKET")
PREFIX=(os.getenv("S3_PREFIX") or "").strip("/")
CLINICAL_KEY=os.getenv("CLINICAL_S3_KEY")  # e.g. clinical/TCGA_clinical_survival_data.tsv
MONGO_URI=os.getenv("MONGO_URI")

assert AWS_REGION and BUCKET and MONGO_URI, "Missing AWS_REGION/S3_BUCKET/MONGO_URI"
s3=boto3.client("s3", region_name=AWS_REGION)

def s3_list(prefix):
    keys=[]
    pfx=f"{PREFIX}/{prefix}" if PREFIX else prefix
    p=s3.get_paginator("list_objects_v2")
    for page in p.paginate(Bucket=BUCKET, Prefix=pfx):
        for o in page.get("Contents", []): keys.append(o["Key"])
    return keys

def s3_get_bytes(key):
    return s3.get_object(Bucket=BUCKET, Key=key)["Body"].read()

def maybe_decompress(b):
    return gzip.decompress(b) if b[:2]==b"\x1f\x8b" else b

def pid(barcode:str)->str:
    parts=str(barcode).split("-")
    return "-".join(parts[:3]) if len(parts)>=3 else str(barcode)

GENES = ["C6orf150","CCL5","CXCL10","TMEM173","CXCL9","CXCL11","NFKB1","IKBKE","IRF3","TREX1","ATM","IL6","IL8","CXCL8"]

# --- Mongo ---
c=pymongo.MongoClient(MONGO_URI)
db = c.get_default_database()
if db is None:
    db = c["pppk"]

col=db["gene_expression"]

# --- Ingest all gene TSVs from S3 ---
keys=s3_list("gene_expression/")
tsv_keys=[k for k in keys if k.endswith(".tsv.gz")]
print("Found gene TSVs:", tsv_keys or "NONE")
upserts=0
for key in tsv_keys:
    raw = maybe_decompress(s3_get_bytes(key))
    df  = pd.read_csv(io.BytesIO(raw), sep="\t", dtype=str, low_memory=False)
    # detect gene-name column (Xena often uses 'sample' as header)
    gene_col = df.columns[0]
    if gene_col.lower() not in {"sample","gene","genes","name"}:
        # fallback: assume first column is gene names anyway
        pass
    df.set_index(gene_col, inplace=True)
    # cohort label from folder name
    parts=key.split("/")
    short = parts[1] if len(parts)>1 else "UNKNOWN"
    cohort = short if short.startswith("TCGA-") else f"TCGA-{short}"
    # iterate sample columns
    for colname in df.columns:
        patient = pid(colname)
        # resolve IL8/CXCL8 alias
        genes={}
        for g in GENES:
            if g in df.index:
                v=df.at[g, colname]
            elif g=="IL8" and "CXCL8" in df.index:
                v=df.at["CXCL8", colname]
            else:
                v=None
            try: v = None if v in (None,"","NA","NaN") else float(v)
            except: v = None
            genes[g if g!="IL8" else "CXCL8"]=v  # store CXCL8 canonical
        res=col.update_one(
            {"patient_id":patient, "cancer_cohort":cohort},
            {"$set":{"patient_id":patient,"cancer_cohort":cohort,"genes":genes}},
            upsert=True
        )
        upserts += int(bool(res.upserted_id))
print("Gene upserts:", upserts)

# --- Clinical merge (optional but recommended) ---
updated=0
if CLINICAL_KEY:
    clin_raw = maybe_decompress(s3_get_bytes(CLINICAL_KEY))
    clin = pd.read_csv(io.BytesIO(clin_raw), sep="\t", dtype=str, low_memory=False)
    # column resolution (case-insensitive)
    cols = {c.lower(): c for c in clin.columns}
    bc = cols.get("bcr_patient_barcode")
    if bc:
        def to01(x):
            if pd.isna(x): return None
            x=str(x).strip().lower()
            if x in {"1","0"}: return int(x)
            if x in {"true","yes"}: return 1
            if x in {"false","no"}: return 0
            return None
        slim = pd.DataFrame()
        slim["patient_id"]=clin[bc].map(pid)
        if "os" in cols:  slim["OS"]=clin[cols["os"]].map(to01)
        if "dss" in cols: slim["DSS"]=clin[cols["dss"]].map(to01)
        if "clinical_stage" in cols: slim["clinical_stage"]=clin[cols["clinical_stage"]].replace(["","NA","NaN"], None)
        slim=slim.groupby("patient_id", as_index=False).agg({"OS":"max","DSS":"max","clinical_stage":"last"})
        for row in slim.itertuples(index=False):
            r=col.update_many({"patient_id":row.patient_id},
                              {"$set":{"clinical":{"OS":getattr(row,"OS",None),
                                                   "DSS":getattr(row,"DSS",None),
                                                   "clinical_stage":getattr(row,"clinical_stage",None)}}})
            updated += r.matched_count
print("Clinical updates (matched docs):", updated)

# --- Report ---
print("Total docs:", col.estimated_document_count())
print("Cohorts:", col.distinct("cancer_cohort")[:10])
samp = col.find_one({}, {"_id":0,"patient_id":1,"cancer_cohort":1,"genes.TMEM173":1,"clinical":1})
print("Sample doc:", samp)
PY
