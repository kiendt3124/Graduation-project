#!/bin/bash
# =============================================================
# deploy.sh - Script deploy Graduation Project lên Ubuntu Server
# Chạy: bash deploy.sh
# =============================================================
set -e

APP_DIR="/home/$(whoami)/graduation-project"
COMPOSE_FILE="$APP_DIR/docker-compose-prod.yml"
IMAGE="vietkata09/graduation-project:latest"

echo "======================================"
echo " Graduation Project Deploy Script"
echo "======================================"

echo ""
echo "🔄 [1/4] Pulling latest Docker image..."
docker pull "$IMAGE"

echo ""
echo "🛑 [2/4] Stopping old containers..."
docker compose -f "$COMPOSE_FILE" down || true

echo ""
echo "🚀 [3/4] Starting new containers..."
docker compose -f "$COMPOSE_FILE" up -d

echo ""
echo "⏳ [4/4] Waiting for app to be healthy (15s)..."
sleep 15

# Kiểm tra health
if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ App healthy! http://localhost:8080"
elif curl -sf http://localhost:8080 > /dev/null 2>&1; then
    echo "✅ App running! http://localhost:8080"
else
    echo "⚠️  App chưa ready, kiểm tra logs:"
    docker compose -f "$COMPOSE_FILE" logs --tail=50 app
fi

echo ""
echo "======================================"
echo " Container Status:"
docker compose -f "$COMPOSE_FILE" ps
echo "======================================"
echo ""
echo "📋 Xem logs: docker compose -f $COMPOSE_FILE logs -f app"
