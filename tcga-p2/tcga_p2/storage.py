from typing import List, Optional
import boto3

class S3Storage:
    def __init__(self, settings):
        self.s3 = boto3.client(
            "s3",
            region_name=settings.aws_region,
            aws_access_key_id=settings.aws_access_key_id,
            aws_secret_access_key=settings.aws_secret_access_key,
        )
        self.bucket = settings.s3_bucket
        self.prefix = (settings.s3_prefix or "").strip("/")

    def _key(self, key: str) -> str:
        key = key.lstrip("/")
        return f"{self.prefix}/{key}" if self.prefix else key

    def put_bytes(self, key: str, data: bytes, content_type: Optional[str] = None):
        self.s3.put_object(
            Bucket=self.bucket,
            Key=self._key(key),
            Body=data,
            ContentType=content_type or "application/octet-stream",
        )

    def get_bytes(self, key: str) -> bytes:
        return self.s3.get_object(Bucket=self.bucket, Key=self._key(key))["Body"].read()

    def list(self, prefix: str) -> List[str]:
        p = prefix if prefix.endswith("/") else prefix + "/"
        p = self._key(p)
        keys: List[str] = []
        paginator = self.s3.get_paginator("list_objects_v2")
        for page in paginator.paginate(Bucket=self.bucket, Prefix=p):
            for obj in page.get("Contents", []):
                k = obj["Key"]
                if self.prefix and k.startswith(self.prefix + "/"):
                    k = k[len(self.prefix) + 1:]
                keys.append(k)
        return keys
