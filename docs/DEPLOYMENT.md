# 배포 가이드

## 권장 구성

```text
Vercel 프론트엔드
→ Railway Spring Boot 백엔드
   ├─ Railway MySQL
   └─ Railway AI 서비스 → 영속 Chroma 저장소 / OpenAI API

인가된 로컬 수집 작업자
→ 백엔드 import API
```

Selenium 수집기는 Chrome 로그인과 영속 브라우저 프로필이 필요하므로 서버리스 환경이 아닌 인가된 로컬 작업자에서 실행합니다.

## 필수 환경 변수

### 백엔드

| 변수 | 용도 |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | 운영 MySQL 연결 |
| `JWT_SECRET` | 32자 이상 JWT 서명 키 |
| `CORS_ALLOWED_ORIGINS` | 허용할 Vercel 도메인 목록 |
| `AI_SERVICE_BASE_URL` | AI 서비스 주소 |
| `EVERYTIME_IMPORT_TOKEN` | 로컬 수집 결과 import 검증 토큰 |
| `PORT` | 플랫폼이 할당한 포트 |

### AI 서비스

| 변수 | 용도 |
| --- | --- |
| `OPENAI_API_KEY` | 답변 생성 API 키 |
| `OPENAI_MODEL` | 사용할 모델명 |
| `RAG_PARTIAL_MIN_SCORE` | 부분 근거 최소 점수. 기본값 `0.20` |
| `CHROMA_PERSIST_DIR` | 영속 볼륨 경로(예: `/data/chroma`) |
| `PORT` | 플랫폼이 할당한 포트 |

### 프론트엔드

| 변수 | 용도 |
| --- | --- |
| `VITE_API_BASE_URL` | 공개 백엔드 URL |

실제 비밀 값은 각 플랫폼의 Secret 기능으로 관리하고 저장소에 커밋하지 않습니다.

## 로컬 배포 점검

```powershell
Copy-Item infra/.env.example infra/.env
docker compose --env-file infra/.env -f infra/docker-compose.yml up --build
```

- 프론트엔드: `http://localhost:5173`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- AI 상태: `http://localhost:8000/health`

`infra/.env`는 커밋하지 않습니다. MySQL과 Chroma 볼륨은 업데이트 전에 백업합니다.

## 릴리스 순서

1. 백엔드 테스트, 프론트엔드 빌드, AI 테스트를 실행합니다.
2. 운영 스키마와 필요한 migration을 검토해 적용합니다.
3. AI 서비스를 먼저 배포하고 `/health`를 확인합니다.
4. 백엔드를 배포해 DB 연결, 포트 기동, AI 연결을 확인합니다.
5. 프론트엔드를 배포하고 CORS·로그인·새로고침 라우팅을 확인합니다.
6. 승인된 위키를 벡터 저장소에 동기화하고 검색 출처를 확인합니다.
7. 공식 자료 수집, 질문 선정, 관리자 API를 실제 권한별로 점검합니다.

## 운영 확인 체크리스트

- 비밀 키와 쿠키가 로그·이미지·Git 이력에 없음
- Vercel 운영/미리보기 도메인이 CORS 설정과 일치
- 재배포 후 Chroma 데이터가 유지됨
- 공식 위키와 함께 만든 위키가 AI 검색 결과에 모두 포함됨
- 수집 import API가 관리자 권한과 전용 토큰으로 보호됨
- 배포 태그와 실행 중인 커밋이 일치함
- AI 질문 직후 Railway 로그에 `Killed` 또는 컨테이너 재시작이 없음
- AI 검색이 전체 Chroma 레코드가 아닌 최대 60개 후보와 선택된 위키 청크만 조회함
