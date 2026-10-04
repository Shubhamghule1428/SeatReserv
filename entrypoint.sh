#!/bin/bash
set -e

# 1. Start MySQL daemon in the background using its official entrypoint
docker-entrypoint.sh mysqld &
MYSQL_PID=$!

# 2. Wait until MySQL accepts local connections before Spring Boot attempts to connect
echo "Waiting for MySQL to initialize..."
until mysqladmin ping -h"127.0.0.1" --silent; do
    sleep 2
done
echo "MySQL is alive and accepting connections."

# 3. Start Spring Boot with an explicit heap limit to leave RAM for MySQL
echo "Starting Spring Boot..."
java -Xms128m -Xmx384m -jar /app/app.jar &
APP_PID=$!

# 4. Forward shutdown signals to both processes when Render restarts or redeploys
trap "kill -TERM $APP_PID $MYSQL_PID 2>/dev/null" SIGTERM SIGINT

wait -n