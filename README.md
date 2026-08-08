# UniWiki-AI

세종대학교 생활 정보를 공식 자료와 학생 질문·답변으로 정리하고, 실제 위키 문서를 근거로 AI 답변을 제공하는 서비스입니다.

> 현재 릴리스: `v1.3.0`

## 주요 기능

- 세종대학교 공식 공지와 PDF·Excel·DOCX 첨부파일 수집
- 연도·학기·카테고리별 위키 검색과 출처 확인
- 질문, 답변, 추천 및 답변 완료 상태 관리
- 관리자가 질문·답변을 `함께 만든 위키`로 선정
- 공식 위키와 함께 만든 위키를 함께 검색하는 RAG 챗봇
- 데이터 수집량·최신성·출처 신뢰도를 보여주는 데이터 지도
- 관리자 대시보드와 공식 출처 수집 관리

## 시스템 구조

```text
React + Vite
      │ REST API / JWT
      ▼
Spring Boot ─────────── MySQL
      │                   │
      │ vector sync jobs  │ 원본 데이터
      ▼                   │
FastAPI ────────────── ChromaDB
      │ retrieved context
      ▼
OpenAI Responses API
```

프론트엔드는 OpenAI나 데이터베이스에 직접 접근하지 않습니다. 백엔드가 인증과 비즈니스 로직을 처리하고, AI 서비스는 동기화된 위키만 검색해 답변과 실제 출처를 반환합니다.

## 기술 스택

| 영역 | 기술 |
|---|---|
| Frontend | React 19, Vite 8, React Router 7, Axios |
| Backend | Java 21, Spring Boot 3.4.5, Spring Data JPA, JWT |
| Database | MySQL, H2 |
| AI | Python, FastAPI, FastEmbed, ChromaDB, OpenAI Responses API |
| Collection | Jsoup, Selenium, PDFBox, Apache POI |
| Deployment | Vercel, Railway, Docker |

## 저장소 구조

```text
UniWiki-AI/
├── backend/    Spring Boot API, 수집 및 워크플로
├── frontend/   React 웹 애플리케이션
├── ai/         임베딩, 벡터 검색, RAG 답변 서비스
├── database/   MySQL 스키마와 마이그레이션
├── infra/      로컬 Docker Compose 구성
└── docs/       설계, 운영 및 발표 문서
```

## 로컬 실행

### 1. 인프라

```powershell
Copy-Item infra/.env.example infra/.env
docker compose --env-file infra/.env -f infra/docker-compose.yml up -d mysql
```

### 2. 백엔드

Java 21이 필요합니다.

```powershell
cd backend
./gradlew.bat bootRun
```

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui/index.html`

### 3. AI 서비스

Python 3.10 이상이 필요합니다.

```powershell
cd ai
python -m venv .venv
./.venv/Scripts/python.exe -m pip install -r requirements.txt
./.venv/Scripts/python.exe -m uvicorn app.main:app --reload --port 8000
```

- API 문서: `http://localhost:8000/docs`

AI 답변을 생성하려면 실행 환경에 `OPENAI_API_KEY`를 설정합니다. 키는 프론트엔드나 저장소에 저장하지 않습니다.

### 4. 프론트엔드

```powershell
cd frontend
npm install
npm run dev
```

- 웹: `http://localhost:5173`
- 개발 서버는 `/api` 요청을 기본적으로 `http://localhost:8080`으로 전달합니다.

## 검증

```powershell
cd backend
./gradlew.bat test

cd ../frontend
npm run build

cd ../ai
./.venv/Scripts/python.exe -m pytest -q
```

## 데이터 흐름

### 공식 자료

```text
등록된 공식 출처
→ 변경 감지
→ 원문·첨부파일 저장 및 텍스트 추출
→ 위키 생성·갱신
→ 벡터 동기화 작업
→ AI 검색 반영
```

### 학생 커뮤니티 자료

```text
로컬 수집
→ 욕설 필터링
→ 질문과 댓글·답변으로 저장
→ 관리자 선정
→ 함께 만든 위키
→ AI 검색 반영
```

커뮤니티 원문은 자동으로 위키가 되지 않습니다. 질문 게시판을 거쳐 관리자가 선정한 자료만 함께 만든 위키와 AI 출처로 사용됩니다.

## 문서

| 문서 | 내용 |
|---|---|
| [아키텍처](docs/architecture.md) | 서비스 경계와 주요 처리 흐름 |
| [API 명세](docs/api-spec.md) | 백엔드·AI 주요 엔드포인트 |
| [ERD](docs/erd.md) | 핵심 데이터 모델 |
| [개발 규칙](docs/convention.md) | 코드·브랜치·커밋·보안 규칙 |
| [배포 가이드](docs/DEPLOYMENT.md) | Railway·Vercel 운영 구성 |
| [공식 출처 파이프라인](docs/OFFICIAL_SOURCE_PIPELINE.md) | 변경 감지와 첨부파일 처리 |
| [AI 튜닝 이력](docs/AI_TUNING_HISTORY.md) | 검색·프롬프트·점수 개선 과정 |
| [크롤러 가이드](backend/CRAWLER_GUIDE.md) | 승인된 로컬 수집과 업로드 |
| [발표: 백엔드](docs/PRESENTATION_BACKEND.md) | 백엔드 기술과 시연 흐름 |
| [발표: 프론트엔드](docs/PRESENTATION_FRONTEND.md) | 프론트엔드 기술과 시연 흐름 |
| [발표: AI](docs/PRESENTATION_AI_PIPELINE.md) | 벡터 검색과 RAG 처리 |

## 보안 원칙

- `.env`, JWT 비밀키, DB 비밀번호, OpenAI 키를 커밋하지 않습니다.
- 작성자 정보는 요청 본문이 아니라 검증된 JWT에서 가져옵니다.
- 크롤러 로그인 정보와 세션은 승인된 로컬 환경에서만 사용합니다.
- AI 답변은 검색된 위키 자료만 근거로 사용하며 근거가 부족하면 이를 명시합니다.

## 라이선스

현재 별도 라이선스가 지정되지 않았습니다. 외부 사용이나 재배포 전 저장소 소유자에게 확인하세요.
