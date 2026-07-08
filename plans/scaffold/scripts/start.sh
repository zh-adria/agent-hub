#!/bin/bash
echo "=== 启动 AgentHub 开发环境 ==="
docker-compose up -d
echo "等待服务启动..."
sleep 30
echo "服务状态:"
docker-compose ps
echo ""
echo "访问地址:"
echo "  前端: http://localhost:3000"
echo "  后端: http://localhost:8080"
echo "  MySQL: localhost:3306 (agenthub/agenthub123)"
echo "  Redis: localhost:6379"
echo "  Qdrant: http://localhost:6333"
echo "  RocketMQ: localhost:9876"