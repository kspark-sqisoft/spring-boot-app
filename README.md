# Spring Boot 게시판 + React

Java **Spring Boot 4** 백엔드와 **React 19** 프론트엔드로 만든 게시판 예제입니다. PostgreSQL·JWT·httpOnly 리프레시 쿠키·이미지 업로드를 포함하며, **Docker Compose**로 한 번에 띄우거나 백엔드/프론트만 로컬에서 실행할 수 있습니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| 백엔드 | Java 17, Spring Boot 4, Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation |
| DB | PostgreSQL 16 (로컬 또는 Compose) |
| 인증 | JWT(Bearer) + 리프레시 토큰(SHA-256 해시 저장, httpOnly 쿠키) |
| 프론트엔드 | React 19, Vite 8, TypeScript, TanStack Query, Zustand, React Router 7, Tailwind CSS 4, shadcn/ui |
| 컨테이너 | Docker, Compose(운영형 / 개발형 두 가지) |

---

## 저장소 구조

백엔드는 **기능(feature) 단위 패키지** 아래에 `controller` / `service` / `repository` / `entity` / `dto` 등을 두는 형태입니다. 프론트는 `features/*` 아래에 인증·게시글 UI를 모읍니다.

```
spring-boot-app/
├── backend/                              # Spring Boot API (Gradle)
│   ├── src/main/java/com/noa99kee/board/
│   │   ├── BoardApplication.java
│   │   ├── auth/                         # 회원가입·로그인·리프레시·JWT
│   │   │   ├── controller/             # /api/auth/register|login|logout|refresh
│   │   │   ├── service/                  # AuthService, JwtService
│   │   │   ├── dto/
│   │   │   ├── filter/                 # JwtAuthenticationFilter
│   │   │   ├── principal/
│   │   │   └── util/
│   │   ├── user/                         # 현재 사용자 프로필 (URL은 /api/auth/me 유지)
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   └── dto/
│   │   ├── post/                         # 게시글 CRUD·이미지 업로드 API
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   └── dto/
│   │   ├── health/
│   │   │   └── controller/             # /api/health
│   │   ├── common/                     # GlobalExceptionHandler, ImageUploadValidation
│   │   └── config/                     # Security, CORS, JWT, 업로드 경로
│   ├── src/test/java/com/noa99kee/board/
│   │   ├── support/                    # 통합 테스트 베이스 (MockMvc)
│   │   ├── auth/                       # AuthApiIntegrationTest, service 단위 테스트
│   │   ├── post/                       # PostApiIntegrationTest, service·repository 테스트
│   │   ├── health/
│   │   └── BoardApplicationTests.java
│   ├── src/main/resources/             # application.properties 등
│   ├── src/test/resources/             # application-test.properties (H2)
│   └── TESTING.md
├── frontend/                             # Vite + React SPA
│   └── src/
│       ├── features/
│       │   └── auth/                   # 로그인·회원가입 폼, RequireAuth, auth-store
│       ├── pages/                      # Home, Login, Register, Profile, NotFound
│       ├── app/                        # router, RootLayout, SiteHeaderBar
│       ├── api/                        # auth.ts (백엔드 호출)
│       ├── components/ui/              # shadcn/ui 공통 컴포넌트
│       └── lib/                        # api-fetch, query-client, utils
├── docker-compose.yml                    # 운영에 가까운 스택 (JAR + nginx)
├── docker-compose.dev.yml                # 개발용 (bootRun + Vite, 소스 마운트)
├── .env.example
├── DOCKER.md
├── STUDY.md
└── README.md
```

---

## 빠른 시작

### 1) Docker로 개발 스택 전체 (권장: 핫 리로드)

프로젝트 루트에서:

```powershell
docker compose -f docker-compose.dev.yml up --build
```

- API: `http://localhost:3000`
- 웹: `http://localhost:5173` 또는 `http://localhost:8080` (동일 Vite)
- DB: 호스트 `5432`

첫 기동 시 Gradle 의존성 때문에 백엔드 healthcheck 통과까지 **수 분** 걸릴 수 있습니다.

### 2) Docker로 운영에 가까운 스택

```powershell
docker compose up --build
```

- 웹(nginx): `http://localhost:8080`
- API: `http://localhost:3000`

### 3) 호스트에서만 백엔드 실행

PostgreSQL이 먼저 실행 중이어야 합니다. DB만 Compose로 띄우려면:

```powershell
docker compose -f docker-compose.dev.yml up db -d
```

이후:

```powershell
cd backend
.\gradlew.bat bootRun
```

### 4) 호스트에서만 프론트 실행

```powershell
cd frontend
npm install
npm run dev
```

API 프록시 대상은 `frontend`의 Vite 설정(`VITE_API_PROXY_TARGET` 등)을 `STUDY.md`와 맞춥니다.

---

## 환경 변수

루트의 **[.env.example](.env.example)** 를 복사해 `.env`를 만들고, Docker 또는 로컬 실행에 맞게 수정합니다. (DB URL, JWT 시크릿, CORS, 업로드 디렉터리 등)

---

## API 개요

| 영역 | 설명 |
|------|------|
| `GET /api/health` | 헬스체크 |
| `/api/auth/*` | 회원가입, 로그인, 로그아웃, 리프레시, 프로필(`me`), 아바타 업로드 |
| `/api/posts` | 게시글 목록·상세(공개), 작성·수정·삭제·이미지 업로드(JWT 필요) |
| `/uploads/**` | 저장된 프로필·게시글 이미지 정적 서빙 |

프론트 SPA는 **인증·프로필 UI만** 포함합니다. 게시판 API는 백엔드에 그대로 있으며, Postman 등으로 호출하거나 나중에 UI를 다시 붙이면 됩니다.

자세한 인증 흐름·포트 정리·트러블슈팅은 아래 문서를 참고하세요.

---

## 문서

| 문서 | 내용 |
|------|------|
| [STUDY.md](STUDY.md) | 아키텍처, 로컬/`bootRun`, 로깅 레벨·필터링, API·프록시, 트러블슈팅 |
| [DOCKER.md](DOCKER.md) | 두 Compose 파일 차이, 포트 표, 로그·중지 명령 |
| [backend/TESTING.md](backend/TESTING.md) | 백엔드 테스트 실행, `test` 프로필, 클래스/패키지 단위 실행 |
| [frontend/README.md](frontend/README.md) | 프론트 스크립트·스택 요약 |

---

## 빌드 확인

```powershell
cd backend
.\gradlew.bat compileJava test

cd ..\frontend
npm run build
```

백엔드 테스트만 자세히 보려면 [backend/TESTING.md](backend/TESTING.md) 를 참고하세요.

---

## 라이선스

학습·포트폴리오 용도 예제입니다. 필요 시 저장소 소유자 기준으로 라이선스를 명시하세요.
