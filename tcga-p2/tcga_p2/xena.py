from __future__ import annotations
import io, gzip, requests
from tqdm import tqdm

def download(url: str) -> bytes:
    with requests.get(url, stream=True, timeout=180) as r:
        r.raise_for_status()
        total = int(r.headers.get("Content-Length", 0) or 0)
        buf = io.BytesIO()
        bar = tqdm(total=total, unit="B", unit_scale=True, desc=f"GET {url.split('/')[-1]}")
        for chunk in r.iter_content(1024*1024):
            if chunk: buf.write(chunk); bar.update(len(chunk))
        bar.close(); buf.seek(0); return buf.read()

def maybe_gzip(data: bytes, url_hint: str) -> tuple[bytes,str]:
    # Keep gz if already .gz; otherwise gzip it.
    if url_hint.endswith(".gz"):
        return data, "application/gzip"
    out = io.BytesIO()
    with gzip.GzipFile(fileobj=out, mode="wb") as g: g.write(data)
    return out.getvalue(), "application/gzip"
