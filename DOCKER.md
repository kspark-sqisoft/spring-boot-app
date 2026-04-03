# Docker 실행 가이드

이 저장소에는 **두 가지 Compose 파일**이 있습니다. 목적이 다르므로, 쓰려는 상황에 맞게 고르면 됩니다.

| 파일 | 프로젝트 이름(기본) | 용도 |
|------|----------------------|------|
| `docker-compose.yml` | 디렉터리 이름 기반 | **운영에 가깝게**: JAR 백엔드 + nginx 정적 프론트, 핫리로드 없음 |
| `docker-compose.dev.yml` | `spring-board-dev` (파일 안 `name:`) | **개발**: 소스 볼륨 마운트 + Gradle `bootRun` + Vite |

---

## 1. 포트 한눈에

로컬에서 브라우저·클라이언트가 쓰는 주소입니다.

| 서비스 | 운영 Compose (`docker-compose.yml`) | 개발 Compose (`docker-compose.dev.yml`) |
|--------|-------------------------------------|----------------------------------------|
| PostgreSQL | 호스트 `5432` | 호스트 `5432` |
| Spring API | `http://localhost:3000` | `http://localhost:3000` |
| 웹 UI | `http://localhost:8080` (nginx → 80) | `http://localhost:5173` 또는 `http://localhost:8080` (둘 다 Vite) |

**주의:** 두 스택을 **동시에** 띄우면 **5432**(그리고 필요 시 **8080**)가 겹칩니다. 한쪽은 `down` 하거나, Compose에서 포트 매핑을 바꿔야 합니다.

---

## 2. 실행 시나리오별 명령

프로젝트 루트(`spring-boot-app`)에서 실행한다고 가정합니다.

### 2.1 운영 스타일 — 전체 스택 (DB + API + 웹)

이미지 빌드까지 포함해 처음부터 올릴 때:

```powershell
docker compose up --build
```

이미 빌드된 이미지로만 올릴 때:

```powershell
docker compose up
```

- 접속: 웹 **`http://localhost:8080`**, API 직접 호출 시 **`http://localhost:3000`**

### 2.2 개발 스타일 — 전체 스택 (핫리로드)

```powershell
docker compose -f docker-compose.dev.yml up --build
```

또는 빌드 생략:

```powershell
docker compose -f docker-compose.dev.yml up
```

- 접속: **`http://localhost:5173`** 또는 **`http://localhost:8080`** (같은 Vite)
- 백엔드가 **healthcheck**에 통과해야 프론트 컨테이너가 시작합니다. 첫 기동은 Gradle 때문에 **수 분** 걸릴 수 있습니다.

### 2.3 백그라운드(데몬)으로만 올리기

운영:

```powershell
docker compose up -d --build
```

개발:

```powershell
docker compose -f docker-compose.dev.yml up -d --build
```

로그 보기:

```powershell
docker compose logs -f
# 또는
docker compose -f docker-compose.dev.yml logs -f
```

특정 서비스만:

```powershell
docker compose -f docker-compose.dev.yml logs -f backend
```

### 2.4 DB만 올리기 (호스트에서 `bootRun` / `npm run dev` 할 때)

개발 Compose의 DB는 계정이 앱 기본값(`board` / `board` / DB `board`)과 맞춰져 있습니다.

```powershell
docker compose -f docker-compose.dev.yml up db -d
```

운영 Compose의 DB만 쓰려면:

```powershell
docker compose up db -d
```

이후 호스트에서:

- `backend`: `.\gradlew.bat bootRun` (같은 DB 접속 정보)
- `frontend`: `npm run dev` (`VITE_API_PROXY_TARGET` 기본 `http://localhost:3000`)

### 2.5 특정 서비스만 재빌드·재기동

예: 개발 백엔드 Dockerfile만 바꾼 뒤:

```powershell
docker compose -f docker-compose.dev.yml up -d --build backend
```

### 2.6 중지·정리

컨테이너만 내리기 (운영 스택):

```powershell
docker compose down
```

개발 스택:

```powershell
docker compose -f docker-compose.dev.yml down
```

**볼륨(DB·업로드·캐시)까지 지우고 처음부터** (데이터 삭제됨):

```powershell
docker compose down -v
docker compose -f docker-compose.dev.yml down -v
```

---

## 3. 두 Compose가 나뉘는 이유 (요약)

| 항목 | 운영 `docker-compose.yml` | 개발 `docker-compose.dev.yml` |
|------|---------------------------|-------------------------------|
| 백엔드 이미지 | `backend/Dockerfile` → JAR 실행 | `backend/Dockerfile.dev` → `bootRun` + 소스 마운트 |
| 프론트 이미지 | `frontend/Dockerfile` → 빌드 후 nginx | `frontend/Dockerfile.dev` → Vite + 소스 마운트 |
| 볼륨 | DB, 업로드 | DB, 업로드, **gradle_cache**, **frontend_node_modules** |
| CORS 기본값 | `localhost:8080` 위주 | Vite·8080 병행 |
| Compose `name` | 없음(폴더명) | `spring-board-dev` → **프로젝트/볼륨 네임스페이스 분리** |

같은 머신에서 운영 스택과 개발 스택을 **번갈아** 쓸 때는, 한쪽은 반드시 `down` 해 두는 것이 안전합니다.

---

## 4. 환경 변수 · `.env`

루트에 `.env` 를 두면 Compose가 읽습니다. 예시는 `.env.example` 참고.

자주 쓰는 변수:

- `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME` — Postgres
- `JWT_ACCESS_SECRET` — 운영·공유 환경에서는 반드시 강한 값으로 교체
- `CORS_ORIGIN` — 쉼표로 구분한 Origin 목록 (브라우저에서 쿠키·자격증명 쓸 때 필요)

한 번만 터미널에서 넘기기:

```powershell
$env:CORS_ORIGIN="http://localhost:3000"; docker compose up
```

---

## 5. 컨테이너 이름 (디버깅용)

| 운영 Compose | 개발 Compose |
|----------------|--------------|
| `spring-board-db` | `spring-board-db-dev` |
| `spring-board-api` | `spring-board-api-dev` |
| `spring-board-web` | `spring-board-web-dev` |

예:

```powershell
docker logs spring-board-api-dev --tail 100
```

---

## 6. 자주 겪는 이슈

1. **프론트 로그가 안 보인다**  
   개발 Compose에서는 `frontend`가 `backend` **healthy** 이후에 시작합니다. 백엔드 로그에서 DB 연결·설정 오류를 먼저 해결하세요. (`STUDY.md` 7절 참고)

2. **포트 이미 사용 중**  
   다른 Postgres(5432)나 다른 앱이 3000/8080/5173을 쓰면 실패합니다. 해당 프로세스를 끄거나 `docker-compose*.yml`의 `ports:` 를 수정하세요.

3. **Windows에서 `gradlew` 실행 권한**  
   개발 백엔드 컨테이너는 호스트의 `./backend`를 마운트합니다. Git이 `gradlew` 실행 비트를 빼먹은 경우 컨테이너 안에서 `./gradlew`가 실패할 수 있습니다. 그때는 이미지 빌드 단계의 `chmod`와 호스트 파일 권한을 확인하세요.

4. **이미지 캐시만 쓰고 싶다**  
   `up --build` 는 Dockerfile/컨텍스트 변경 시 재빌드합니다. 강제로 깨끗이 하려면 `docker compose build --no-cache` 후 `up` 합니다.

---

## 7. 관련 문서

- 전체 학습 노트·트러블슈팅: [STUDY.md](./STUDY.md)
- 환경 변수 예시: [.env.example](./.env.example)
