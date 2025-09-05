from __future__ import annotations
import json, io, gzip, typer
import os

from .config import Settings, mongo_db
from .storage import S3Storage
from . import xena
from . import etl

cli = typer.Typer(add_completion=False)

@cli.command("fetch")
def fetch_cmd(cohorts_path: str = "cohorts.json"):
    s = Settings(); s.validate()
    store = S3Storage(s)
    with open(cohorts_path, "r", encoding="utf-8") as f:
        cohorts = json.load(f)
    for cohort, url in cohorts.items():
        typer.echo(f"\n== {cohort} ==")
        try:
            data = xena.download(url)
            gz, ctype = xena.maybe_gzip(data, url)
            key = f"gene_expression/{cohort}/gene_expression.tsv.gz"
            store.put_bytes(key, gz, content_type=ctype)
            typer.secho(f"✔ uploaded s3://{s.s3_bucket}/{s.s3_prefix}{key}", fg=typer.colors.GREEN)
        except Exception as e:
            typer.secho(f"✘ {cohort}: {e}", fg=typer.colors.RED)

@cli.command("upload-local")
def upload_local(cohort: str, path: str):
    import io, gzip
    from .config import Settings, s3_client
    s = Settings(); s.validate()
    s3 = s3_client(s)

    with open(path, "rb") as f:
        data = f.read()
    if not path.endswith(".gz"):
        out = io.BytesIO()
        with gzip.GzipFile(fileobj=out, mode="wb") as g:
            g.write(data)
        data = out.getvalue()

    key = f"gene_expression/{cohort}/gene_expression.tsv.gz"
    s3.put_object(Bucket=s.s3_bucket, Key=key, Body=data, ContentType="application/gzip")
    # hard-verify
    s3.head_object(Bucket=s.s3_bucket, Key=key)
    print(f"✔ uploaded s3://{s.s3_bucket}/{key}")


@cli.command("ingest")
def ingest_cmd():

    s = Settings(); s.validate()
    db = mongo_db(s)
    store = S3Storage(s)

    keys = [k for k in store.list_keys("gene_expression/") if
            k.endswith("gene_expression.tsv.gz") or k.endswith("gene_expression.tsv")]
    if not keys:
        raise SystemExit("No TSVs under raw/* in S3. Run `./run.sh fetch` or `./run.sh upload-local` first.")

    total = 0
    for key in keys:
        # key shape: raw/<COHORT>/gene_expression.tsv.gz
        parts = key.split("/")
        cohort = parts[1] if len(parts) >= 3 else "UNKNOWN"
        typer.echo(f"\n== Ingest {cohort} :: {key}")
        try:
            n = etl.ingest_key(store, key, cohort, db)
            total += n
            typer.secho(f"✔ upserted {n}", fg=typer.colors.GREEN)
        except Exception as e:
            typer.secho(f"✘ {cohort}: {e}", fg=typer.colors.RED)

    typer.echo(f"\nTOTAL upserts: {total}")

@cli.command("upload-clinical-local")
def upload_clinical_local(path: str):
    import io, gzip
    s = Settings(); s.validate()
    store = S3Storage(s)
    key = os.getenv("CLINICAL_S3_KEY", "clinical/TCGA_clinical_survival_data.tsv")
    with open(path, "rb") as f:
        data = f.read()
    store.put_bytes(key, data, content_type="text/tab-separated-values")
    print(f"✔ uploaded s3://{s.s3_bucket}/{s.s3_prefix}{key}")

@cli.command("clinical")
def clinical_cmd(local_path: str = typer.Option(None, help="Optional local TSV override")):

    s = Settings(); s.validate()
    db = mongo_db(s)
    from .clinical import ingest_clinical_from_local, ingest_clinical_from_s3, ingest_clinical_from_http
    if local_path:
        n = ingest_clinical_from_local(local_path, db)
    else:
        key = os.getenv("CLINICAL_S3_KEY", "").strip()
        url = os.getenv("CLINICAL_TSV_URL", "").strip()
        if key:
            n = ingest_clinical_from_s3(S3Storage(s), key, db)
        elif url:
            n = ingest_clinical_from_http(url, db)
        else:
            raise SystemExit("Provide --local-path or set CLINICAL_S3_KEY or CLINICAL_TSV_URL in .env")
    print(f"✔ clinical upserts: {n}")
