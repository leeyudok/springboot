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

## 매우중요

```txt
Vscode bash에서 스프링부트 시작시 한글깨짐 현상은
윈도우 국가설정에서 utf8 설정체크 후 재부팅해야 함 - ㅆㅂ 혈압상승
```
