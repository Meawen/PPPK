from __future__ import annotations
import io
import matplotlib
matplotlib.use("Agg")  # headless
import matplotlib.pyplot as plt

GENE_ORDER = ["C6orf150","CCL5","CXCL10","TMEM173","CXCL9","CXCL11",
              "NFKB1","IKBKE","IRF3","TREX1","ATM","IL6","CXCL8"]  # IL8 == CXCL8

def make_patient_plot(patient_id: str, cohort: str, genes: dict[str, float | int | None]) -> bytes:
    xs, ys = [], []
    for g in GENE_ORDER:
        xs.append(g)
        v = genes.get(g)
        try:
            ys.append(float(v) if v is not None else 0.0)
        except Exception:
            ys.append(0.0)

    fig = plt.figure(figsize=(10, 4.5))
    plt.bar(xs, ys)  # no explicit colors/styles per constraints
    plt.title(f"{patient_id} · {cohort}")
    plt.ylabel("Expression")
    plt.xticks(rotation=45, ha="right")
    plt.tight_layout()

    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=144)
    plt.close(fig)
    buf.seek(0)
    return buf.read()
