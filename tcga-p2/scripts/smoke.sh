#!/usr/bin/env bash
set -euo pipefail

# cd to tcga-p2 no matter where it's launched from
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

# Load env
if [ -f .env ]; then set -a; source .env; set +a; fi

echo "== tcga-p2 SMOKE: Step 2 (upload-local) + Step 3 (ingest) =="

# Sanity on env
: "${AWS_REGION:?Missing AWS_REGION}"
: "${AWS_ACCESS_KEY_ID:?Missing AWS_ACCESS_KEY_ID}"
: "${AWS_SECRET_ACCESS_KEY:?Missing AWS_SECRET_ACCESS_KEY}"
: "${S3_BUCKET:?Missing S3_BUCKET}"
: "${MONGO_URI:?Missing MONGO_URI}"

# Create a tiny TSV (genes x samples). Keep only a subset of genes; pipeline can handle missing ones.
TMPDIR="$(mktemp -d)"
TSV="$TMPDIR/gene_expression.tsv"
cat > "$TSV" <<'TSV'
gene	TCGA-AB-1234-01A	TCGA-AB-1235-01A
C6orf150	5.1	6.2
TMEM173	7.8	8.1
CXCL8	3.4	2.9
IRF3	6.0	6.1
ATM	4.2	4.0
TSV

echo "➜ Uploading sample TSV to s3://$S3_BUCKET/gene_expression/BRCA/gene_expression.tsv.gz"
python -m tcga_p2.cli upload-local BRCA "$TSV"

echo "➜ Ingesting from S3 -> Mongo"
python -m tcga_p2.cli ingest

echo "➜ Running assertions"
python3 scripts/asserts.py

echo "✔ SMOKE PASS"
