#!/bin/sh
set -e
cd /app
exec ./gradlew bootRun --no-daemon
