# Official Source Pipeline

공식 출처의 신규·변경 자료를 감지하고 위키와 벡터 DB까지 연결하는 백엔드 파이프라인이다.

## 처리 흐름

```text
official_sources
  -> HTML 목록 및 상세 페이지 수집
  -> raw_official_documents 원문·SHA-256 저장
  -> official_wiki_documents로 위키와 원문 연결
  -> DRAFT 또는 APPROVED wiki_posts 생성·갱신
  -> APPROVED 문서 벡터 동기화 작업 등록
```

동일한 원문 URL의 제목과 본문 해시가 같으면 다시 위키를 생성하지 않는다. 해시가 변경되면 원시 자료와 기존 위키를 갱신한다. 자동 게시 출처가 아닌 경우 변경된 위키는 다시 `DRAFT`가 된다.

## 보안 범위

- 모든 관리 API는 로그인한 `ADMIN` 사용자만 호출할 수 있다.
- 기본적으로 HTTPS와 `sejong.ac.kr` 및 그 하위 도메인만 허용한다.
- `OFFICIAL_SOURCE_ALLOWED_HOST_SUFFIXES`에서 허용할 도메인 접미사를 쉼표로 추가할 수 있다.

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
  "articleLinkSelector": "a[href*='mode=view']",
  "titleSelector": "h4, .view-title, .board-view-title",
  "contentSelector": ".view-content, .board-view-content, article",
  "autoPublish": false
}
```

선택자는 실제 페이지 DOM을 확인한 뒤 출처별로 등록해야 한다. 처음에는 `autoPublish=false`로 검증하고 제목·본문 추출이 안정적인 출처만 자동 게시로 전환하는 것을 권장한다.

## 스케줄러

기본값은 비활성화다.

```text
OFFICIAL_SOURCE_SCHEDULER_ENABLED=true
OFFICIAL_SOURCE_INTERVAL_MS=3600000
OFFICIAL_SOURCE_MAX_ARTICLES=20
OFFICIAL_SOURCE_AUTHOR_ID=1
OFFICIAL_SOURCE_ALLOWED_HOST_SUFFIXES=sejong.ac.kr
```

스케줄러는 활성 출처를 순서대로 확인한다. 한 출처가 실패해도 다음 출처 수집은 계속하며, 출처 행에 마지막 확인 시각과 오류를 기록한다.
