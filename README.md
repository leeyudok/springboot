# springboot

금융권 API 프로젝트에 범용으로 재사용하는 Spring Boot 베이스. 애플리케이션은 `demo/` 디렉터리에 있다.

표준 응답·에러코드·거래추적·감사로그·마스킹·JWT 인증·트랜잭션 규약이 이미 들어 있어,
새 프로젝트는 `domain/` 아래에 업무 도메인만 추가하면 된다.

## 스택

| 구분 | 선택 | 비고 |
|---|---|---|
| 런타임 | Java **8** | Gradle toolchain 으로 고정, 없으면 foojay resolver 가 자동 다운로드 |
| 프레임워크 | Spring Boot **2.7.18** | Java 8을 지원하는 마지막 라인 |
| 영속 계층 | **MyBatis** 2.3.2 | SQL 을 XML 로 분리 — 실행계획 리뷰·튜닝·감사 대응 |
| DB | **PostgreSQL** (드라이버 42.7.5) | 스키마는 Flyway 로 버전 관리 |
| 보안 | Spring Security + **JWT** (jjwt 0.11.5) | 무상태(STATELESS), 역할 기반 인가 |
| 문서 | springdoc-openapi **1.8.0** | 운영 프로파일에서는 비활성 |
| 로깅 | logback **1.2.13** | 마스킹 레이아웃 + 거래추적 ID |
| 빌드 | Gradle Wrapper **8.14.3** | `demo/gradlew` |
| 테스트 | JUnit 5 + **Testcontainers** 1.19.8 | 실제 PostgreSQL 로 통합테스트 |

## 실행

### 1. DB 기동

```sh
cd demo
docker compose up -d          # 또는 podman compose up -d
```

PostgreSQL 16 이 **호스트 5433** 으로 뜬다 (5432 는 다른 용도로 점유되는 경우가 많아 피했다).
스키마와 시드 데이터는 애플리케이션 기동 시 Flyway 가 적용한다.

### 2. 애플리케이션 기동

```sh
cd demo
./gradlew clean build         # 빌드
./run.sh                      # 로컬 실행 (macOS/Linux)
run.bat                       # 로컬 실행 (Windows)
```

- 포트: **8000**
- 프로파일: `local`(기본) / `dev` / `prod`
- Swagger UI: http://localhost:8000/swagger-ui/index.html
- OpenAPI 스펙: http://localhost:8000/v3/api-docs
- 헬스체크: http://localhost:8000/actuator/health

### 3. 호출 예시

```sh
# 로그인 → 액세스 토큰 획득
TOKEN=$(curl -s -X POST http://localhost:8000/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"loginId":"user01","password":"User1234!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')

# 내 계좌 목록
curl -s http://localhost:8000/api/v1/accounts -H "Authorization: Bearer $TOKEN"

# 이체
curl -s -X POST http://localhost:8000/api/v1/accounts/transfers \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"fromAccountNo":"110-0001-000001","toAccountNo":"110-0001-000002","amount":10000,"memo":"생활비"}'
```

시드 계정 — `admin` / `Passw0rd!` (ROLE_ADMIN), `user01`·`user02` / `User1234!` (ROLE_USER),
`mgr01` / `User1234!` (ROLE_MANAGER). **운영 배포 전 `V2__seed_data.sql` 은 반드시 제거한다.**

## 엔드포인트

| Method | Path | 인가 | 설명 |
|---|---|---|---|
| POST | `/api/v1/auth/login` | 공개 | 로그인 — 액세스/리프레시 토큰 발급 |
| POST | `/api/v1/auth/refresh` | 공개 | 리프레시 토큰으로 재발급 |
| GET | `/api/v1/members` | ADMIN, MANAGER | 회원 목록 (페이징) |
| GET | `/api/v1/members/me` | 인증 | 내 정보 |
| GET | `/api/v1/members/{memberId}` | ADMIN, MANAGER | 회원 단건 |
| POST | `/api/v1/members` | ADMIN | 회원 등록 |
| GET | `/api/v1/accounts` | 인증 | 내 계좌 목록 |
| GET | `/api/v1/accounts/{accountNo}` | 인증(본인) | 계좌 단건 |
| GET | `/api/v1/accounts/{accountNo}/transfers` | 인증(본인) | 거래내역 |
| POST | `/api/v1/accounts/transfers` | 인증(본인) | 계좌 이체 |

## 공통 규약

### 응답 형식

성공·실패 모두 동일한 봉투(envelope)로 내려간다. HTTP 상태코드는 전송 계층 결과,
`code` 는 업무 결과다.

```json
{
  "success": true,
  "code": "0000",
  "message": "정상 처리되었습니다.",
  "data": { },
  "traceId": "6f1c9a2b4d8e4f3a9c0b1d2e3f4a5b6c",
  "timestamp": "2026-07-29 10:11:12.345"
}
```

### 에러코드 체계

`ErrorCode` enum 한 곳에서 관리한다. 앞 2자리가 분류다.

| 대역 | 분류 | 예 |
|---|---|---|
| `00xx` | 공통 | `0004` 리소스 없음 |
| `10xx` | 요청 검증 | `1000` 입력값 오류 |
| `20xx` | 인증/인가 | `2001` 자격증명 불일치, `2004` 권한 없음 |
| `30xx` | 회원 | `3001` 아이디 중복 |
| `40xx` | 계좌/거래 | `4002` 잔액 부족, `4004` 한도 초과 |
| `90xx` | 시스템/연계 | `9000` 시스템 오류 |

컨트롤러·서비스에 `try/catch` 를 두지 않는다. 업무 오류는 `BusinessException` 만 던지고
변환은 `GlobalExceptionHandler` 가 전담한다.

### 거래추적 (traceId)

`TraceIdFilter` 가 요청마다 추적 ID 를 발급하고 MDC·응답 헤더(`X-Trace-Id`)·응답 본문에 심는다.
채널계/EAI 가 보낸 `X-Trace-Id` 가 있으면 그대로 이어받아 시스템 간 거래를 한 줄로 꿴다.
모든 로그 라인에 `[traceId] [userId]` 가 찍힌다.

### 감사로그

자금 이동·개인정보 변경 등 사후 추적이 필요한 메서드에 `@Auditable` 을 붙이면
`audit_log` 테이블에 "누가/언제/어디서/무엇을/결과"가 남는다. 성공·실패 모두 기록하며,
**독립 트랜잭션**(`REQUIRES_NEW`)이라 업무 트랜잭션이 롤백돼도 시도 기록은 남는다.

```java
@Auditable(eventType = "TRANSFER", targetExpression = "fromAccountNo")
public TransferResponse transfer(Long memberId, TransferRequest request) { ... }
```

### 민감정보 마스킹

- **로그** — `MaskingPatternLayout` 이 출력 직전에 주민번호·계좌·카드·전화·이메일·비밀번호를 치환한다.
  애플리케이션 코드가 실수로 찍어도 파일에는 마스킹된 값만 남는다.
- **응답** — DTO 변환 시 `SensitiveMasker.maskXxx` 로 필드 단위 적용 (`MemberResponse` 참고).

### 트랜잭션 규약

- 트랜잭션 경계는 **서비스 계층**에만 둔다. 컨트롤러·매퍼에는 붙이지 않는다.
- 서비스 클래스는 `@Transactional(readOnly = true)` 가 기본, 쓰기 메서드에만 재선언한다.
- 자금 이동은 `AccountService.transfer` 를 참조 구현으로 따른다:
  단일 트랜잭션 / 계좌번호 사전순 행 잠금(교착 방지) / `balance = balance + ?` DB 측 갱신 /
  애플리케이션 검증 + DB CHECK 이중 방어.

## 테스트

```sh
cd demo
./gradlew test
```

- 단위테스트(마스킹 등)는 항상 수행된다.
- 통합테스트는 Testcontainers 로 **실제 PostgreSQL** 을 띄워 Flyway 마이그레이션까지 그대로 검증한다.
  H2 로 대체하지 않는 이유는 `FOR UPDATE`·`numeric` 정밀도·CHECK 제약 동작이 달라
  "테스트는 통과하는데 운영에서 깨지는" 상황을 만들기 때문이다.
- 컨테이너 런타임이 없으면 통합테스트는 **실패가 아니라 건너뜀**으로 처리된다.

podman 사용 시 DOCKER_HOST 를 지정한다.

```sh
podman machine start
export DOCKER_HOST="unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')"
./gradlew test
```

## 구조

```
demo/src/main/
├── java/com/example/demo/
│   ├── DemoApplication.java
│   ├── common/
│   │   ├── audit/      @Auditable, AuditAspect, AuditLogService     감사로그
│   │   ├── config/     MyBatisConfig, JacksonConfig, OpenApiConfig
│   │   ├── error/      ErrorCode, BusinessException, GlobalExceptionHandler
│   │   ├── mask/       SensitiveMasker, MaskingPatternLayout        민감정보 마스킹
│   │   ├── response/   ApiResponse, PageResponse, PageRequestDto    표준 응답
│   │   └── trace/      TraceContext, TraceIdFilter, RequestLoggingFilter
│   ├── security/       SecurityConfig, AuthService, jwt/            인증·인가
│   └── domain/
│       ├── member/     회원
│       └── account/    계좌·이체 (트랜잭션 참조 구현)
└── resources/
    ├── application*.yml            프로파일별 설정
    ├── logback-spring.xml          마스킹 + 거래추적 로깅
    ├── db/migration/               Flyway 마이그레이션
    └── mapper/                     MyBatis SQL (XML)
```

## 새 프로젝트에 적용할 때

1. `com.example.demo` 패키지명을 프로젝트 이름으로 일괄 치환한다
   (`build.gradle` 의 `group`, `application.yml` 의 `mybatis.type-aliases-package` 포함).
2. `domain/member`, `domain/account` 는 예시다. 업무 도메인으로 교체한다.
3. `ErrorCode` 의 `30xx`·`40xx` 대역을 업무 코드로 다시 채운다.
4. `V2__seed_data.sql` 을 제거하고 운영 스키마 마이그레이션을 이어 붙인다.
5. 운영 배포 전 확인:
   - `APP_JWT_SECRET`, `DB_USERNAME`, `DB_PASSWORD` 를 환경변수로 주입 (설정 파일에 평문 금지)
   - `SecurityConfig` 의 CORS 허용 오리진을 실제 채널 도메인으로 축소
   - `prod` 프로파일에서 Swagger 비활성 확인

## 알려진 제약

- 액세스 토큰은 서버측 저장소가 없어 만료 전 강제 폐기가 불가능하다.
  즉시 로그아웃/강제 세션 종료가 요건이면 Redis 기반 블랙리스트(`jti` 기준)를 추가해야 한다.
- 주민등록번호 컬럼(`member.reg_no`)은 애플리케이션 암호화 저장을 전제로 자리만 잡아 뒀다.
  실제 사용 시 KMS 연동 암복호화를 붙인다.

## 트러블슈팅

```txt
Vscode bash에서 스프링부트 시작시 한글깨짐 현상은
윈도우 국가설정에서 utf8 설정체크 후 재부팅해야 함 - ㅆㅂ 혈압상승
```
