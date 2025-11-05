# API 문서

## 파일 업로드 API

### 1. 단일 파일 업로드

파일 하나를 업로드합니다.

**요청**
```http
POST /api/files/upload
Content-Type: multipart/form-data

file: [파일]
```

**응답 (200 OK)**
```json
{
  "id": 1,
  "originalFilename": "example.jpg",
  "storedFilename": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "fileSize": 102400,
  "mimeType": "image/jpeg",
  "fileType": "IMAGE",
  "fileExtension": "jpg",
  "downloadUrl": "http://localhost:8080/api/files/1/download",
  "createdAt": "2025-11-05T10:30:00"
}
```

**에러 응답 (400 Bad Request)**
```json
{
  "status": 400,
  "error": "Invalid File",
  "message": "허용되지 않는 파일 형식입니다: exe. 허용된 형식: jpg, jpeg, png, gif, ...",
  "path": "/api/files/upload",
  "timestamp": "2025-11-05T10:30:00"
}
```

**cURL 예시**
```bash
curl -X POST http://localhost:8080/api/files/upload \
  -F "file=@/path/to/file.jpg"
```

---

### 2. 다중 파일 업로드

여러 파일을 한 번에 업로드합니다.

**요청**
```http
POST /api/files/upload/multiple
Content-Type: multipart/form-data

files: [파일1]
files: [파일2]
files: [파일3]
```

**응답 (200 OK)**
```json
[
  {
    "id": 1,
    "originalFilename": "file1.jpg",
    "storedFilename": "uuid1.jpg",
    "fileSize": 102400,
    "mimeType": "image/jpeg",
    "fileType": "IMAGE",
    "fileExtension": "jpg",
    "downloadUrl": "http://localhost:8080/api/files/1/download",
    "createdAt": "2025-11-05T10:30:00"
  },
  {
    "id": 2,
    "originalFilename": "file2.png",
    "storedFilename": "uuid2.png",
    "fileSize": 204800,
    "mimeType": "image/png",
    "fileType": "IMAGE",
    "fileExtension": "png",
    "downloadUrl": "http://localhost:8080/api/files/2/download",
    "createdAt": "2025-11-05T10:30:01"
  }
]
```

**cURL 예시**
```bash
curl -X POST http://localhost:8080/api/files/upload/multiple \
  -F "files=@/path/to/file1.jpg" \
  -F "files=@/path/to/file2.png"
```

---

### 3. 파일 정보 조회

업로드된 파일의 메타데이터를 조회합니다.

**요청**
```http
GET /api/files/{id}
```

**파라미터**
- `id` (path) - 파일 ID

**응답 (200 OK)**
```json
{
  "id": 1,
  "originalFilename": "example.jpg",
  "storedFilename": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "fileSize": 102400,
  "mimeType": "image/jpeg",
  "fileType": "IMAGE",
  "fileExtension": "jpg",
  "downloadUrl": "http://localhost:8080/api/files/1/download",
  "createdAt": "2025-11-05T10:30:00"
}
```

**에러 응답 (404 Not Found)**
```json
{
  "status": 404,
  "error": "File Not Found",
  "message": "파일을 찾을 수 없습니다. ID: 999",
  "path": "/api/files/999",
  "timestamp": "2025-11-05T10:30:00"
}
```

**cURL 예시**
```bash
curl -X GET http://localhost:8080/api/files/1
```

---

### 4. 파일 다운로드

업로드된 파일을 다운로드합니다.

**요청**
```http
GET /api/files/{id}/download
```

**파라미터**
- `id` (path) - 파일 ID

**응답 (200 OK)**
- Content-Type: 파일의 MIME 타입
- Content-Disposition: attachment; filename="원본파일명"
- Body: 파일 바이너리 데이터

**cURL 예시**
```bash
curl -X GET http://localhost:8080/api/files/1/download \
  -o downloaded-file.jpg
```

---

### 5. 전체 파일 목록 조회 (페이징)

업로드된 모든 파일 목록을 페이징하여 조회합니다.

**요청**
```http
GET /api/files?page={page}&size={size}&sortBy={sortBy}&sortDir={sortDir}
```

**파라미터**
- `page` (query, optional) - 페이지 번호 (기본값: 0)
- `size` (query, optional) - 페이지 크기 (기본값: 10)
- `sortBy` (query, optional) - 정렬 기준 필드 (기본값: createdAt)
- `sortDir` (query, optional) - 정렬 방향 (ASC/DESC, 기본값: DESC)

**응답 (200 OK)**
```json
{
  "files": [
    {
      "id": 1,
      "originalFilename": "file1.jpg",
      "storedFilename": "uuid1.jpg",
      "fileSize": 102400,
      "mimeType": "image/jpeg",
      "fileType": "IMAGE",
      "fileExtension": "jpg",
      "downloadUrl": "http://localhost:8080/api/files/1/download",
      "createdAt": "2025-11-05T10:30:00"
    }
  ],
  "totalCount": 100,
  "page": 0,
  "size": 10,
  "totalPages": 10
}
```

**cURL 예시**
```bash
curl -X GET "http://localhost:8080/api/files?page=0&size=20&sortBy=fileSize&sortDir=DESC"
```

---

### 6. 파일 타입별 조회

특정 타입의 파일 목록을 조회합니다.

**요청**
```http
GET /api/files/type/{fileType}?page={page}&size={size}
```

**파라미터**
- `fileType` (path) - 파일 타입
  - `IMAGE` - 이미지 파일
  - `DOCUMENT` - 문서 파일
  - `SPREADSHEET` - 스프레드시트 파일
  - `PRESENTATION` - 프레젠테이션 파일
  - `ARCHIVE` - 압축 파일
  - `TEXT` - 텍스트 파일
  - `OTHER` - 기타
- `page` (query, optional) - 페이지 번호 (기본값: 0)
- `size` (query, optional) - 페이지 크기 (기본값: 10)

**응답 (200 OK)**
```json
{
  "files": [
    {
      "id": 1,
      "originalFilename": "photo.jpg",
      "storedFilename": "uuid1.jpg",
      "fileSize": 102400,
      "mimeType": "image/jpeg",
      "fileType": "IMAGE",
      "fileExtension": "jpg",
      "downloadUrl": "http://localhost:8080/api/files/1/download",
      "createdAt": "2025-11-05T10:30:00"
    }
  ],
  "totalCount": 25,
  "page": 0,
  "size": 10,
  "totalPages": 3
}
```

**cURL 예시**
```bash
curl -X GET "http://localhost:8080/api/files/type/IMAGE?page=0&size=10"
```

---

### 7. 파일 삭제

업로드된 파일을 삭제합니다. (소프트 삭제)

**요청**
```http
DELETE /api/files/{id}
```

**파라미터**
- `id` (path) - 파일 ID

**응답 (204 No Content)**
- 본문 없음

**cURL 예시**
```bash
curl -X DELETE http://localhost:8080/api/files/1
```

---

## 파일 타입 분류

시스템은 파일 확장자에 따라 자동으로 타입을 분류합니다:

| 파일 타입 | 확장자 |
|----------|--------|
| IMAGE | jpg, jpeg, png, gif, bmp, webp, svg |
| DOCUMENT | doc, docx, pdf, txt, rtf, odt |
| SPREADSHEET | xls, xlsx, csv, ods |
| PRESENTATION | ppt, pptx, odp |
| ARCHIVE | zip, rar, 7z, tar, gz |
| TEXT | txt, md, log |
| OTHER | 위에 해당하지 않는 모든 파일 |

---

## 에러 코드

| HTTP 상태 코드 | 에러 타입 | 설명 |
|--------------|-----------|------|
| 400 | Invalid File | 잘못된 파일 (빈 파일, 허용되지 않는 확장자, Path Traversal 등) |
| 404 | File Not Found | 파일을 찾을 수 없음 |
| 413 | File Too Large | 파일 크기가 최대 허용 크기 초과 |
| 500 | File Storage Error | 파일 저장/삭제 중 서버 오류 |
| 500 | Internal Server Error | 기타 서버 내부 오류 |

---

## 제약사항

1. **최대 파일 크기**: 50MB
2. **최대 요청 크기**: 100MB (다중 파일 업로드 시)
3. **허용된 파일 확장자**: jpg, jpeg, png, gif, bmp, webp, svg, pdf, doc, docx, xls, xlsx, ppt, pptx, txt, zip
4. **파일명**: Path Traversal 공격 방지를 위해 ".." 패턴 차단

---

## 보안

1. 모든 파일은 UUID 기반 파일명으로 저장되어 원본 파일명 노출 방지
2. SHA-256 체크섬으로 파일 무결성 검증
3. 화이트리스트 기반 파일 확장자 필터링
4. 파일명 Path Traversal 공격 방어
