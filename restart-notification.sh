#!/usr/bin/env bash
# restart-notification.sh — перезапуск notification-service с FeignAuthConfig
set -uo pipefail
cd "$(dirname "$0")"
pid=$(cat .pids/notification-service.pid 2>/dev/null)
if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
  kill "$pid"
  for _ in 1 2 3 4 5; do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
fi
pkill -f "GradleWrapperMain.*notification-service" 2>/dev/null || true
sleep 2
rm -f .pids/notification-service.pid
set -a; source .env; set +a
export POSTGRES_PORT=25435
nohup ./gradlew :notification-service:bootRun --console=plain > logs/notification-service.log 2>&1 &
echo $! > .pids/notification-service.pid
disown
echo "started PID=$(cat .pids/notification-service.pid)"