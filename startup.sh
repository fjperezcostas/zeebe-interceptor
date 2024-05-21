#!/bin/bash

YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}[INFO] building JAR artifact...${NC}"
mvn clean package
echo -e "${YELLOW}[INFO] building Docker image...${NC}"
docker build -t fjpc/zeebe:1.0.0 .
echo -e "${YELLOW}[INFO] shutdown current local deployment...${NC}"
docker rm -f zeebe operate elasticsearch tasklist connectors
echo -e "${YELLOW}[INFO] deploy local docker-compose...${NC}"
docker-compose -f docker/docker-compose.yaml up -d
