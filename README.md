# springboot

스프링부트

## java1.8

## gradle

```sh
# gradle wrapper --gradle-version 7.6.3
gradle wrapper --gradle-version 8.5 #java21 에서 사용
./gradlew clean build # 빌드
./gradlew bootRun     # 실행
```

### local실행

```sh
./gradlew bootRun -Dspring.profiles.active=local
./gradlew bootRun -PspringBootRun.systemProperties='spring.profiles.active=local'
```
