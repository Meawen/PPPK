from __future__ import annotations
import json, io, gzip, typer
import os
import json
from typing import Optional
from .viz import make_patient_plot

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
        mapping = json.load(f)

    uploaded = 0
    for short, url in mapping.items():
        cohort = f"TCGA-{short}"
        print(f"→ {cohort} :: {url}")
        data = xena.download(url)
        gz, ctype = xena.maybe_gzip(data, url)
        key = f"gene_expression/{cohort}/gene_expression.tsv.gz"
        store.put_bytes(key, gz, content_type=ctype)
        print(f"✔ uploaded s3://{s.s3_bucket}/{(s.s3_prefix or '') + key}")
        uploaded += 1

    print(f"DONE: {uploaded} cohorts")


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
    from .etl import ingest_all_from_s3
    n = ingest_all_from_s3()
    print(f"TOTAL upserts: {n}")
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
def clinical_cmd(
    source: str = typer.Option("s3", help="s3|local|http"),
    key: str = typer.Option(None, help="S3 key if source=s3"),
    path: str = typer.Option(None, help="Local path if source=local"),
    url: str = typer.Option(None,  help="HTTP URL if source=http"),
):
    from .config import Settings, mongo_db
    from .storage import S3Storage
    from .clinical import ingest_clinical_from_s3, ingest_clinical_from_local, ingest_clinical_from_http

    s = Settings(); s.validate()
    db = mongo_db(s)

    if source == "s3":
        key = key or os.getenv("CLINICAL_S3_KEY")
        if not key:
            typer.echo("ERROR: set --key or CLINICAL_S3_KEY", err=True); raise typer.Exit(code=1)
        n = ingest_clinical_from_s3(S3Storage(s), key, db)
    elif source == "local":
        if not path:
            typer.echo("ERROR: set --path for local", err=True); raise typer.Exit(code=1)
        n = ingest_clinical_from_local(path, db)
    elif source == "http":
        if not url:
            typer.echo("ERROR: set --url for http", err=True); raise typer.Exit(code=1)
        n = ingest_clinical_from_http(url, db)
    else:
        typer.echo("ERROR: source must be s3|local|http", err=True); raise typer.Exit(code=1)

    typer.echo(f"Clinical matched/updated: {n}")

@cli.command("get")
def get_cmd(patient_id: str, cohort: str):
    """Dump one patient's expression+clinical as JSON."""
    s = Settings(); s.validate()
    db = mongo_db(s)
    doc = db["gene_expression"].find_one(
        {"patient_id": patient_id, "cancer_cohort": cohort},
        {"_id": 0}
    )
    if not doc:
        raise SystemExit("not found")
    print(json.dumps(doc, ensure_ascii=False, indent=2))

@cli.command("find")
def find_cmd(cohort: str, gene: str, min: float = typer.Option(None), max: float = typer.Option(None), limit: int = 100):
    """Find patients in a cohort by gene value range."""
    s = Settings(); s.validate()
    db = mongo_db(s)
    q = {"cancer_cohort": cohort}
    cond = {}
    if min is not None: cond["$gte"] = float(min)
    if max is not None: cond["$lte"] = float(max)
    if cond:
        q[f"genes.{gene}"] = cond
    cur = db["gene_expression"].find(q, {"_id": 0, "patient_id": 1, "genes."+gene: 1, "clinical": 1}).limit(int(limit))
    for d in cur:
        print(json.dumps(d, ensure_ascii=False))

@cli.command("viz")
def viz_cmd(patient_id: str, cohort: str, out_path: Optional[str] = None, upload_s3: bool = False):
    """Render a PNG bar chart for the 13 genes."""
    s = Settings(); s.validate()
    db = mongo_db(s)
    doc = db["gene_expression"].find_one({"patient_id": patient_id, "cancer_cohort": cohort}, {"_id": 0, "genes": 1})
    if not doc:
        raise SystemExit("not found")
    png = make_patient_plot(patient_id, cohort, doc.get("genes", {}))
    if upload_s3:
        key = f"viz/{cohort}/{patient_id}.png"
        S3Storage(s).put_bytes(key, png, content_type="image/png")
        print(f"s3://{s.s3_bucket}/{s.s3_prefix + key if s.s3_prefix else key}")
    else:
        out_path = out_path or f"{patient_id}_{cohort}.png"
        with open(out_path, "wb") as f: f.write(png)
        print(out_path)

@cli.command("serve")
def serve_cmd(host: str = "0.0.0.0", port: int = 8000, reload: bool = False):
    """Run the FastAPI service (requires uvicorn)."""
    import uvicorn
    uvicorn.run("tcga_p2.api:app", host=host, port=port, reload=reload)

def main():
    cli()

if __name__ == "__main__":
    main()