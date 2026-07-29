# springboot

여러 프로젝트에 범용으로 재사용하는 Spring Boot 백엔드 베이스. 애플리케이션은 `demo/` 디렉터리에 있다.

표준 응답·에러코드·요청추적·감사로그·마스킹·JWT 인증·스케줄러·캐시·트랜잭션 규약이 이미 들어 있어,
새 프로젝트는 `sample/` 을 지우고 `domain/` 아래에 업무 도메인만 추가하면 된다.

## 스택

| 구분 | 선택 | 비고 |
|---|---|---|
| 런타임 | Java **8** | Gradle toolchain 으로 고정, 없으면 foojay resolver 가 자동 다운로드 |
| 프레임워크 | Spring Boot **2.7.18** | Java 8을 지원하는 마지막 라인 |
| 영속 계층 | **MyBatis** 2.3.2 | SQL 을 XML 로 분리 — 실행계획 리뷰·튜닝·감사 대응 |
| DB | **PostgreSQL** (드라이버 42.7.5) | 스키마는 Flyway 로 버전 관리 |
| 보안 | Spring Security + **JWT** (jjwt 0.11.5) | 무상태(STATELESS), 역할 기반 인가 |
| 스케줄러 | **Quartz** (JDBC JobStore) | 재기동에도 트리거 보존, 다중 인스턴스에서 1회만 실행 |
| 캐시 | **Caffeine** 2.9.3 | 3.x 는 Java 11+ 요구 |
| 문서 | springdoc-openapi **1.8.0** | 운영 프로파일에서는 비활성 |
| 로깅 | logback **1.2.13** | 마스킹 레이아웃 + 요청추적 ID |
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

# 내 계정 목록 (샘플)
curl -s http://localhost:8000/api/v1/accounts -H "Authorization: Bearer $TOKEN"

# 잔액 이동 (샘플)
curl -s -X POST http://localhost:8000/api/v1/accounts/transfers \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"fromAccountNo":"110-0001-000001","toAccountNo":"110-0001-000002","amount":10000,"memo":"생활비"}'
```

시드 계정 — `admin` / `Passw0rd!` (ROLE_ADMIN), `user01`·`user02` / `User1234!` (ROLE_USER),
`mgr01` / `User1234!` (ROLE_MANAGER). **운영 배포 전 `V3__seed_data.sql` 은 반드시 제거한다.**

## 엔드포인트

| Method | Path | 인가 | 설명 |
|---|---|---|---|
| POST | `/api/v1/auth/login` | 공개 | 로그인 — 액세스/리프레시 토큰 발급 |
| POST | `/api/v1/auth/refresh` | 공개 | 리프레시 토큰으로 재발급 |
| GET | `/api/v1/members` | ADMIN, MANAGER | 회원 목록 (페이징) |
| GET | `/api/v1/members/me` | 인증 | 내 정보 |
| GET | `/api/v1/members/{memberId}` | ADMIN, MANAGER | 회원 단건 |
| POST | `/api/v1/members` | ADMIN | 회원 등록 |
| GET | `/api/v1/accounts` | 인증 | ★ 샘플 — 내 계정 목록 |
| GET | `/api/v1/accounts/{accountNo}` | 인증(본인) | ★ 샘플 — 계정 단건 |
| GET | `/api/v1/accounts/{accountNo}/transfers` | 인증(본인) | ★ 샘플 — 이동 내역 |
| POST | `/api/v1/accounts/transfers` | 인증(본인) | ★ 샘플 — 잔액 이동 |

★ 는 `sample/ledger` 참조 구현이다. 새 프로젝트에서는 삭제한다.

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

앞 2자리가 분류다. 공통 대역은 `ErrorCode` 가 고정으로 갖고, 업무 대역은 `ErrorCodeSpec` 을 구현한 도메인별 enum 에 둔다.
공통 계층이 도메인 지식을 갖지 않아야 새 프로젝트에서 도메인 enum 만 갈아끼울 수 있다.

| 대역 | 분류 | 정의 위치 | 예 |
|---|---|---|---|
| `00xx` | 공통 | `ErrorCode` | `0004` 리소스 없음 |
| `10xx` | 요청 검증 | `ErrorCode` | `1000` 입력값 오류 |
| `20xx` | 인증/인가 | `ErrorCode` | `2001` 자격증명 불일치, `2004` 권한 없음 |
| `90xx` | 시스템/연계 | `ErrorCode` | `9000` 시스템 오류 |
| `30xx` | 회원 | `MemberErrorCode` | `3001` 아이디 중복 |
| `40xx` | 원장(샘플) | `LedgerErrorCode` | `4002` 잔액 부족, `4004` 한도 초과 |

컨트롤러·서비스에 `try/catch` 를 두지 않는다. 업무 오류는 `BusinessException` 만 던지고
변환은 `GlobalExceptionHandler` 가 전담한다.

### 요청추적 (traceId)

`TraceIdFilter` 가 요청마다 추적 ID 를 발급하고 MDC·응답 헤더(`X-Trace-Id`)·응답 본문에 심는다.
상위 시스템이 보낸 `X-Trace-Id` 가 있으면 그대로 이어받아 시스템 간 요청을 한 줄로 꿴다.
모든 로그 라인에 `[traceId] [userId]` 가 찍힌다.

**클라이언트 IP 는 설정으로 지정한 헤더 하나만 신뢰한다** (`app.trace.client-ip-header`, 기본 `X-Real-IP`).
`X-Forwarded-For` 는 기존 값 뒤에 이어 붙이는 헤더라 클라이언트가 미리 넣어 보낸 값이 맨 앞에 남는다.
그대로 쓰면 감사로그 IP 를 위조할 수 있고, IP 기준 차단·유량제어를 우회하거나 반대로 타인의 IP 를
지목해 잠그는 서비스 거부가 가능하다. **프록시가 해당 헤더를 무조건 덮어쓸 때만** 값을 지정하고
(nginx `proxy_set_header X-Real-IP $remote_addr;`), 직접 노출 환경이면 빈 값으로 둔다.

### 감사로그

자원 이동·개인정보 변경 등 사후 추적이 필요한 메서드에 `@Auditable` 을 붙이면
`audit_log` 테이블에 "누가/언제/어디서/무엇을/결과"가 남는다. 성공·실패 모두 기록한다.

```java
@Auditable(eventType = "TRANSFER", targetExpression = "fromAccountNo",
           params = {"fromAccountNo", "toAccountNo", "amount"})
public TransferResponse transfer(Long memberId, TransferRequest request) { ... }
```

- **`params` 는 화이트리스트다.** 지정한 필드만 기록하고 비워두면 아무것도 남기지 않는다.
  전체 파라미터를 직렬화한 뒤 마스킹으로 걸러내는 방식은, 마스킹 패턴에 걸리지 않는 새로운 형태의
  비밀값(API 키, 일회용 인증번호)이 추가되는 순간 그대로 평문 적재된다.
- **적재는 비동기**(`@Async`)라 감사 기록의 DB 왕복이 업무 응답시간에 얹히지 않는다.
  실행기는 큐가 차면 호출 스레드가 대신 실행하는 정책이라 기록이 유실되지는 않는다.
- **독립 트랜잭션**(`REQUIRES_NEW`)이라 업무 트랜잭션이 롤백돼도 시도 기록은 남는다.
- 보존기간이 지난 행은 Quartz 정리 배치가 삭제한다 (아래 참고).

### 민감정보 마스킹

- **로그** — `MaskingPatternLayout` 이 출력 직전에 주민번호·계좌·카드·전화·이메일·비밀번호를 치환한다.
  애플리케이션 코드가 실수로 찍어도 파일에는 마스킹된 값만 남는다.
- **응답** — DTO 변환 시 `SensitiveMasker.maskXxx` 로 필드 단위 적용 (`MemberResponse` 참고).

### 트랜잭션 규약

- 트랜잭션 경계는 **서비스 계층**에만 둔다. 컨트롤러·매퍼에는 붙이지 않는다.
- 서비스 클래스는 `@Transactional(readOnly = true)` 가 기본, 쓰기 메서드에만 재선언한다.
- **장시간 외부 I/O 는 트랜잭션 밖으로 뺀다.** 외부 API 호출·파일 전송을 `@Transactional` 안에 두면
  응답을 기다리는 내내 DB 커넥션을 점유한다. 동시 요청이 몇 건만 겹쳐도 커넥션 풀이 고갈되고
  그 순간 서비스 전체가 멈춘다. 외부 호출은 트랜잭션 밖에서 끝내고 그 결과만 짧은 트랜잭션으로 반영한다
  (불가피하면 `Propagation.NOT_SUPPORTED`).
- **자기호출(self-invocation)에는 트랜잭션 속성이 걸리지 않는다.** 같은 빈의 메서드를 직접 부르면
  스프링 프록시를 거치지 않아 `@Transactional`·`@Async` 가 조용히 무시된다.
  전파 속성을 바꿔야 하는 메서드는 별도 빈으로 분리한다
  (`AuditLogChunkDeleter`, `LoginAttemptService` 참고).
- 동시성 있는 수량 갱신은 `sample/ledger` 를 참조 구현으로 따른다:
  단일 트랜잭션 / 키 사전순 행 잠금(교착 방지) / `balance = balance + ?` DB 측 갱신 /
  애플리케이션 검증 + DB CHECK 이중 방어.

### SQL 규약

- **동적 정렬은 허용 목록 분기로만 만든다.** 컬럼명·정렬방향은 `#{}` 로 바인딩할 수 없고
  `${}` 로 이어 붙이면 그 순간 SQL 인젝션 통로가 된다. 클라이언트가 보낸 정렬 키를
  `<choose>` 로 검사해 정해진 `ORDER BY` 만 emit 한다 (`MemberMapper.xml` 의 `listOrderBy`).
- `SELECT *` 를 쓰지 않는다. 컬럼 목록은 `<sql>` 조각으로 재사용한다.
- **대량 삭제는 청크로 끊는다.** 한 문장으로 수백만 행을 지우면 롱 트랜잭션이 되고,
  그동안 잡은 락과 불어난 WAL 이 다른 처리·복제까지 밀어낸다. PostgreSQL 의 `DELETE` 에는
  `LIMIT` 절이 없으므로 PK 서브쿼리로 상한을 건다 (`AuditLogMapper.xml` 의 `deleteOlderThan`).
- `mapper-locations` 는 반드시 `classpath*:` (별표)로 둔다. 별표가 없으면 `mapper/` 를 가진
  클래스패스 루트 중 **처음 하나만** 훑기 때문에, 테스트 리소스에 `mapper/` 가 생기는 순간
  운영 매퍼 XML 이 통째로 무시되고 `Invalid bound statement` 로 터진다.

### 스케줄러 / 배치

`@Scheduled` 대신 Quartz + JDBC JobStore 를 쓴다. 트리거 상태가 DB 에 있어 재기동해도 스케줄이
보존되고, 인스턴스를 여러 대 띄워도 잡이 한 번만 실행된다(`isClustered`). `@Scheduled` 는
인스턴스마다 각자 돌아 중복 실행된다.

기본 제공 잡은 감사로그 보존기간 정리 하나다(`AuditLogRetentionJob`, 매일 03:30).
새 잡은 `QuartzConfig` 에 `JobDetail` + `Trigger` 를 추가하고 크론은 설정 파일로 뺀다.
misfire(실행 시각을 놓침) 정책을 반드시 명시한다 — 기본 잡은
`withMisfireHandlingInstructionDoNothing` 이라 배포·장애로 놓친 회차를 몰아서 실행하지 않는다.
정리 배치가 밀린 회차까지 한꺼번에 돌면 재기동 직후 가장 바쁜 시점에 대량 삭제가 겹친다.

Quartz 스키마는 `V5__quartz_schema.sql` 로 관리한다. Quartz jar 에 들어 있는 DDL 을 옮기되
상단의 `DROP TABLE` 구문은 제거했다 — 재실행 시 기존 잡·트리거가 날아가면 안 된다.

### 캐시

Caffeine 로컬 캐시를 용도별 매니저로 나눠 둔다 (`CacheConfig`) — 짧은 주기 1분(`SHORT_TERM`),
준정적 데이터 1시간(`LONG_TERM`). 매니저 하나에 TTL 하나만 두면 "10분이면 충분한 것"과
"1분도 긴 것"이 같은 값을 쓰게 된다.

인스턴스별 로컬 캐시라 스케일아웃하면 인스턴스마다 값이 다를 수 있고 무효화도 전파되지 않는다.
정합성이 중요한 데이터는 캐시하지 말거나 Redis 같은 공유 캐시로 옮긴다.

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
│   │   ├── audit/      @Auditable, AuditAspect, 보존기간 정리 배치     감사로그
│   │   ├── config/     MyBatis, Jackson, OpenApi, Async, Quartz, Cache
│   │   ├── error/      ErrorCodeSpec, ErrorCode, BusinessException,
│   │   │               GlobalExceptionHandler
│   │   ├── mask/       SensitiveMasker, MaskingPatternLayout        민감정보 마스킹
│   │   ├── response/   ApiResponse, PageResponse, PageRequestDto    표준 응답
│   │   └── trace/      TraceContext, TraceIdFilter, RequestLoggingFilter
│   ├── security/       SecurityConfig, AuthService, jwt/            인증·인가
│   ├── domain/
│   │   └── member/     회원 — 범용 CRUD 참조
│   └── sample/         ★ 새 프로젝트는 통째로 삭제
│       └── ledger/     동시성 갱신 참조 구현 (행 잠금 + DB측 산술 갱신)
└── resources/
    ├── application*.yml            프로파일별 설정
    ├── logback-spring.xml          마스킹 + 요청추적 로깅
    ├── db/migration/               Flyway 마이그레이션
    │   ├── V1  코어 스키마 (member, audit_log)
    │   ├── V2  ★ 원장 샘플 스키마
    │   ├── V3  회원 시드
    │   ├── V4  ★ 원장 샘플 시드
    │   └── V5  Quartz 스키마
    └── mapper/                     MyBatis SQL (XML)
```

## 새 프로젝트에 적용할 때

1. **샘플을 지운다** (★ 표시):
   ```sh
   rm -rf demo/src/main/java/com/example/demo/sample
   rm -rf demo/src/test/java/com/example/demo/sample
   rm demo/src/main/resources/mapper/AccountMapper.xml
   rm demo/src/main/resources/db/migration/V2__sample_ledger_schema.sql
   rm demo/src/main/resources/db/migration/V4__sample_ledger_seed.sql
   ```
2. `com.example.demo` 패키지명을 프로젝트 이름으로 일괄 치환한다
   (`build.gradle` 의 `group`, `application.yml` 의 `mybatis.type-aliases-package` 포함).
3. `domain/member` 는 도메인 작성 예시다. 업무 도메인으로 교체하거나 그대로 쓴다.
   도메인 에러코드는 `MemberErrorCode` 처럼 `ErrorCodeSpec` 구현 enum 으로 대역을 하나씩 배정한다.
4. `V3__seed_data.sql` 을 제거하고 운영 스키마 마이그레이션을 이어 붙인다.
5. 운영 배포 전 확인:
   - `APP_JWT_SECRET`, `DB_USERNAME`, `DB_PASSWORD` 를 환경변수로 주입 (설정 파일에 평문 금지)
   - `SecurityConfig` 의 CORS 허용 오리진을 실제 서비스 도메인으로 축소
   - `app.trace.client-ip-header` 가 실제 프록시 구성과 맞는지 확인 (없으면 빈 값으로)
   - `app.audit.retention-days` 를 적용 도메인의 법정 보존기간에 맞춰 조정
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
