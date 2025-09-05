from __future__ import annotations
from fastapi import FastAPI, HTTPException, Response, Query
from typing import Optional
from .config import Settings, mongo_db
from .viz import make_patient_plot

app = FastAPI(title="TCGA Gene API")

_s = Settings()
_db = mongo_db(_s)
_coll = _db["gene_expression"]

@app.get("/healthz")
def healthz():
    _coll.estimated_document_count()  # ping
    return {"ok": True}

@app.get("/patients/{patient_id}")
def get_patient(patient_id: str, cohort: str = Query(..., description="TCGA cohort, e.g. TCGA-BRCA")):
    doc = _coll.find_one({"patient_id": patient_id, "cancer_cohort": cohort}, {"_id": 0})
    if not doc:
        raise HTTPException(404, "Not found")
    return doc

@app.get("/cohorts/{cohort}/patients")
def query_patients(
    cohort: str,
    gene: Optional[str] = None,
    min: Optional[float] = None,
    max: Optional[float] = None,
    limit: int = 100
):
    q: dict = {"cancer_cohort": cohort}
    if gene:
        path = f"genes.{gene}"
        cond = {}
        if min is not None: cond["$gte"] = float(min)
        if max is not None: cond["$lte"] = float(max)
        if cond: q[path] = cond
    cur = _coll.find(q, {"_id": 0, "patient_id": 1, "genes": 1, "clinical": 1}).limit(int(limit))
    return list(cur)

@app.get("/viz/{patient_id}", response_class=Response)
def viz_patient(patient_id: str, cohort: str):
    doc = _coll.find_one({"patient_id": patient_id, "cancer_cohort": cohort}, {"_id": 0, "genes": 1})
    if not doc:
        raise HTTPException(404, "Not found")
    png = make_patient_plot(patient_id, cohort, doc.get("genes", {}))
    return Response(content=png, media_type="image/png")


