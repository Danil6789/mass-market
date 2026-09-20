#!/usr/bin/env bash
# startup.sh — запускает 7 микросервисов в фоне с хоста
# Использование: ./startup.sh
# Требует: инфра (postgres×5, kafka, mailhog, zipkin) уже поднята через docker-compose.infra.yml

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PIDS_DIR="$ROOT_DIR/.pids"
LOGS_DIR="$ROOT_DIR/logs"
mkdir -p "$PIDS_DIR" "$LOGS_DIR"

# Загружаем .env (там правильные SPRING_DATASOURCE_URL_* с портами 25432-25436
# под Windows Hyper-V резервирование 5432-5435)
if [ -f "$ROOT_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
  echo "==> .env загружен"
else
  echo "WARN: $ROOT_DIR/.env не найден, сервисы будут использовать defaults"
fi

# Порядок: eureka первым (другие регистрируются в нём), затем gateway, затем бизнес-сервисы.
# postgres_port — хост-порт, на который docker-compose мапит контейнерный 5432
# (25432-25436 чтобы обойти Hyper-V резервирование 5425-5524 на Windows).
SERVICES=(
  "eureka-server:8761:postgres-na"
  "api-gateway:8080:postgres-na"
  "user-service:8081:25432"
  "product-service:8082:25433"
  "order-service:8083:25434"
  "notification-service:8084:25435"
  "admin-service:8085:25436"
)

start_service() {
  local module="$1"
  local port="$2"
  local pg_port="$3"
  local pid_file="$PIDS_DIR/${module}.pid"
  local log_file="$LOGS_DIR/${module}.log"

  # Если уже запущен — пропускаем
  if [ -f "$pid_file" ]; then
    local old_pid
    old_pid=$(cat "$pid_file")
    if kill -0 "$old_pid" 2>/dev/null; then
      echo "[$module] уже запущен (PID=$old_pid)"
      return 0
    fi
    rm -f "$pid_file"
  fi

  echo "[$module] запускаю..."
  cd "$ROOT_DIR"
  # Экспортируем per-service POSTGRES_PORT (env из .env + override)
  export POSTGRES_PORT="$pg_port"
  # Запускаем gradle bootRun в фоне, логируем в файл
  (./gradlew ":${module}:bootRun" --console=plain > "$log_file" 2>&1) &
  local pid=$!
  echo "$pid" > "$pid_file"
  echo "[$module] PID=$pid, POSTGRES_PORT=$pg_port, логи: $log_file"
}

# Очищаем старые PID-файлы мёртвых процессов
for svc_port in "${SERVICES[@]}"; do
  module="${svc_port%%:*}"
  pid_file="$PIDS_DIR/${module}.pid"
  if [ -f "$pid_file" ]; then
    old_pid=$(cat "$pid_file")
    if ! kill -0 "$old_pid" 2>/dev/null; then
      rm -f "$pid_file"
    fi
  fi
done

# Запускаем eureka первым, ждём 30с чтобы он точно поднялся
start_service "eureka-server" "8761" "postgres-na"
echo "==> Ждём 30с чтобы eureka поднялся..."
sleep 30

# Проверяем что eureka healthy
if ! curl -sf http://localhost:8761/actuator/health -o /dev/null; then
  echo "FAIL: eureka не отвечает на :8761"
  exit 1
fi
echo "==> eureka UP"

# Запускаем остальные 6 последовательно (параллельный старт создаёт конкуренцию
# за gradle daemons и может приводить к deadlock). Каждый сервис ~30-40с на старт.
for svc_def in "${SERVICES[@]:1}"; do
  module="${svc_def%%:*}"
  rest="${svc_def#*:}"
  port="${rest%%:*}"
  pg_port="${rest##*:}"
  start_service "$module" "$port" "$pg_port"
  # Ждём чтобы предыдущий сервис точно поднялся, прежде чем стартовать следующий
  sleep 35
done

echo
echo "==> Все 7 сервисов запущены в фоне."
echo "==> Проверить Eureka dashboard: http://localhost:8761"
echo "==> Логи: $LOGS_DIR/<service>.log"
echo "==> PIDs: $PIDS_DIR/<service>.pid"
echo "==> Для остановки: ./shutdown.sh"