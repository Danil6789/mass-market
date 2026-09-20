#!/usr/bin/env bash
# restart-product.sh — перезапуск product-service с FeignAuthConfig
set -uo pipefail
cd "$(dirname "$0")"
pid=$(cat .pids/product-service.pid 2>/dev/null)
if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
  kill "$pid"
  for _ in 1 2 3 4 5; do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
fi
pkill -f "GradleWrapperMain.*product-service" 2>/dev/null || true
sleep 2
rm -f .pids/product-service.pid
set -a; source .env; set +a
export POSTGRES_PORT=25433
nohup ./gradlew :product-service:bootRun --console=plain > logs/product-service.log 2>&1 &
echo $! > .pids/product-service.pid
disown
echo "started PID=$(cat .pids/product-service.pid)"