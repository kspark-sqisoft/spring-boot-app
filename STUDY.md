# Spring Boot 게시판 + React (Docker) 학습 노트

이 저장소는 `d:\Study\Docker\docker_app` 과 **동일한 HTTP API 계약**(경로·JSON 필드명·인증 방식)을 유지하면서, 백엔드만 **Java 17 + Spring Boot 4** 로 구현한 예제입니다. 프론트엔드는 React 19, Tailwind CSS 4, shadcn/ui, TanStack Query 등 기존 스택을 그대로 사용합니다.

**프론트 SPA**는 로그인·회원가입·프로필·게시판(목록·상세·작성·수정) UI를 포함합니다. 백엔드 API는 **Swagger UI**로도 호출·스펙 확인이 가능합니다(아래 Swagger 절).

---

## 1. 아키텍처 한눈에

| 구분 | 기술 |
|------|------|
| API | Spring Web MVC, 전역 prefix 없음(컨트롤러에서 `/api/...`) |
| DB | PostgreSQL 16, JPA(Hibernate), `ddl-auto=update`(학습용) |
| 보안 | Spring Security + Stateless 세션 |
| 액세스 토큰 | JWT(HS256), `Authorization: Bearer` |
| 리프레시 | 랜덤 토큰 → SHA-256 해시만 DB 저장, **httpOnly 쿠키** (`path=/api/auth`) |
| 정적 업로드 | 디스크 `UPLOADS_DIR` → URL `/uploads/profiles/...`, `/uploads/posts/...` |

인증 흐름은 NestJS 샘플과 같습니다.

- 회원가입/로그인/리프레시 시 응답 JSON에 `accessToken` + `user`, 동시에 리프레시 쿠키 설정.
- 프론트는 `fetch(..., { credentials: 'include' })` 로 쿠키 전송(이미 `docker_app` 클라이언트 코드에 반영됨).

---

## 2. 로컬에서 백엔드만 실행

1. **PostgreSQL 이 먼저 떠 있어야 합니다.** 기본 설정은 `localhost:5432`, DB 이름 `board`, 사용자 `board`, 비밀번호 `board` 입니다(`application.properties` / 환경 변수 `DB_*` 와 일치해야 함).
2. 터미널:

```powershell
Set-Location D:\Study\SpringBoot\spring-boot-app\backend
.\gradlew.bat bootRun
```

기본 포트는 **3000** (`SERVER_PORT`).  
프론트(Vite)는 `/api`, `/uploads` 를 같은 포트로 프록시하므로, 프론트만 켤 때는 `vite.config.ts` 의 `VITE_API_PROXY_TARGET` 이 `http://localhost:3000` 이면 됩니다.

**DB만 Docker로 켜고, 백엔드는 호스트에서 `bootRun` 하고 싶을 때** (프로젝트 루트에서):

```powershell
Set-Location D:\Study\SpringBoot\spring-boot-app
docker compose -f docker-compose.dev.yml up db -d
```

이후 다시 `backend` 폴더에서 `.\gradlew.bat bootRun` 합니다. (Compose DB 기본 계정이 `board` / `board` / DB `board` 와 맞습니다.)

---

## 3. Docker Compose — 운영 스타일 (`docker-compose.yml`)

Docker 실행 패턴·포트·부분 기동 등은 **[DOCKER.md](./DOCKER.md)** 에 정리해 두었습니다.

멀티 스테이지 **Dockerfile** 로 백엔드를 JAR 실행하고, 프론트는 **빌드 결과물을 nginx** 로 서빙합니다. 핫 리로드는 없습니다.

```powershell
Set-Location D:\Study\SpringBoot\spring-boot-app
docker compose up --build
```

- DB: `localhost:5432`
- API: `http://localhost:3000`
- 웹(nginx): `http://localhost:8080` → API/업로드는 nginx 가 `backend:3000` 으로 리버스 프록시

업로드 파일은 볼륨 `backend_uploads` 에 유지됩니다.

**CORS**: 운영 compose 에서는 `CORS_ORIGIN` 기본값으로 `http://localhost:8080` 계열을 허용합니다. 실제 배포 도메인에 맞게 `.env` 로 바꿉니다.

---

## 4. Docker Compose — 개발 스타일 (`docker-compose.dev.yml`)

(명령 모음·포트 표는 [DOCKER.md](./DOCKER.md) 참고.)

소스 디렉터리를 컨테이너에 마운트합니다.

- **백엔드**: `./gradlew bootRun` + **Spring DevTools** → 클래스패스 변경 시 자동 재시작(핫 리로드에 가까운 경험).
- **프론트**: Vite dev 서버(`5173`, 동시에 `8080` 포트로도 접근 가능).

```powershell
docker compose -f docker-compose.dev.yml up --build
```

첫 기동 시 Gradle 이 의존성을 받느라 백엔드 healthcheck 가 **최대 수 분** 걸릴 수 있습니다(`start_period` 를 넉넉히 잡아 두었습니다).

볼륨:

- `gradle_cache`: Gradle 캐시 재사용
- `frontend_node_modules`: `node_modules` 를 호스트와 분리
- `backend_uploads_dev`: 업로드 파일

---

## 5. 백엔드 패키지 구조 (학습용)

기능(feature)마다 `controller` / `service` / `repository` / `entity` / `dto` 를 두는 형태입니다(루트 패키지는 `com.noa99kee.board`).

- `config` — `SecurityConfig`, `WebConfig`(CORS, `/uploads` 리소스 핸들러), JWT/업로드 설정 바인딩, `OpenApiConfig`(Swagger UI용 OpenAPI 메타·JWT Bearer 스키마)
- `auth` — `controller`(회원가입·로그인·로그아웃·리프레시), `service`(`AuthService`, `JwtService`), `dto`, `filter`(`JwtAuthenticationFilter`), `principal`(`BoardUserPrincipal`), `util`(`TokenHasher`)
- `user` — `controller`(`GET/PATCH/POST /api/auth/me` … 프론트 계약 유지), `service`, `repository`, `entity`, `dto`
- `post` — `controller`, `service`, `repository`, `entity`, `dto`(게시글 CRUD·이미지 URL 검증, `/uploads/posts/` 만 허용)
- `health` — `controller`(`GET /api/health`)
- `common` — 전역 예외 처리(`message`, `statusCode`, `path`, `timestamp`)

보안 규칙 요약:

- 공개: `GET /api/health`, `GET /api/posts`, `GET /api/posts/{id}`, `POST /api/auth/register|login|refresh`, `/uploads/**`
- 인증 필요: 글 작성·수정·삭제, 이미지 업로드, 프로필, 로그아웃 등

### 5.1 Swagger UI (OpenAPI 3)

백엔드에 **SpringDoc OpenAPI**(`springdoc-openapi-starter-webmvc-ui`)를 두어, 런타임에 스펙을 생성하고 **Swagger UI**로 브라우저에서 API를 읽고 호출할 수 있습니다.

**`/v3/api-docs` 는 사람용 HTML이 아니라 스펙 원본(JSON)** 이라서 브라우저에 JSON만 보이는 것이 정상입니다. 버튼·설명이 있는 문서는 **Swagger UI** 주소로 엽니다.

| 항목 | 내용 |
|------|------|
| UI 주소 | `http://호스트:포트/swagger-ui/index.html` (기본 포트는 `SERVER_PORT`, 보통 `3000`). `/swagger-ui.html` 로 들어가도 UI로 연결됩니다. |
| 스펙 JSON | `http://호스트:포트/v3/api-docs` — 기계·도구용. Swagger UI가 내부적으로 이 URL에서 스펙을 불러옵니다. |
| Gradle | `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2` (`build.gradle`의 `dependencies`) |
| 설정 | `application.properties` — `springdoc.swagger-ui.path=/swagger-ui`, `springdoc.api-docs.path=/v3/api-docs` |
| 보안 | `SecurityConfig`에서 `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`, `/v3/api-docs/**` 는 **인증 없이 허용**(`permitAll`)합니다. 그 외 API 규칙은 기존과 동일합니다. |
| JWT 시험 | UI 상단 **Authorize** → `bearerAuth`에 액세스 토큰만 입력하거나 `Bearer ` 접두사를 포함해 입력(SpringDoc이 스키마로 `HTTP Bearer`를 노출). 토큰은 `POST /api/auth/login` 또는 `register` 응답 JSON의 `accessToken`을 복사합니다. |
| 코드 | `com.noa99kee.board.config.OpenApiConfig` — API 제목·설명·버전과 Bearer 스키마 이름(`bearerAuth`) 정의. |
| IDE | 일부 IDE는 전이 의존성만으로 `io.swagger.v3.oas.models` 를 못 잡을 수 있어, `io.swagger.core.v3:swagger-models-jakarta:2.2.43` 을 **직접** `implementation` 으로 선언해 두었습니다. |
| 로그 | `HttpRequestLoggingFilter`에서 `/swagger-ui`, `/v3/api-docs` 요청은 헬스와 같이 **DEBUG**만 남기도록 해 두어 INFO 로그가 지나치게 늘지 않게 했습니다. |

**운영 배포 시**에는 문서·스펙을 외부에 노출하지 않으려면 프로필로 끄는 것이 일반적입니다.

```properties
# 예: application-prod.properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

로컬·학습용 기본 `application.properties` 에는 위 비활성화를 넣지 않았습니다.

---

## 6. 환경 변수 요약

| 변수 | 설명 |
|------|------|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL |
| `SERVER_PORT` | 기본 3000 |
| `JWT_ACCESS_SECRET` | HS256 비밀키, **UTF-8 기준 32바이트 이상** |
| `JWT_ACCESS_EXPIRES_MINUTES` | 액세스 TTL(분) |
| `JWT_REFRESH_EXPIRES_DAYS` | 리프레시 쿠키 max-age |
| `REFRESH_COOKIE_SECURE` | HTTPS 에서만 true 권장 |
| `UPLOADS_DIR` | 업로드 루트(컨테이너에서는 `/app/uploads`) |
| `CORS_ORIGIN` | 쉼표로 구분한 허용 Origin; 비우면 로컬 개발용 기본 목록 |

루트 `.env.example` 를 참고해 `.env` 를 만들면 Compose 가 자동으로 읽습니다.

---

## 7. 로깅 레벨과 필터링 (설정에 따라 무엇이 출력되나)

Spring Boot 기본 로깅은 **Logback** 이고, 애플리케이션 코드에서는 보통 **SLF4J**(`Logger`)로 레벨만 지정합니다. **설정한 로거의 레벨이 “문지기”** 역할을 해서, 그보다 **덜 중요한** 로그는 **아예 출력되지 않습니다**(필터링).

### 7.1 TRACE, DEBUG, INFO … 각 단계가 의미하는 것

같은 “로그”라도 **얼마나 자주·얼마나 잘게** 찍을지에 따라 레벨을 나눕니다. 위로 갈수록 **더 잘게·더 많이**, 아래로 갈수록 **덜 자주·더 중요한 것만** 쓰는 식으로 생각하면 됩니다.

| 레벨 | 단계(느낌) | 언제 쓰기 좋은지 | 코드 |
|------|------------|------------------|------|
| `TRACE` | **가장 잘게** 쪼갠 흐름 | 메서드 진입/루프 내부, 프레임워크가 내부 단계를 전부 보여 줄 때 수준. 평소에는 거의 끈다. | `log.trace` |
| `DEBUG` | **개발·원인 분석**용 상세 | 변수 값, 쿼리 건수, “어떤 분기 탔는지” 등. 운영 기본값에는 보통 안 나온다. | `log.debug` |
| `INFO` | **정상 업무·기동** 요약 | 서버 기동 완료, 주요 요청 한 줄 요약, 배치 시작/종료. 운영에서도 자주 남기는 레벨. | `log.info` |
| `WARN` | **이상 징후** | 복구 가능한 문제, 곧 막힐 수 있는 설정, 재시도. | `log.warn` |
| `ERROR` | **실패** | 예외 처리 구간, 처리 불가 오류. | `log.error` |
| `OFF` | **출력 끔** | 해당 로거는 아무 것도 안보냄. | — |

**필터 규칙(한 로거에 레벨을 `L` 로 둔 경우)**  
그 로거로 찍힌 이벤트의 레벨이 **`L` 과 같거나 더 심각**(숫자로는 더 크다)이면 통과합니다.  
즉 `L = INFO` 이면 **나옴**: `INFO`, `WARN`, `ERROR` · **안 나옴**: `TRACE`, `DEBUG`.

정리하면 **“설정 레벨 = 문지기 높이”** 이고, 그보다 **더 장황한**(TRACE/DEBUG 쪽) 로그는 잘립니다.

### 7.2 `logging.level.root` 를 이렇게 두면, 코드의 `log.xxx` 중 무엇이 나오나

**한 로거에 대한 설정이 `root` 하나뿐**이고, 다른 `logging.level.xxx` 로 덮어쓰지 않았다고 가정한 표입니다. (실제로는 패키지·클래스별로 더 구체적인 설정이 있으면 **그쪽이 우선**합니다.)

| 설정 `logging.level.root` | `log.trace` | `log.debug` | `log.info` | `log.warn` | `log.error` |
|----------------------------|:-----------:|:-------------:|:----------:|:----------:|:-----------:|
| `TRACE` | 나옴 | 나옴 | 나옴 | 나옴 | 나옴 |
| `DEBUG` | **안 나옴** | 나옴 | 나옴 | 나옴 | 나옴 |
| `INFO` (Spring Boot 가 묵시적으로 잡는 경우가 많음) | 안 나옴 | 안 나옴 | 나옴 | 나옴 | 나옴 |
| `WARN` | 안 나옴 | 안 나옴 | 안 나옴 | 나옴 | 나옴 |
| `ERROR` | 안 나옴 | 안 나옴 | 안 나옴 | 안 나옴 | 나옴 |
| `OFF` | 안 나옴 | 안 나옴 | 안 나옴 | 안 나옴 | 안 나옴 |

- **`TRACE`** 로 두면 이론상 **가장 많이** 나옵니다. Spring·Tomcat·JDBC 등 **라이브러리 로그까지 전부** 상세해져 콘솔이 매우 시끄럽습니다.
- **`DEBUG`** 는 애플리케이션에서 `log.debug` 까지는 열리지만 **`log.trace` 는 여전히 안 나옵니다.**
- **`INFO`** 는 운영에서 흔한 기본에 가깝고, **`log.info` / `warn` / `error` 만** 나오는 패턴입니다.

`logging.level.com.noa99kee.board=DEBUG` 처럼 **패키지만** 낮추면: 그 패키지 아래 클래스는 표의 **DEBUG 행과 비슷한 효과**, 나머지 패키지는 여전히 `root` 레벨을 따릅니다.

### 7.3 로거 이름과 계층 (패키지·클래스)

- 로거 이름은 보통 **자바 클래스 전체 이름**과 같습니다. 예: `com.noa99kee.board.post.service.PostService`.
- **부모–자식 관계**: `com.noa99kee.board` 는 `com.noa99kee.board.post.service.PostService` 의 **상위** 로거로 취급됩니다.
- Spring Boot / Logback 에서는 **가장 “구체적으로” 매칭된 설정**이 우선합니다.  
  예: `logging.level.com.noa99kee.board=DEBUG` 만 있으면 그 패키지 아래 전부 `DEBUG` 한도까지 열리고,  
  `logging.level.com.noa99kee.board.post.service.PostService=WARN` 을 추가하면 **그 클래스만** `WARN` 이상만 나갑니다.

### 7.4 루트 로거와 Spring Boot 기본값

- **`logging.level.root`** 가 전체 기본입니다. 따로 적지 않으면 Boot 가 보통 **`INFO`** 근처로 잡아, **`DEBUG`/`TRACE` 는 기본적으로 막힙니다.**
- 그래서 `PostService` 의 `log.debug("posts.list count=…")` 는 **별도로 `DEBUG` 를 열어 주지 않으면 안 보이는 것이 정상**입니다.

### 7.5 설정 방법 (`application*.properties`)

```properties
# 전체 기본 (선택)
logging.level.root=INFO

# 패키지 단위
logging.level.com.noa99kee.board=DEBUG

# 클래스 단위
logging.level.com.noa99kee.board.post.service.PostService=DEBUG
```

프로필별 파일(`application-local.properties` 등)이 활성화되면 **그쪽이 덮어씁니다.**  
이 저장소의 `application-local.properties` 에는 `logging.level.com.noa99kee.board=DEBUG` 가 있어, **`local` 프로필**일 때는 게시판 코드의 `DEBUG` 가 터미널에 보입니다.

### 7.6 환경 변수

프로퍼티 키를 대문자·언더스코어로 바꾼 형태입니다.

```text
LOGGING_LEVEL_ROOT=TRACE
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_COM_NOA99KEE_BOARD=DEBUG
```

`LOGGING_LEVEL_ROOT=TRACE` 는 **7.2절 표**에서 `root=TRACE` 행과 같이, 가능한 한 모든 레벨이 나가게 합니다(매우 시끄러움).

### 7.7 프레임워크·라이브러리 로그도 같은 규칙

`org.springframework`, `org.hibernate.SQL` 등 **패키지별로 레벨을 낮추면** SQL이나 웹 요청 로그가 쏟아지고, **높이면** 잡음이 줄어듭니다. 모두 위와 같은 **임계값 필터**입니다.

### 7.8 트러블슈팅과 연결

`log.debug` 만 안 보인다는 짧은 요약은 **8.7절**에도 두었습니다.

---

## 8. 실행·테스트 시 자주 겪는 혼란 (트러블슈팅)

### 8.1 `bootRun` 했는데 곧바로 꺼지고, 로그에 `Connection refused` 가 나올 때

- **직접적인 원인**: `localhost:5432` 에 PostgreSQL 이 없거나, 아직 기동 전이라 TCP 연결이 거절된 상태입니다.
- 로그에 `Unable to determine Dialect`, `entityManagerFactory` 실패, `Application run failed` 까지 이어지는 것은 **DB 연결 실패 → JPA(Hibernate) 기동 실패 → 나머지 빈 연쇄 실패**입니다. Dialect 설정을 먼저 의심할 필요는 없습니다.
- **조치**: PostgreSQL 을 켜고 DB·유저를 맞추거나, **2절**의 `docker compose … up db -d` 로 DB 만 띄운 뒤 다시 `bootRun` 합니다.

### 8.2 마지막에 `BUILD SUCCESSFUL` 인데 앱이 안 떠 있는 것 같을 때

- Gradle `bootRun` 은 JVM 이 **비정상 종료해도** 태스크 관점에서 성공으로 끝나는 경우가 있습니다.
- 판단 기준은 로그 안의 **`Application run failed`** 또는 **`Started BoardApplication`** 여부입니다. SUCCESS 한 줄만 보고 “잘 떴다”고 보지 마세요.

### 8.3 Docker 개발 Compose 에서 백엔드 로그만 보이고 프론트(Vite)가 안 붙을 때

- `docker-compose.dev.yml` 의 `frontend` 는 `backend` 가 **healthcheck 통과(`service_healthy`)** 한 뒤에 시작합니다.
- 백엔드가 DB·설정 문제로 기동에 실패하면 프론트 컨테이너는 대기하거나 함께 실패할 수 있습니다. **먼저 `spring-board-api-dev` 로그에서 백엔드 오류를 해결**해야 합니다.

### 8.4 `./gradlew test` 는 되는데 `bootRun` 만 안 될 때

- **서로 다른 설정을 씁니다.** 단위 테스트는 `src/test/resources/application.properties` 에서 **H2 인메모리 DB** 로 뜨도록 되어 있어, 로컬에 PostgreSQL 이 없어도 `test` 는 통과할 수 있습니다.
- 반면 **기본 `bootRun`** 은 PostgreSQL 을 바랍니다. “테스트는 되는데 실행만 안 된다”는 이 패턴이면 DB 미기동을 의심하면 됩니다.

### 8.5 Spring Boot 4 / Jackson 설정 (참고)

- 예전에 흔히 쓰던 `spring.jackson.serialization.write-dates-as-timestamps=false` 는 **Spring Boot 4(Jackson 3)** 에서 프로퍼티 바인딩이 깨져 **앱이 기동 단계에서 실패**할 수 있습니다. 이 프로젝트에서는 해당 줄을 쓰지 않습니다(날짜·시간은 기본적으로 ISO-8601 문자열에 가깝게 직렬화됩니다).

### 8.6 Git Bash 에서 `taskkill /PID …` 가 이상한 오류 날 때

- Git Bash 는 `/PID` 를 경로로 바꿔서 Windows `taskkill` 에 잘못 넘깁니다.
- 예: `taskkill.exe //PID 11888 //F` 또는 `cmd.exe /c "taskkill /PID 11888 /F"`  
  (PowerShell 이면 `Stop-Process -Id 11888 -Force`)

### 8.7 `log.debug(...)` 가 터미널에 안 나올 때

- **원인·필터 규칙·계층**은 **7절(로깅 레벨과 필터링)** 을 보면 됩니다. 한 줄로만 말하면: 기본 루트가 `INFO` 라서 **`DEBUG`/`TRACE` 는 버려진다**는 뜻입니다.
- **해결** (`application.properties` 또는 프로필 전용 파일, 예: `application-local.properties`):

```properties
# 특정 클래스만
logging.level.com.noa99kee.board.post.service.PostService=DEBUG

# 패키지 전체(게시판 앱)
logging.level.com.noa99kee.board=DEBUG
```

- **Docker / 환경 변수** 예: `LOGGING_LEVEL_COM_NOA99KEE_BOARD=DEBUG`
- **참고**: `application-local.properties` 에는 이미 `logging.level.com.noa99kee.board=DEBUG` 가 있어 **`local` 프로필**이면 `log.debug` 가 보입니다.

---

## 9. 테스트

H2 인메모리 DB와 `test` 프로필로 돌아갑니다. (**로컬 `bootRun` 과 DB 환경이 다릅니다** — 8.4 참고.) 실행 방법·프로필·특정 클래스만 돌리기·Spring Boot 4 테스트 패키지 정리는 **[backend/TESTING.md](backend/TESTING.md)** 에 모아 두었습니다.

빠른 실행만 할 때:

```powershell
Set-Location D:\Study\SpringBoot\spring-boot-app\backend
.\gradlew.bat test
```

---

## 10. 다음에 해볼 만한 것

- `ddl-auto` 를 `validate` 로 두고 **Flyway/Liquibase** 로 스키마 버전 관리
- 액세스 토큰 **RSA(RS256)** 또는 OAuth2 Resource Server
- 운영 로깅·메트릭(Micrometer, Actuator)
- E2E 테스트(Testcontainers + PostgreSQL)

이 문서는 프로젝트를 처음 열었을 때 **무엇이 어디에 있는지** 빠르게 잡기 위한 것입니다. 세부 구현은 각 패키지의 컨트롤러·서비스 코드를 따라가면 됩니다. 실행이 꼬이면 **8절(트러블슈팅)** 을 먼저 보세요. 로그가 왜 안 보이는지는 **7절(로깅 레벨과 필터링)** 을 보세요.
