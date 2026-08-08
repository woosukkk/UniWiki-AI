# 에브리타임 수집 가이드

## 운영 원칙

에브리타임 수집기는 인증이 필요한 로컬 작업자에서만 실행합니다. 운영 서버에는 계정 정보나 브라우저 쿠키를 저장하지 않고, 수집 결과만 토큰으로 보호된 import API에 전달합니다. 서비스 이용약관과 접근 권한을 준수해야 합니다.

```text
로컬 Chrome 수집기
→ 원시 데이터 import
→ 개인정보 마스킹·욕설 필터링
→ 질문/답변 데이터 변환
→ 관리자 선정
→ 함께 만든 위키 및 AI 벡터 반영
```

## 주요 환경 변수

| 변수 | 설명 |
| --- | --- |
| `EVERYTIME_CRAWL_ENABLED` | 로컬 브라우저 수집 활성화 여부 |
| `EVERYTIME_HEADLESS` | 브라우저 숨김 실행 여부. 최초 로그인은 `false` 권장 |
| `EVERYTIME_IMPORT_TOKEN` | 운영 import API가 검증할 토큰 |
| `EVERYTIME_UPLOAD_TOKEN` | 로컬 작업자가 전송할 동일 토큰 |
| `EVERYTIME_COMMUNITY_UPLOAD_URL` | 커뮤니티 import API 주소 |
| `EVERYTIME_UPLOAD_URL` | 강의평 import API 주소 |

실제 값은 실행 환경에서만 관리하며 `.env`나 브라우저 프로필을 Git에 올리지 않습니다.

## 관리자 API

기본 경로는 `/api/admin/crawl`입니다. 브라우저 수집·조회·후처리 API는 관리자 로그인을 사용하고, 외부 작업자용 import·통계 API는 `X-Crawler-Token` 헤더를 검증합니다.

| 메서드 | 경로 | 용도 |
| --- | --- | --- |
| POST | `/everytime/board` | 로컬 브라우저로 게시판 수집 |
| GET | `/everytime/board` | 저장된 커뮤니티 원시 데이터 조회 |
| POST | `/everytime/community/import` | 외부 작업자의 게시물·댓글·추천 수 업로드 |
| GET | `/everytime/community/stats` | 커뮤니티 처리 통계 |
| POST | `/everytime/lecture` | 단일 강의평 수집 |
| POST | `/everytime/lecture/batch` | 교과정보 기반 강의·교수 조합 일괄 수집 |
| POST | `/everytime/lecture/import` | 외부 작업자의 강의평 업로드 |
| POST | `/everytime/lecture/process` | 저장된 강의평 후처리 |
| GET | `/everytime/lecture` | 강의평 처리 상태 조회 |

정확한 요청 필드는 실행 중인 Swagger UI에서 확인합니다.

## 커뮤니티 데이터 흐름

게시물 제목·본문·댓글·추천 수를 각각 보존해 원시 테이블에 저장합니다. 현재 필터는 욕설과 개인정보 같은 안전 항목에 집중하며, 주제만으로 게시물을 임의 제외하지 않습니다.

원시 게시물은 질문으로, 댓글은 해당 질문의 답변으로 변환됩니다. 댓글이 있으면 질문은 답변 완료 상태가 됩니다. 이 단계에서 곧바로 위키가 생성되지는 않습니다. 관리자가 질문 또는 답변을 선정하면 `함께 만든 위키`가 생성되고 벡터 동기화 큐에 등록됩니다.

## 강의평 데이터 흐름

강의명과 교수명을 교과정보와 대조하고, 개인정보 마스킹과 품질 검사를 거친 뒤 처리 상태를 기록합니다. 강의평 공개 UI는 현재 숨겨져 있으므로 데이터 수집·검수 기능과 사용자 노출 여부를 구분해 운영합니다.

## 점검 항목

- 수집 계정이 대상 게시판을 열람할 권한이 있는지 확인
- Chrome 프로필을 동시에 여러 프로세스에서 사용하지 않기
- 운영 import URL과 토큰이 서로 일치하는지 확인
- 원문 URL 중복 저장 여부와 댓글·추천 수 매핑 확인
- 로그나 오류 응답에 쿠키·토큰·개인정보가 노출되지 않는지 확인
