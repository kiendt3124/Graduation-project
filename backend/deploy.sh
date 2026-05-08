#!/bin/bash

# Deploy script cho VPS
echo "Starting deployment..."

# Stop existing containers
docker-compose down

# Pull latest changes (nếu dùng git)
# git pull origin main

# Build và start containers
docker-compose --env-file .env.production up -d --build

# Show running containers
docker-compose ps

echo "Deployment completed!"
echo "Backend running at: http://your-vps-ip:8081"