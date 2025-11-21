# Web Editor File Upload Service - Claude Development Guide

## 프로젝트 개요

웹 에디터에서 전송되는 이미지 및 첨부파일을 저장하고 관리하는 Spring Boot 기반 백엔드 서비스입니다.

**핵심 기능:**
- 단일/다중 파일 업로드 및 다운로드
- 파일 메타데이터 관리 (데이터베이스)
- 파일 타입별 분류 및 조회
- 소프트 삭제 지원
- 보안 강화 (Path Traversal 방어, 화이트리스트 기반 검증)

## 기술 스택

- **Spring Boot**: 3.4.0
- **Java**: 21 (LTS)
- **Build Tool**: Gradle 8.10.2
- **Database**: MariaDB 11.4+
- **ORM**: JPA/Hibernate
- **Testing**: JUnit 5, Testcontainers
- **Code Quality**: Checkstyle, SpotBugs, JaCoCo

## 프로젝트 구조

```
src/
├── main/java/com/bluebird/webeditor/
│   ├── WebEditorFileUploadApplication.java    # 메인 애플리케이션
│   ├── config/                                 # 설정 클래스
│   │   ├── FileUploadProperties.java          # 파일 업로드 프로퍼티
│   │   └── FileStorageConfig.java             # 스토리지 설정
│   ├── controller/
│   │   └── FileUploadController.java          # REST API 컨트롤러
│   ├── dto/                                    # 데이터 전송 객체
│   │   ├── FileUploadResponse.java
│   │   ├── FileListResponse.java
│   │   └── ErrorResponse.java
│   ├── entity/
│   │   └── FileMetadata.java                  # JPA 엔티티
│   ├── exception/                              # 예외 클래스
│   │   ├── GlobalExceptionHandler.java        # 전역 예외 핸들러
│   │   ├── FileStorageException.java
│   │   ├── FileNotFoundException.java
│   │   └── InvalidFileException.java
│   ├── repository/
│   │   └── FileMetadataRepository.java        # JPA Repository
│   └── service/
│       └── FileStorageService.java            # 비즈니스 로직
└── test/                                       # 테스트 코드
    ├── config/TestContainerConfig.java        # Testcontainers 설정
    ├── controller/FileUploadControllerTest.java
    └── service/FileStorageServiceTest.java
```

## 코딩 표준 및 규칙

### Java 코드 스타일

1. **Checkstyle 규칙 준수**
   - 최대 라인 길이: 120자
   - 들여쓰기: 공백 4칸
   - 네이밍 컨벤션: Java 표준 (camelCase, PascalCase)

2. **Lombok 사용**
   - `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor` 적극 활용
   - `@Data`는 주의해서 사용 (불필요한 메소드 생성 가능)

3. **예외 처리**
   - 비즈니스 로직 예외는 커스텀 예외 사용
   - `GlobalExceptionHandler`를 통한 중앙 집중식 예외 처리
   - 사용자 친화적인 에러 메시지 제공

4. **보안 고려사항**
   - 파일 업로드 시 화이트리스트 기반 검증 필수
   - Path Traversal 공격 방어 (.., / 등)
   - 파일명 UUID로 난독화
   - SQL Injection 방지 (JPA 사용)
   - XSS 방지 (입력값 검증)

### 데이터베이스 설계

- **엔티티 네이밍**: 단수형 사용 (FileMetadata)
- **컬럼 네이밍**: snake_case (created_at)
- **인덱스**: 자주 조회되는 컬럼에 인덱스 추가
- **소프트 삭제**: deleted 플래그 사용

### REST API 설계

- **URL 구조**: `/api/{resource}/{id}/{action}`
- **HTTP 메소드**:
  - POST: 생성
  - GET: 조회
  - PUT/PATCH: 수정
  - DELETE: 삭제
- **응답 코드**:
  - 200: 성공
  - 201: 생성 성공
  - 204: 삭제 성공 (본문 없음)
  - 400: 잘못된 요청
  - 404: 리소스 없음
  - 500: 서버 오류

## 개발 워크플로우

### 1. 새 기능 개발

1. **요구사항 분석**: API.md, README.md 참고
2. **설계**: 레이어별 역할 명확히 구분
   - Controller: 요청/응답 처리
   - Service: 비즈니스 로직
   - Repository: 데이터 접근
3. **구현**: 테스트 주도 개발 (TDD) 권장
4. **테스트**: 단위 테스트 + 통합 테스트 작성
5. **코드 품질 검사**: Checkstyle, SpotBugs 통과
6. **커버리지 확인**: JaCoCo (최소 70%)

### 2. 버그 수정

1. **재현**: 테스트 케이스로 버그 재현
2. **분석**: 로그, 디버거 활용
3. **수정**: 최소한의 변경으로 수정
4. **검증**: 테스트 통과 확인
5. **회귀 테스트**: 기존 기능 영향 없는지 확인

### 3. 리팩토링

- 테스트 먼저 작성/확인
- 작은 단위로 점진적 개선
- 코드 리뷰 필수

## 테스트 전략

### 단위 테스트 (Unit Test)

- **Service Layer**: 비즈니스 로직 검증
- **Mocking**: Mockito 사용
- **커버리지**: 최소 70% (JaCoCo)

### 통합 테스트 (Integration Test)

- **Controller Layer**: API 엔드포인트 테스트
- **Database**: Testcontainers로 실제 MariaDB 사용
- **MockMvc**: HTTP 요청/응답 검증

### 테스트 작성 원칙

1. **AAA 패턴**: Arrange, Act, Assert
2. **독립성**: 각 테스트는 독립적으로 실행 가능
3. **반복성**: 항상 동일한 결과
4. **명확한 네이밍**: `메소드명_상황_예상결과`

```java
@Test
void uploadFile_validFile_returnsFileUploadResponse() {
    // Arrange: 테스트 데이터 준비
    // Act: 메소드 실행
    // Assert: 결과 검증
}
```

## 빌드 및 실행

### 로컬 개발 환경

```bash
# 빌드 (테스트 포함)
./gradlew clean build

# 빌드 (테스트 제외)
./gradlew clean build -x test

# 애플리케이션 실행
./gradlew bootRun

# 특정 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests FileStorageServiceTest

# 특정 테스트 메소드
./gradlew test --tests FileStorageServiceTest.uploadFile_validFile_returnsFileUploadResponse

# 커버리지 리포트 생성
./gradlew test jacocoTestReport
# 리포트 위치: build/reports/jacoco/test/html/index.html

# 커버리지 검증
./gradlew jacocoTestCoverageVerification
```

### 코드 품질 검사

```bash
# Checkstyle (코드 스타일)
./gradlew checkstyleMain checkstyleTest
# 리포트: build/reports/checkstyle/

# SpotBugs (정적 분석)
./gradlew spotbugsMain spotbugsTest
# 리포트: build/reports/spotbugs/

# 전체 품질 검사
./gradlew check
```

## 일반적인 작업

### 새 엔티티 추가

1. `entity/` 패키지에 엔티티 클래스 생성
2. `repository/` 패키지에 Repository 인터페이스 생성
3. `service/` 패키지에 Service 클래스 생성
4. `controller/` 패키지에 Controller 추가
5. `dto/` 패키지에 DTO 추가
6. 테스트 작성

### 새 API 엔드포인트 추가

1. Controller에 메소드 추가
2. Service 로직 구현
3. DTO 정의
4. 통합 테스트 작성
5. API.md 문서 업데이트

### 데이터베이스 마이그레이션

- JPA는 자동으로 스키마 생성/업데이트
- 프로덕션 환경: `spring.jpa.hibernate.ddl-auto=validate`
- 개발 환경: `spring.jpa.hibernate.ddl-auto=update`

## 문제 해결 가이드

### 파일 업로드 실패

1. 파일 크기 확인 (최대 50MB)
2. 파일 확장자 확인 (화이트리스트)
3. 업로드 디렉토리 권한 확인
4. 디스크 용량 확인

### 테스트 실패

1. Testcontainers가 Docker 접근 가능한지 확인
2. 포트 충돌 확인
3. 테스트 격리 확인 (상태 공유 방지)

### 빌드 실패

1. Java 버전 확인 (Java 21 필요)
2. Gradle 캐시 정리: `./gradlew clean --refresh-dependencies`
3. IDE 프로젝트 재동기화

## 참고 자료

- **프로젝트 문서**
  - [README.md](../README.md): 프로젝트 개요 및 설치 가이드
  - [API.md](../API.md): API 명세 및 사용 예시

- **설정 파일**
  - [build.gradle](../build.gradle): Gradle 빌드 설정
  - [application.yml](../src/main/resources/application.yml): Spring Boot 설정
  - [checkstyle.xml](../config/checkstyle/checkstyle.xml): Checkstyle 규칙

- **외부 문서**
  - [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
  - [Spring Data JPA 가이드](https://spring.io/guides/gs/accessing-data-jpa/)
  - [Testcontainers 문서](https://www.testcontainers.org/)

## 커밋 전 체크리스트

코드를 커밋하기 전에 다음 사항을 확인하세요:

- [ ] 코드가 빌드되는가? (`./gradlew build`)
- [ ] 모든 테스트가 통과하는가? (`./gradlew test`)
- [ ] Checkstyle을 통과하는가? (`./gradlew checkstyleMain`)
- [ ] SpotBugs 경고가 없는가? (`./gradlew spotbugsMain`)
- [ ] 테스트 커버리지가 70% 이상인가? (`./gradlew jacocoTestCoverageVerification`)
- [ ] API 문서가 업데이트되었는가? (API 변경 시)
- [ ] 보안 취약점이 없는가?

## 유용한 명령어

```bash
# 프로젝트 클린
./gradlew clean

# 의존성 확인
./gradlew dependencies

# 사용 가능한 태스크 목록
./gradlew tasks

# 빌드 캐시 새로고침
./gradlew build --refresh-dependencies

# 특정 프로파일로 실행 (dev, prod)
./gradlew bootRun --args='--spring.profiles.active=dev'

# JAR 파일 생성 위치
# build/libs/web-editor-file-upload-0.0.1-SNAPSHOT.jar
```

## 연락처 및 지원

문제가 발생하거나 질문이 있으면 이슈를 등록해주세요.
