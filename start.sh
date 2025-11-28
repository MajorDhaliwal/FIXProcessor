#!/usr/bin/env bash

echo "Starting full FIX stack..."

CLEAN_VOLUMES=true

echo "Running Maven clean + package inside Docker..."
docker run --rm \
  -v "$PWD":/workspace \
  -v "$HOME/.m2":/root/.m2 \
  -w /workspace \
  maven:3.9-eclipse-temurin-21 \
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
echo "Swagger UI: http://localhost:8080/swagger-ui/index.html#/"
