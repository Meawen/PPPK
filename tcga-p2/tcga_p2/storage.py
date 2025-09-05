from __future__ import annotations
from .config import Settings, s3_client

class S3Storage:
    def __init__(self, settings: Settings):
        self.s = settings
        self.s3 = s3_client(settings)

    def _key(self, key: str) -> str:
        return (self.s.s3_prefix + key) if self.s.s3_prefix else key

    def put_bytes(self, key: str, data: bytes, content_type: str = "application/octet-stream") -> None:
        self.s3.put_object(
            Bucket=self.s.s3_bucket,
            Key=self._key(key),
            Body=data,
            ContentType=content_type
        )

    def get_bytes(self, key: str) -> bytes:
        obj = self.s3.get_object(Bucket=self.s.s3_bucket, Key=self._key(key))
        return obj["Body"].read()

    def list_keys(self, prefix: str) -> list[str]:
        pref = self._key(prefix)
        keys, token = [], None
        while True:
            kw = dict(Bucket=self.s.s3_bucket, Prefix=pref)
            if token: kw["ContinuationToken"] = token
            resp = self.s3.list_objects_v2(**kw)
            for it in resp.get("Contents", []):
                k = it["Key"]
                if self.s.s3_prefix and k.startswith(self.s.s3_prefix):
                    k = k[len(self.s.s3_prefix):]
                keys.append(k)
            token = resp.get("NextContinuationToken")
            if not token: break
        return keys
