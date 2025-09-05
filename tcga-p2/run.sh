#!/usr/bin/env bash
set -euo pipefail
export PYTHONUNBUFFERED=1
if [ -f .env ]; then set -a; source .env; set +a; fi
python -m tcga_p2.cli "$@"
