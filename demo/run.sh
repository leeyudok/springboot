export LANG=C.UTF-8
export GRADLE_OPTS="-Dfile.encoding=utf-8"
./gradlew bootRun -PspringBootRun.systemProperties='spring.profiles.active=local'
