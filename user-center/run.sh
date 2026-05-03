#!/usr/bin/env bash
set -e

# Load environment variables from .env in the current working directory
# (in the Docker image built from the bundled Dockerfile this resolves to /app/.env).
ENV_FILE="$(pwd)/.env"
if [ -f "${ENV_FILE}" ]; then
  set -a
  # shellcheck disable=SC1090
  . "${ENV_FILE}"
  set +a
fi

exec java ${JAVA_OPTS} -jar app.jar
