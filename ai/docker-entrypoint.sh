#!/bin/sh
set -eu

mkdir -p "${CHROMA_PERSIST_DIR:-/data/chroma}"
chown -R uniwiki:uniwiki /data

exec gosu uniwiki sh -c 'python -m uvicorn app.main:app --host 0.0.0.0 --port "${PORT:-8000}"'
