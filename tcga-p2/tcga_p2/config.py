from __future__ import annotations
import os
from dataclasses import dataclass
from dotenv import load_dotenv
import boto3
from pymongo import MongoClient

load_dotenv()

@dataclass(frozen=True)
class Settings:
    # S3
    aws_region: str = os.getenv("AWS_REGION", "eu-central-1")
    aws_access_key_id: str = os.getenv("AWS_ACCESS_KEY_ID", "")
    aws_secret_access_key: str = os.getenv("AWS_SECRET_ACCESS_KEY", "")
    s3_bucket: str = os.getenv("S3_BUCKET", "pppkbucket")
    s3_prefix: str = os.getenv("S3_PREFIX", "").strip()  # optional global prefix

    # Mongo
    mongo_uri: str = os.getenv("MONGO_URI", "")

    def validate(self) -> None:
        for k in ("aws_region","aws_access_key_id","aws_secret_access_key","s3_bucket"):
            if not getattr(self, k):
                raise RuntimeError(f"Missing env: {k.upper()}")

def s3_client(s: Settings):
    return boto3.client(
        "s3",
        region_name=s.aws_region,
        aws_access_key_id=s.aws_access_key_id,
        aws_secret_access_key=s.aws_secret_access_key,
    )

def mongo_db(s: Settings):
    if not s.mongo_uri:
        raise RuntimeError("Missing env: MONGO_URI")
    client = MongoClient(s.mongo_uri)
    try:
        db = client.get_default_database()
    except Exception:
        db = None
    if db is None:
        db = client["pppk"]  # or read from env if you prefer
    return db
