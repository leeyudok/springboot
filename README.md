# springboot

Spring Boot 연습 프로젝트. 실제 애플리케이션은 `demo/` 디렉터리에 있다.

## 스택

- Spring Boot **2.7.18** (Java 8을 지원하는 마지막 버전)
- Java **8** — Gradle toolchain 으로 고정. 로컬에 JDK 8이 없으면 foojay resolver 가 자동 다운로드
- Gradle Wrapper **8.14.3** (`demo/gradlew`)
- Lombok, spring-boot-devtools, springdoc-openapi-ui 1.7.0 (Swagger)

## 실행

```sh
cd demo
./gradlew clean build   # 빌드
./run.sh                # 로컬 실행 (macOS/Linux) — local 프로파일 + UTF-8
run.bat                 # 로컬 실행 (Windows)
```

`run.sh` 는 아래 명령의 래퍼다.

```sh
./gradlew bootRun -PspringBootRun.systemProperties='spring.profiles.active=local'
```

- 포트: **8000** (`application.yml`)
- `local` 프로파일: devtools 자동 재시작 + livereload 활성, `static/` 리소스 캐시 0 (수정 즉시 반영)

## 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| GET | `/` | Hello |
| GET | `/users` | 사용자 목록 |
| GET | `/gen-users` | 사용자 목록 재생성 |
| GET | `/news` | 뉴스 목록 |

- Swagger UI: http://localhost:8000/swagger-ui/index.html
- OpenAPI 스펙: http://localhost:8000/v3/api-docs
- 정적 페이지: `demo/src/main/resources/static/index.htm`

## 구조

```
demo/src/main/java/com/example/demo/
├── DemoApplication.java
├── controller/   HelloController, UserController, NewsController
├── service/      UserService, NewsService
└── dto/          User, NewsArticle
```

## 매우중요

```txt
Vscode bash에서 스프링부트 시작시 한글깨짐 현상은
윈도우 국가설정에서 utf8 설정체크 후 재부팅해야 함 - ㅆㅂ 혈압상승
```
