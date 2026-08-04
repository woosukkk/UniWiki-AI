# 공식 출처 수집 파이프라인

공식 사이트의 신규·변경 자료를 감지해 원시 자료, 위키 초안, 벡터 동기화 작업까지 연결하는 백엔드 파이프라인이다.

## 처리 흐름

```text
등록된 공식 출처
  -> 주기적 변경 감지
  -> 신규·변경 자료만 수집
  -> raw_official_documents에 원문과 SHA-256 저장
  -> official_wiki_documents로 위키와 원문 연결
  -> DRAFT 또는 APPROVED wiki_posts 생성·갱신
  -> APPROVED 문서 벡터 동기화 작업 등록
```

동일한 원문 URL에서 제목과 본문의 해시가 같으면 위키를 다시 만들지 않는다. 해시가 바뀌면 원시 자료와 기존 위키를 갱신한다. 자동 게시 출처가 아니면 변경된 문서는 다시 `DRAFT` 상태가 된다.

## 기본 출처

`OFFICIAL_SOURCE_BOOTSTRAP_ENABLED=true`이면 다음 출처를 이름 기준으로 중복 없이 등록한다. 모두 `autoPublish=false`이다.

| 출처 | 카테고리 | URL |
|---|---|---|
| 세종대학교 학사공지 | 학사 | `https://www.sejong.ac.kr/kor/intro/notice3.do` |
| 세종대학교 취업공지 | 진로·취업 | `https://www.sejong.ac.kr/kor/intro/notice6.do` |
| 세종대학교 장학공지 | 장학·지원 | `https://www.sejong.ac.kr/kor/intro/notice7.do` |
| 세종대학교 소프트웨어학과 공지 | 학교생활 | `https://dept.sejong.ac.kr/softwaredpt/board/notice.do` |

카테고리 ID가 설정돼 있으면 ID를 우선 사용하고, 없으면 정상 한글 이름으로 조회한다. 둘 다 찾지 못하면 해당 출처는 건너뛰고 경고 로그를 남긴다. `OFFICIAL_SOURCE_COLLECT_ON_STARTUP=true`이면 등록 직후 활성 출처를 한 번 수집한다.

## 보안 범위

- 모든 관리 API는 로그인한 `ADMIN` 사용자만 호출할 수 있다.
- 기본적으로 HTTPS와 `sejong.ac.kr` 및 그 하위 도메인만 허용한다.
- 허용 도메인은 `OFFICIAL_SOURCE_ALLOWED_HOST_SUFFIXES`에 쉼표로 추가한다.
- 공식 위키 작성자는 이메일로 먼저 찾고, 없으면 설정된 사용자 ID를 사용한다.

## 관리 API

- `POST /api/admin/official-sources`: 출처 등록
- `GET /api/admin/official-sources`: 출처와 마지막 수집 상태 조회
- `POST /api/admin/official-sources/{sourceId}/collect`: 즉시 수집
- `GET /api/admin/official-sources/documents`: 원시 자료와 처리 상태 조회
- `POST /api/admin/official-sources/documents/{rawDocumentId}/approve`: 초안 승인 및 벡터 동기화

출처 등록 예시:

```json
{
  "categoryId": 1,
  "name": "세종대학교 학사공지",
  "listUrl": "https://www.sejong.ac.kr/kor/intro/notice3.do",
  "articleLinkSelector": ".b-td-title .b-title-box > a[href*='mode=view'][href*='articleNo=']",
  "titleSelector": ".b-title-box > .b-title",
  "contentSelector": ".b-content-box",
  "autoPublish": false
}
```

## 운영 설정

기본값은 자동 등록, 시작 시 수집, 스케줄러가 모두 비활성화된 상태다.

```text
OFFICIAL_SOURCE_BOOTSTRAP_ENABLED=true
OFFICIAL_SOURCE_COLLECT_ON_STARTUP=false
OFFICIAL_SOURCE_SCHEDULER_ENABLED=true
OFFICIAL_SOURCE_INTERVAL_MS=3600000
OFFICIAL_SOURCE_MAX_ARTICLES=20
OFFICIAL_SOURCE_AUTHOR_ID=1
OFFICIAL_SOURCE_AUTHOR_EMAIL=official-source@local.invalid
OFFICIAL_SOURCE_ALLOWED_HOST_SUFFIXES=sejong.ac.kr
OFFICIAL_SOURCE_ACADEMIC_CATEGORY_ID=0
OFFICIAL_SOURCE_CAREER_CATEGORY_ID=0
OFFICIAL_SOURCE_SCHOLARSHIP_CATEGORY_ID=0
OFFICIAL_SOURCE_CAMPUS_LIFE_CATEGORY_ID=0
```

초기 운영 확인 때는 `OFFICIAL_SOURCE_MAX_ARTICLES=5`, `OFFICIAL_SOURCE_COLLECT_ON_STARTUP=true`로 소량 수집한 뒤 결과를 검수한다. 이후 시작 시 수집을 끄고 스케줄러를 켜는 것을 권장한다.
