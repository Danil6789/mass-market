#!/usr/bin/env bash
# restart-user.sh — перезапуск user-service с FeignAuthConfig
set -uo pipefail
cd "$(dirname "$0")"
pid=$(cat .pids/user-service.pid 2>/dev/null)
if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
  kill "$pid"
  for _ in 1 2 3 4 5; do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
fi
pkill -f "GradleWrapperMain.*user-service" 2>/dev/null || true
sleep 2
rm -f .pids/user-service.pid
set -a; source .env; set +a
export POSTGRES_PORT=25432
nohup ./gradlew :user-service:bootRun --console=plain > logs/user-service.log 2>&1 &
echo $! > .pids/user-service.pid
disown
echo "started PID=$(cat .pids/user-service.pid)"