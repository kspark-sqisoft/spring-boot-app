# 백엔드 테스트 가이드

Spring Boot 4 기준으로 **Gradle(JUnit 5)** 으로 테스트를 실행합니다. 기본 실행 시 **PostgreSQL이 필요 없습니다** — 테스트 전용 프로필에서 **H2 인메모리 DB**를 사용합니다.

---

## 1. 한 줄로 전체 실행

프로젝트 루트가 아니라 **`backend` 디렉터리**에서 실행합니다.

**Windows (PowerShell / CMD)**

```powershell
cd D:\Study\SpringBoot\spring-boot-app\backend
.\gradlew.bat test
```

**Linux / macOS**

```bash
cd backend
./gradlew test
```

성공 시 `BUILD SUCCESSFUL` 이 나오고, HTML 리포트는 `backend/build/reports/tests/test/index.html` 에 생성됩니다.

### 콘솔에 “뭐가 몇 개 통과했는지”가 잘 안 보일 때

- **Gradle 기본 동작**은 테스트 태스크가 조용한 편이라, 예전에는 `> Task :test` 한 줄과 `BUILD SUCCESSFUL` 만 보이는 경우가 많습니다.
- 이 프로젝트 `build.gradle` 에 **`testLogging`** 과 **마지막 요약 줄**(실행/성공/실패/스킵 개수)을 넣어 두었습니다. `.\gradlew.bat test` 를 **`-q`(quiet) 없이** 실행하면 각 테스트 `PASSED` / `FAILED` 와 요약이 터미널에 나옵니다.
- 더 붙이고 싶으면 예시처럼 로그 레벨을 올립니다.

```powershell
.\gradlew.bat test --info
```

- **가장 자세한 결과**는 HTML 리포트입니다. 브라우저로 `backend/build/reports/tests/test/index.html` 을 열면 클래스·메서드별 성공/실패와 스택을 볼 수 있습니다.

컴파일만 확인할 때:

```powershell
.\gradlew.bat compileTestJava
```

애플리케이션 코드와 테스트를 함께 확인할 때(README와 동일):

```powershell
.\gradlew.bat compileJava test
```

---

## 2. 용어: 통합 테스트(integration test)란?

말씀하신 **“퉁합” 테스트**는 보통 **통합 테스트**를 뜻합니다.

| 구분 | 의미 | 이 프로젝트에서 예시 |
|------|------|----------------------|
| **단위 테스트** | 클래스·메서드 하나만 격리해서 검증. DB·HTTP·다른 빈은 **목(Mock)** 으로 대체 | `PostServiceTest`, `AuthServiceTest`, `JwtServiceTest` |
| **통합 테스트** | **여러 계층을 실제에 가깝게 함께** 돌려 봄. 예: Spring 컨텍스트 + Security + JPA + 컨트롤러까지 연결 | `PostApiIntegrationTest`, `AuthApiIntegrationTest`, `HealthControllerIntegrationTest` |
| **슬라이스 테스트** | 통합보다 얇게, “JPA만” 같은 **일부 자동설정만** 로드 | `PostRepositoryTest` (`@DataJpaTest`) |

여기서 API 통합 테스트는 **실제 HTTP 서버를 띄우지 않고** `MockMvc` 로 요청을 보내는 방식입니다. 그래도 필터·시큐리티·DB(H2)까지 같이 타므로 **통합에 가깝다**고 부릅니다.

---

## 3. 테스트 프로필과 설정

- 활성 프로필: **`test`**
- 설정 파일: `src/test/resources/application-test.properties`

| 항목 | 테스트에서의 동작 |
|------|-------------------|
| DB | H2 인메모리 (`MODE=PostgreSQL` 에 가깝게 맞춤) |
| 스키마 | `spring.jpa.hibernate.ddl-auto=create-drop` (테스트 종료 시 드롭) |
| JWT | 고정 길이 테스트용 `app.jwt.access-secret` |
| 업로드 | OS 임시 디렉터리 아래 `board-test-uploads` |

로컬에서 `bootRun` 할 때 쓰는 `application.properties`(PostgreSQL)와 **완전히 분리**되어 있으므로, DB를 띄우지 않아도 테스트는 돌아갑니다.

---

## 4. 테스트 종류와 배치

| 종류 | 어노테이션 / 방식 | 디렉터리 예시 | 역할 |
|------|-------------------|---------------|------|
| 통합(API) | `AbstractIntegrationTest` 상속 → 전체 컨텍스트 + `MockMvc` + `@Transactional` 롤백 | `health/`, `auth/`, `post/PostApiIntegrationTest` | HTTP까지 포함한 API·시큐리티·JPA 연동 검증 |
| 단위(Mock) | `@ExtendWith(MockitoExtension.class)` | `auth/service/`, `post/service/` | 서비스 로직·외부 의존성 목 처리 |
| JPA 슬라이스 | `@DataJpaTest` | `post/repository/PostRepositoryTest` | 리포지토리·JPQL·영속성 컨텍스트(fetch) 검증 |
| 스모크 | `@SpringBootTest` + `contextLoads` | `BoardApplicationTests` | 애플리케이션 컨텍스트 기동 확인 |

공통 베이스 클래스: `com.noa99kee.board.support.AbstractIntegrationTest`

---

## 5. 특정 테스트만 실행

**클래스 단위**

```powershell
.\gradlew.bat test --tests "com.noa99kee.board.post.PostApiIntegrationTest"
```

**메서드 단위** (JUnit 5 `methodName` 또는 전체 FQCN)

```powershell
.\gradlew.bat test --tests "com.noa99kee.board.post.PostApiIntegrationTest.postCrudHappyPath"
```

**패키지 단위**

```powershell
.\gradlew.bat test --tests "com.noa99kee.board.auth.**"
```

---

## 6. Spring Boot 4에서 알아두면 좋은 점

이 프로젝트는 Spring Boot **4.x** 를 사용합니다. 테스트 코드에서 패키지가 Boot 3 이전과 다릅니다.

| 용도 | 패키지 |
|------|--------|
| `MockMvc` 자동설정 | `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` |
| JPA 테스트 슬라이스 | `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` |
| `TestEntityManager` | `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager` |
| JSON (`ObjectMapper`) | `tools.jackson.databind.ObjectMapper` (Jackson 3) |

새 통합 테스트를 추가할 때는 기존 `AbstractIntegrationTest` 를 상속하는 방식이 가장 단순합니다.

---

## 7. 실패 시 확인할 것

1. **리포트**  
   `build/reports/tests/test/index.html` 에서 스택 트레이스와 실패한 케이스 이름을 확인합니다.

2. **포트·프로세스**  
   통합 테스트는 임베디드 서블릿 컨테이너를 쓰지 않고 `MockMvc` 만 사용하므로, 일반적으로 **고정 포트 충돌은 없습니다**.

3. **테스트 전용 설정**  
   `application-test.properties` 를 바꾼 뒤에는 H2·JWT 길이(최소 32바이트 UTF-8)·경로가 깨지지 않았는지 확인합니다.

4. **데이터 잔존**  
   통합/리포지토리 테스트 클래스에 `@Transactional` 이 있으면 많은 경우 DB 변경이 롤백됩니다. 업로드 디렉터리에 남는 파일이 신경 쓰이면 임시 경로를 비우거나 테스트에서 별도 정리할 수 있습니다.

---

## 8. CI에서의 실행 예

저장소 루트가 아니라 **`backend` 를 작업 디렉터리로** 두고:

```bash
./gradlew test --no-daemon
```

JDK 17이 `JAVA_HOME` 에 잡혀 있어야 합니다. PostgreSQL·Docker는 **단위/통합 테스트 기본 경로에서는 불필요**합니다. 나중에 Testcontainers 등으로 실 DB에 가깝게 올리면 그때 CI에 Docker 서비스를 추가하면 됩니다.
