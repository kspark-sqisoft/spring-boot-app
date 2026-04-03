#!/bin/sh
set -e
cd /app
exec sh ./gradlew bootRun --no-daemon
