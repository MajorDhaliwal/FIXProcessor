#!/usr/bin/env bash

echo "Starting full FIX stack..."

# -------- SETTINGS --------
CLEAN_VOLUMES=true    # set to false if you don't want postgres/redis wiped
# --------------------------

echo "Running Maven clean + package..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
  echo "Maven build failed. Exiting."
  exit 1
fi

echo "Stopping Docker containers..."
if [ "$CLEAN_VOLUMES" = true ]; then
  docker compose down -v
else
  docker compose down
fi

echo "Starting Docker containers..."
docker compose up --build -d

echo "Waiting for containers to become healthy..."
sleep 8

echo
echo "Current running containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo

echo "Tailing FIX API logs ('docker compose down' to stop)"
#docker logs -f api-fix-api-1

