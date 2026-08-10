# UniWiki-AI

세종대학교 생활 정보를 공식 자료와 학생 질문·답변으로 정리하고, 실제 위키 문서를 근거로 AI 답변을 제공하는 서비스입니다.

> 2026 콘텐츠소프트웨어학과 학술제 최종 릴리스: [`v1.5.1`](https://github.com/woosukkk/UniWiki-AI/releases/tag/v1.5.1)

## 학술제 심사 안내

UniWiki-AI는 별도의 실행 파일을 내려받아 실행하는 프로그램이 아니라, Vercel과 Railway에 배포된 웹 프로젝트입니다. 따라서 GitHub Releases에는 로컬 실행 파일 대신 심사 기준 커밋을 나타내는 `v1.5.1` 태그를 등록했습니다.

| 항목 | 주소 |
|---|---|
| 배포 웹사이트 | https://frontend-six-pi-h5i7tztups.vercel.app |
| 위키 문서 목록 | https://frontend-six-pi-h5i7tztups.vercel.app/wiki |
| AI 챗봇 | https://frontend-six-pi-h5i7tztups.vercel.app/chatbot |
| 질문 게시판 | https://frontend-six-pi-h5i7tztups.vercel.app/questions |
| GitHub Repository | https://github.com/woosukkk/UniWiki-AI |
| 제출 릴리스 | https://github.com/woosukkk/UniWiki-AI/releases/tag/v1.5.1 |

심사 시에는 별도 설치 없이 배포 웹사이트에 접속하면 됩니다. 위키 열람과 AI 질문은 로그인하지 않아도 사용할 수 있으며, 질문·답변 작성 등 사용자 기능은 회원가입 후 사용할 수 있습니다.

로컬에서 전체 서비스를 실행하려면 Java 21, Python 3.10 이상, Node.js, Docker가 필요합니다. 아래의 `로컬 실행` 절차에 따라 MySQL, 백엔드, AI 서비스, 프론트엔드를 순서대로 실행합니다. 비밀키와 운영 계정 정보는 저장소에 포함하지 않으므로 AI 답변 등 외부 서비스 연동에는 별도의 환경변수 설정이 필요합니다.

## 주요 기능

### v1.5.1 수집 정책 및 위키 UI 안정화

- 공식 문서 적재 정책: SW중심대학사업단·TOSC·uDream은 출처별 통합, 그 외는 하위 문서 1건당 위키 1건
- 2024년 이후 공식 문서를 수집하며 관리자가 즉시 수집할 수 있고 정기 수집은 매일 자정 실행
- 기존 적재 자료는 유지하고 이후 네트워크 요청부터 세종대학교 `robots.txt` 정책과 호스트별 요청 간격 적용
- 운영 DB에 남은 혼합 레거시 인코딩 카테고리를 API에서 정상 한글로 복원하고 정상 ID를 우선해 중복 제거
- 위키 목록의 작성 버튼을 데스크톱·모바일에서 일관된 일반 버튼 크기와 정렬로 조정
- 완료된 벡터 동기화 작업의 본문과 중복 작업을 소규모 배치로 정리해 MySQL·컨테이너 용량 사용을 제한
- AI 검색은 Chroma 전체를 메모리에 올리지 않고 제한된 벡터 후보와 정확 키워드 후보를 결합
- 근거 부족 답변은 질문 게시판 작성 흐름으로 연결

- 세종대학교 공식 공지와 PDF·Excel·DOCX 첨부파일 수집
- 연도·학기·카테고리별 위키 검색과 출처 확인
- 질문, 답변, 추천 및 답변 완료 상태 관리
- 관리자가 질문·답변을 `함께 만든 위키`로 선정
- 공식 위키와 함께 만든 위키를 함께 검색하는 RAG 챗봇
- 데이터 수집량·최신성·출처 신뢰도를 보여주는 데이터 지도
- 관리자 대시보드와 공식 출처 수집 관리
- 자정 자동 수집과 관리자 수동 수집, 중복 실행 방지
- 전체 벡터 저장소를 읽지 않는 메모리 제한형 AI 검색

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

프론트엔드는 OpenAI나 데이터베이스에 직접 접근하지 않습니다. 백엔드가 인증과 비즈니스 로직을 처리하고, AI 서비스는 동기화된 위키만 검색해 답변과 실제 출처를 반환합니다. AI 검색은 벡터 후보 60개와 서버에서 필터링한 핵심어 후보 20개 안에서 재정렬하고, 최종 선택된 위키의 청크만 조회해 컨테이너 메모리를 일정하게 유지합니다.

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
