#!/usr/bin/env bash
# restart-order.sh — перезапуск order-service с FeignAuthConfig
set -uo pipefail
cd "$(dirname "$0")"
pid=$(cat .pids/order-service.pid 2>/dev/null)
if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
  kill "$pid"
  for _ in 1 2 3 4 5; do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
fi
pkill -f "GradleWrapperMain.*order-service" 2>/dev/null || true
sleep 2
rm -f .pids/order-service.pid
set -a; source .env; set +a
export POSTGRES_PORT=25434
nohup ./gradlew :order-service:bootRun --console=plain > logs/order-service.log 2>&1 &
echo $! > .pids/order-service.pid
disown
echo "started PID=$(cat .pids/order-service.pid)"