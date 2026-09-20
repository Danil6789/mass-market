#!/usr/bin/env bash
# restart-admin.sh — перезапуск admin-service с FeignAuthConfig
set -uo pipefail
cd "$(dirname "$0")"
pid=$(cat .pids/admin-service.pid 2>/dev/null)
if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
  kill "$pid"
  for _ in 1 2 3 4 5; do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
fi
pkill -f "GradleWrapperMain.*admin-service" 2>/dev/null || true
sleep 2
rm -f .pids/admin-service.pid
set -a; source .env; set +a
export POSTGRES_PORT=25436
nohup ./gradlew :admin-service:bootRun --console=plain > logs/admin-service.log 2>&1 &
echo $! > .pids/admin-service.pid
disown
echo "started PID=$(cat .pids/admin-service.pid)"