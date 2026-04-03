# Spring Boot 게시판 + React (Docker) 학습 노트

이 저장소는 `d:\Study\Docker\docker_app` 과 **동일한 HTTP API 계약**(경로·JSON 필드명·인증 방식)을 유지하면서, 백엔드만 **Java 17 + Spring Boot 4** 로 구현한 예제입니다. 프론트엔드는 React 19, Tailwind CSS 4, shadcn/ui, TanStack Query 등 기존 스택을 그대로 사용합니다.

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

- `config` — `SecurityConfig`, `WebConfig`(CORS, `/uploads` 리소스 핸들러), JWT/업로드 설정 바인딩
- `domain.user` / `domain.post` — JPA 엔티티·리포지토리
- `auth` — JWT 발급/검증, 회원가입·로그인·리프레시·쿠키
- `security` — `JwtAuthenticationFilter`, `BoardUserPrincipal`
- `posts` — 게시글 CRUD·이미지 업로드 URL 검증(`/uploads/posts/` 만 허용)
- `user` — 안전한 사용자 응답 DTO 변환
- `common` — 전역 예외 처리(`message`, `statusCode`, `path`, `timestamp`)

보안 규칙 요약:

- 공개: `GET /api/health`, `GET /api/posts`, `GET /api/posts/{id}`, `POST /api/auth/register|login|refresh`, `/uploads/**`
- 인증 필요: 글 작성·수정·삭제, 이미지 업로드, 프로필, 로그아웃 등

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

## 7. 실행·테스트 시 자주 겪는 혼란 (트러블슈팅)

### 7.1 `bootRun` 했는데 곧바로 꺼지고, 로그에 `Connection refused` 가 나올 때

- **직접적인 원인**: `localhost:5432` 에 PostgreSQL 이 없거나, 아직 기동 전이라 TCP 연결이 거절된 상태입니다.
- 로그에 `Unable to determine Dialect`, `entityManagerFactory` 실패, `Application run failed` 까지 이어지는 것은 **DB 연결 실패 → JPA(Hibernate) 기동 실패 → 나머지 빈 연쇄 실패**입니다. Dialect 설정을 먼저 의심할 필요는 없습니다.
- **조치**: PostgreSQL 을 켜고 DB·유저를 맞추거나, **2절**의 `docker compose … up db -d` 로 DB 만 띄운 뒤 다시 `bootRun` 합니다.

### 7.2 마지막에 `BUILD SUCCESSFUL` 인데 앱이 안 떠 있는 것 같을 때

- Gradle `bootRun` 은 JVM 이 **비정상 종료해도** 태스크 관점에서 성공으로 끝나는 경우가 있습니다.
- 판단 기준은 로그 안의 **`Application run failed`** 또는 **`Started BoardApplication`** 여부입니다. SUCCESS 한 줄만 보고 “잘 떴다”고 보지 마세요.

### 7.3 Docker 개발 Compose 에서 백엔드 로그만 보이고 프론트(Vite)가 안 붙을 때

- `docker-compose.dev.yml` 의 `frontend` 는 `backend` 가 **healthcheck 통과(`service_healthy`)** 한 뒤에 시작합니다.
- 백엔드가 DB·설정 문제로 기동에 실패하면 프론트 컨테이너는 대기하거나 함께 실패할 수 있습니다. **먼저 `spring-board-api-dev` 로그에서 백엔드 오류를 해결**해야 합니다.

### 7.4 `./gradlew test` 는 되는데 `bootRun` 만 안 될 때

- **서로 다른 설정을 씁니다.** 단위 테스트는 `src/test/resources/application.properties` 에서 **H2 인메모리 DB** 로 뜨도록 되어 있어, 로컬에 PostgreSQL 이 없어도 `test` 는 통과할 수 있습니다.
- 반면 **기본 `bootRun`** 은 PostgreSQL 을 바랍니다. “테스트는 되는데 실행만 안 된다”는 이 패턴이면 DB 미기동을 의심하면 됩니다.

### 7.5 Spring Boot 4 / Jackson 설정 (참고)

- 예전에 흔히 쓰던 `spring.jackson.serialization.write-dates-as-timestamps=false` 는 **Spring Boot 4(Jackson 3)** 에서 프로퍼티 바인딩이 깨져 **앱이 기동 단계에서 실패**할 수 있습니다. 이 프로젝트에서는 해당 줄을 쓰지 않습니다(날짜·시간은 기본적으로 ISO-8601 문자열에 가깝게 직렬화됩니다).

### 7.6 Git Bash 에서 `taskkill /PID …` 가 이상한 오류 날 때

- Git Bash 는 `/PID` 를 경로로 바꿔서 Windows `taskkill` 에 잘못 넘깁니다.
- 예: `taskkill.exe //PID 11888 //F` 또는 `cmd.exe /c "taskkill /PID 11888 /F"`  
  (PowerShell 이면 `Stop-Process -Id 11888 -Force`)

---

## 8. 테스트

H2 인메모리 DB로 컨텍스트를 띄웁니다. (**로컬 `bootRun` 과 DB 환경이 다릅니다** — 7.4 참고.)

```powershell
Set-Location D:\Study\SpringBoot\spring-boot-app\backend
.\gradlew.bat test
```

---

## 9. 다음에 해볼 만한 것

- `ddl-auto` 를 `validate` 로 두고 **Flyway/Liquibase** 로 스키마 버전 관리
- 액세스 토큰 **RSA(RS256)** 또는 OAuth2 Resource Server
- 운영 로깅·메트릭(Micrometer, Actuator)
- E2E 테스트(Testcontainers + PostgreSQL)

이 문서는 프로젝트를 처음 열었을 때 **무엇이 어디에 있는지** 빠르게 잡기 위한 것입니다. 세부 구현은 각 패키지의 컨트롤러·서비스 코드를 따라가면 됩니다. 실행이 꼬이면 **7절(트러블슈팅)** 을 먼저 보세요.
