# 개발 규칙

## 공통 원칙

- 요청 범위에 필요한 코드만 수정합니다.
- 실제 코드와 문서가 다르면 런타임 코드를 기준으로 문서를 갱신합니다.
- 비밀키, 비밀번호, 세션 쿠키와 실제 `.env`를 커밋하지 않습니다.
- 기능 단위로 테스트하고 의미 있는 단위로 커밋합니다.

## 브랜치와 커밋

브랜치 예시:

```text
feat/wiki-search
fix/auth-session
docs/readme
release/v1.3.0
```

커밋 메시지는 영어 명령형으로 작성합니다.

```text
feat: add official source collection
fix: preserve department terms in search
docs: update deployment guide
```

강제 푸시와 이력 재작성은 협업자에게 영향을 주므로 사전 합의 없이 수행하지 않습니다.

## 백엔드

- Java 21과 Gradle Wrapper를 사용합니다.
- 패키지는 `controller`, `service`, `repository`, `dto`, `entity`, `config`, `util` 계층을 유지합니다.
- Controller는 HTTP와 검증, Service는 비즈니스 로직과 트랜잭션을 담당합니다.
- 인증 사용자 ID는 `@LoginUserId Long userId`로 받습니다.
- 클라이언트가 보낸 작성자 ID를 신뢰하지 않습니다.
- 엔티티를 응답으로 직접 반환하지 않고 DTO로 변환합니다.
- 쓰기 작업에는 `@Transactional`, 조회 중심 서비스에는 읽기 전용 트랜잭션을 사용합니다.
- `ManyToOne`은 특별한 이유가 없으면 지연 로딩합니다.
- enum은 문자열로 저장합니다.

검증:

```powershell
cd backend
./gradlew.bat test
./gradlew.bat clean build
```

## 프론트엔드

- 페이지는 `src/pages`, 재사용 UI는 `src/components`에 둡니다.
- API URL과 HTTP 처리는 `src/api.js`에 모읍니다.
- 인증 상태는 `AuthContext`와 `auth.js`를 사용합니다.
- 공개, 로그인, 관리자 경로는 기존 Route guard를 재사용합니다.
- 공통 색상과 간격은 `styles/tokens.css`를 우선 사용합니다.
- 서버 비밀값과 OpenAI 키를 `VITE_` 환경 변수로 노출하지 않습니다.

검증:

```powershell
cd frontend
npm run build
```

## AI

- 외부 요청과 응답은 Pydantic 모델로 검증합니다.
- 사용자 질문 원문은 응답에서 유지하고 검색용 표현만 확장합니다.
- 공식 위키와 함께 만든 위키에 같은 기본 점수 규칙을 적용합니다.
- 특정 문장이나 위키 ID를 검색 우선순위에 하드코딩하지 않습니다.
- 근거 점수 미달 시 LLM을 호출하지 않습니다.
- 출처는 검색된 실제 `wikiPostId`로 구성합니다.
- 임베딩 모델이나 청크 규칙을 바꾸면 기존 벡터의 재생성 여부를 검토합니다.

검증:

```powershell
cd ai
./.venv/Scripts/python.exe -m pytest -q
```

## 데이터와 수집

- 원본 파일은 `ai/data/raw`, 가공 결과는 `ai/data/normalized`에 둡니다.
- 원본 URL, 게시일·시행일과 수집 기준을 README에 기록합니다.
- 공식 자료와 커뮤니티 자료의 처리 경로를 섞지 않습니다.
- 커뮤니티 로그인과 세션은 승인된 로컬 환경에만 둡니다.
- 운영 적재 전에 중복, 인코딩, 개인정보와 욕설 필터 결과를 확인합니다.

## 문서

- 루트 README에는 프로젝트 소개와 시작 방법만 유지합니다.
- 상세 설계는 `docs/`에서 관리하고 README에서 링크합니다.
- 실제로 실행한 검증만 성공했다고 기록합니다.
- 오래된 경로, 환경 변수와 배포 주소가 남지 않도록 릴리스 전에 링크를 점검합니다.
