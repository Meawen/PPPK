import os, sys, boto3
from pymongo import MongoClient

AWS_REGION=os.getenv("AWS_REGION"); AWS_ACCESS_KEY_ID=os.getenv("AWS_ACCESS_KEY_ID")
AWS_SECRET_ACCESS_KEY=os.getenv("AWS_SECRET_ACCESS_KEY"); S3_BUCKET=os.getenv("S3_BUCKET")
MONGO_URI=os.getenv("MONGO_URI"); S3_PREFIX=(os.getenv("S3_PREFIX") or "").strip()

for v in ("AWS_REGION","AWS_ACCESS_KEY_ID","AWS_SECRET_ACCESS_KEY","S3_BUCKET","MONGO_URI"):
    if not globals()[v]: print(f"❌ Missing {v}"); sys.exit(1)

def s3_key(local: str) -> str: return f"{S3_PREFIX}{local}" if S3_PREFIX else local

s3=boto3.client("s3",region_name=AWS_REGION,
                aws_access_key_id=AWS_ACCESS_KEY_ID,
                aws_secret_access_key=AWS_SECRET_ACCESS_KEY)

key = s3_key("gene_expression/BRCA/gene_expression.tsv.gz")
try:
    s3.head_object(Bucket=S3_BUCKET, Key=key)
    print(f"✅ S3 object exists: s3://{S3_BUCKET}/{key}")
except Exception as e:
    print(f"❌ head_object miss: s3://{S3_BUCKET}/{key} -> {e}")
    # list what *does* exist so we see the real key
    resp=s3.list_objects_v2(Bucket=S3_BUCKET, Prefix=s3_key("raw/BRCA/"))
    keys=[it["Key"] for it in resp.get("Contents",[])]
    print("🔎 Keys under prefix:", keys or "(none)")
    sys.exit(2)

client = MongoClient(MONGO_URI)
try:
    db = client.get_default_database()
except Exception:
    db = None
if db is None:
    db = client["pppk"]
col = db["gene_expression"]

doc=col.find_one({"patient_id":"TCGA-AB-1234","cancer_cohort":"BRCA"})
if not doc: print("❌ Mongo missing TCGA-AB-1234 / BRCA"); sys.exit(3)
genes=doc.get("genes") or {}
for g in ["C6orf150","TMEM173","CXCL8","IRF3","ATM"]:
    if g not in genes: print(f"❌ Missing gene key: {g}"); sys.exit(4)
cnt=col.count_documents({"cancer_cohort":"BRCA"})
if cnt<2: print(f"❌ Expected ≥2 BRCA docs, found {cnt}"); sys.exit(5)
print(f"✅ Mongo ok: {cnt} BRCA docs; sample genes present")
