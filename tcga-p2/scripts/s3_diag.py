import os, boto3
AWS_REGION=os.getenv("AWS_REGION"); AID=os.getenv("AWS_ACCESS_KEY_ID")
ASEC=os.getenv("AWS_SECRET_ACCESS_KEY"); BUCKET=os.getenv("S3_BUCKET")
PFX = (os.getenv("S3_PREFIX") or "") + "gene_expression/BRCA/"
s3=boto3.client("s3",region_name=AWS_REGION,aws_access_key_id=AID,aws_secret_access_key=ASEC)
resp=s3.list_objects_v2(Bucket=BUCKET, Prefix=PFX)
print("Prefix:", PFX)
print("Keys:", [it["Key"] for it in resp.get("Contents",[])] or "(none)")
