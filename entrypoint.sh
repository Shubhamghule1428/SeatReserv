#!/bin/bash
set -e

# 1. Start MySQL daemon in the background
docker-entrypoint.sh mysqld &
MYSQL_PID=$!

# 2. Wait for MySQL to become ready
echo "Waiting for MySQL to initialize..."
until mysqladmin ping -h"127.0.0.1" --silent; do
    sleep 2
done
echo "MySQL is alive and accepting connections."

# 3. Start Spring Boot with SerialGC and strict memory ceilings
echo "Starting Spring Boot..."
java -XX:+UseSerialGC \
     -Xms64m \
     -Xmx180m \
     -XX:MaxMetaspaceSize=110m \
     -XX:CompressedClassSpaceSize=24m \
     -Xss256k \
     -jar /app/app.jar &
APP_PID=$!

# 4. Graceful termination handler
trap "kill -TERM $APP_PID $MYSQL_PID 2>/dev/null" SIGTERM SIGINT

wait -n