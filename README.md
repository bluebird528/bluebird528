# Web Editor File Upload Service

웹 에디터에서 전송되는 이미지 및 첨부파일을 저장하고 관리하는 Spring Boot 기반 백엔드 서비스입니다.

## 기술 스택

- **Spring Boot 3.4.0** - 최신 Spring Framework
- **Java 21** - LTS 버전
- **Gradle 8.10.2** - 빌드 도구
- **MariaDB 11.4+** - 데이터베이스
- **JPA/Hibernate** - ORM
- **Testcontainers** - 통합 테스트
- **JUnit 5** - 단위 테스트
- **Checkstyle** - 코드 스타일 검사
- **SpotBugs** - 정적 코드 분석
- **JaCoCo** - 코드 커버리지

## 주요 기능

### 파일 업로드
- 단일 파일 업로드
- 다중 파일 업로드 (배치)
- 파일 형식 검증 (화이트리스트 기반)
- 파일 크기 제한 (최대 50MB)
- Path Traversal 공격 방어
- 자동 파일 타입 분류 (이미지, 문서, 스프레드시트 등)

### 파일 저장
- 날짜 기반 디렉토리 구조 (YYYY/MM/DD)
- UUID 기반 파일명 생성 (충돌 방지)
- SHA-256 체크섬 계산
- 메타데이터 데이터베이스 저장

### 파일 조회
- 파일 메타데이터 조회
- 파일 다운로드
- 페이징 기능 (정렬, 필터링)
- 파일 타입별 조회

### 파일 삭제
- 소프트 삭제 (논리적 삭제)
- 물리적 파일 제거

## 프로젝트 구조

```
src/
├── main/
│   ├── java/com/bluebird/webeditor/
│   │   ├── WebEditorFileUploadApplication.java
│   │   ├── config/
│   │   │   ├── FileUploadProperties.java
│   │   │   └── FileStorageConfig.java
│   │   ├── controller/
│   │   │   └── FileUploadController.java
│   │   ├── dto/
│   │   │   ├── FileUploadResponse.java
│   │   │   ├── FileListResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── entity/
│   │   │   └── FileMetadata.java
│   │   ├── exception/
│   │   │   ├── FileStorageException.java
│   │   │   ├── FileNotFoundException.java
│   │   │   ├── InvalidFileException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── repository/
│   │   │   └── FileMetadataRepository.java
│   │   └── service/
│   │       └── FileStorageService.java
│   └── resources/
│       └── application.yml
└── test/
    ├── java/com/bluebird/webeditor/
    │   ├── config/
    │   │   └── TestContainerConfig.java
    │   ├── controller/
    │   │   └── FileUploadControllerTest.java
    │   └── service/
    │       └── FileStorageServiceTest.java
    └── resources/
        └── application-test.yml
```

## 설치 및 실행

### 사전 요구사항

- Java 21 이상
- Docker (Testcontainers 사용 시)
- MariaDB 11.4+ (로컬 실행 시)

### 데이터베이스 설정

```sql
CREATE DATABASE webeditor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'webeditor'@'localhost' IDENTIFIED BY 'webeditor123';
GRANT ALL PRIVILEGES ON webeditor.* TO 'webeditor'@'localhost';
FLUSH PRIVILEGES;
```

### 애플리케이션 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 또는 JAR 파일로 실행
java -jar build/libs/web-editor-file-upload-0.0.1-SNAPSHOT.jar
```

애플리케이션은 기본적으로 `http://localhost:8080`에서 실행됩니다.

## API 엔드포인트

### 파일 업로드

**단일 파일 업로드**
```http
POST /api/files/upload
Content-Type: multipart/form-data

file: (binary)
```

**응답 예시**
```json
{
  "id": 1,
  "originalFilename": "image.jpg",
  "storedFilename": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "fileSize": 102400,
  "mimeType": "image/jpeg",
  "fileType": "IMAGE",
  "fileExtension": "jpg",
  "downloadUrl": "http://localhost:8080/api/files/1/download",
  "createdAt": "2025-11-05T10:30:00"
}
```

**다중 파일 업로드**
```http
POST /api/files/upload/multiple
Content-Type: multipart/form-data

files: (binary)
files: (binary)
```

### 파일 조회

**파일 정보 조회**
```http
GET /api/files/{id}
```

**파일 다운로드**
```http
GET /api/files/{id}/download
```

**전체 파일 목록 조회 (페이징)**
```http
GET /api/files?page=0&size=10&sortBy=createdAt&sortDir=DESC
```

**파일 타입별 조회**
```http
GET /api/files/type/IMAGE?page=0&size=10
```

파일 타입: `IMAGE`, `DOCUMENT`, `SPREADSHEET`, `PRESENTATION`, `ARCHIVE`, `TEXT`, `OTHER`

### 파일 삭제

```http
DELETE /api/files/{id}
```

## 테스트

### 전체 테스트 실행

```bash
./gradlew test
```

### 테스트 커버리지 확인

```bash
./gradlew test jacocoTestReport
```

커버리지 리포트: `build/reports/jacoco/test/html/index.html`

### 코드 품질 검사

**Checkstyle 실행**
```bash
./gradlew checkstyleMain checkstyleTest
```

**SpotBugs 실행**
```bash
./gradlew spotbugsMain spotbugsTest
```

리포트:
- Checkstyle: `build/reports/checkstyle/`
- SpotBugs: `build/reports/spotbugs/`

## 설정

### application.yml 주요 설정

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB          # 최대 파일 크기
      max-request-size: 100MB      # 최대 요청 크기

  datasource:
    url: jdbc:mariadb://localhost:3306/webeditor
    username: webeditor
    password: webeditor123

file:
  upload:
    dir: ./uploads                 # 업로드 디렉토리
    max-size: 52428800            # 50MB (바이트)
    allowed-extensions: jpg,jpeg,png,gif,bmp,webp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,zip
```

## 보안 고려사항

1. **파일 타입 검증**: 화이트리스트 기반 확장자 필터링
2. **Path Traversal 방어**: 파일명에서 ".." 패턴 차단
3. **파일 크기 제한**: 최대 50MB 제한
4. **파일명 난독화**: UUID 기반 저장 파일명 생성
5. **체크섬 검증**: SHA-256 해시로 파일 무결성 검증

## 성능 최적화

1. **날짜 기반 디렉토리 구조**: 파일 시스템 성능 향상
2. **데이터베이스 인덱스**: 자주 조회되는 컬럼에 인덱스 적용
3. **소프트 삭제**: 빠른 삭제 처리
4. **페이징**: 대용량 데이터 효율적 처리

## 개발 가이드

### 코드 스타일

프로젝트는 Checkstyle을 사용하여 일관된 코드 스타일을 유지합니다.
- 최대 라인 길이: 120자
- 들여쓰기: 공백 4칸
- 네이밍 컨벤션: Java 표준 준수

### 커밋 전 체크리스트

```bash
# 빌드 및 모든 테스트 실행
./gradlew clean build

# 코드 스타일 검사
./gradlew checkstyleMain checkstyleTest

# 정적 분석
./gradlew spotbugsMain

# 테스트 커버리지 확인
./gradlew jacocoTestCoverageVerification
```

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 문의

문제가 발생하거나 기능 요청이 있으시면 이슈를 등록해주세요.
