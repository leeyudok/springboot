#!/bin/sh
# 로컬 실행 — local 프로파일 + UTF-8.
# 사전 조건: docker compose up -d (PostgreSQL, 호스트 5433)
export LANG=C.UTF-8
export GRADLE_OPTS="-Dfile.encoding=utf-8"
./gradlew bootRun -PspringBootRun.systemProperties='spring.profiles.active=local'
