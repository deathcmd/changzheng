#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_DIR="$(cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$PROJECT_DIR/.env"

cd "$PROJECT_DIR"

if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  echo "Docker with the Compose plugin is required." >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE. Copy .env.example to .env and replace every placeholder." >&2
  exit 1
fi

if grep -Eq '(^|=)replace-with-|your-domain\.example' "$ENV_FILE"; then
  echo ".env still contains placeholder values." >&2
  exit 1
fi

docker compose --env-file "$ENV_FILE" config --quiet

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven 3.8+ is required to build the backend." >&2
  exit 1
fi
if ! command -v npm >/dev/null 2>&1; then
  echo "Node.js and npm are required to build the admin frontend." >&2
  exit 1
fi

mvn --batch-mode clean package
npm --prefix changzheng-admin-web ci
npm --prefix changzheng-admin-web run build

docker compose --env-file "$ENV_FILE" build --pull
docker compose --env-file "$ENV_FILE" up -d
docker compose --env-file "$ENV_FILE" ps
