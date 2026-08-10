# API 명세 요약

기본 백엔드 경로는 `/api`입니다. 상세 요청·응답 스키마는 실행 중인 Swagger UI(`/swagger-ui/index.html`)를 기준으로 합니다.

## 인증

로그인이 필요한 요청은 다음 헤더를 사용합니다.

```http
Authorization: Bearer {token}
```

| Method | Path | 인증 | 설명 |
|---|---|---:|---|
| POST | `/api/users/signup` | 없음 | 회원가입 |
| POST | `/api/users/login` | 없음 | 로그인 및 JWT 발급 |
| GET | `/api/users/me` | 필요 | 현재 사용자 조회 |

## 위키

| Method | Path | 인증 | 설명 |
|---|---|---:|---|
| GET | `/api/wiki-posts` | 없음 | 승인 위키 목록 |
| GET | `/api/wiki-posts/{id}` | 없음 | 위키 상세 |
| GET | `/api/wiki-posts/search` | 없음 | 제목·본문 검색과 출처 필터 |
| GET | `/api/wiki-posts/category/{categoryId}` | 없음 | 카테고리별 조회 |
| GET | `/api/wiki-posts/community` | 없음 | 함께 만든 위키 목록 |
| GET | `/api/wiki-posts/coverage` | 없음 | 데이터 지도 집계 |
| GET | `/api/wiki-posts/me` | 필요 | 내가 작성한 위키 |
| POST | `/api/wiki-posts` | 필요 | 위키 작성 |
| PUT | `/api/wiki-posts/{id}` | 필요 | 위키 수정 |
| DELETE | `/api/wiki-posts/{id}` | 필요 | 위키 삭제 |

검색 매개변수:

```text
keyword: 제목·본문 검색어
source: ALL, OFFICIAL, COMMUNITY
contentType: 선택 분류
```

## 질문과 답변

| Method | Path | 인증 | 설명 |
|---|---|---:|---|
| GET | `/api/questions` | 없음 | 질문 목록 |
| GET | `/api/questions/{id}` | 없음 | 질문 상세 |
| POST | `/api/questions` | 필요 | 질문 작성 |
| PUT | `/api/questions/{id}` | 필요 | 질문 수정 |
| DELETE | `/api/questions/{id}` | 필요 | 질문 삭제 |
| GET | `/api/questions/{id}/answers` | 없음 | 답변 목록 |
| POST | `/api/answers/questions/{questionId}` | 필요 | 답변 작성 |
| PUT | `/api/answers/{id}` | 필요 | 답변 수정 |
| DELETE | `/api/answers/{id}` | 필요 | 답변 삭제 |
| PATCH | `/api/answers/{id}/accept` | 필요 | 작성자·관리자 답변 채택 |

## 추천

위키, 질문, 답변은 같은 형태로 추천 API를 제공합니다.

```text
POST   /api/{resource}/{id}/likes
DELETE /api/{resource}/{id}/likes
GET    /api/{resource}/{id}/likes
GET    /api/{resource}/{id}/likes/count
```

`resource`는 `wiki-posts`, `questions`, `answers` 중 하나입니다.

## AI

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/ai/answers` | 위키 기반 AI 답변과 출처 반환 |
| POST | `/api/ai/wiki-posts/{id}/summary` | 위키 요약 |

답변 요청 예시:

```json
{
  "question": "소프트웨어학과 졸업하려면 뭐 필요해?",
  "categoryId": 2
}
```

`categoryId`는 선택 항목입니다. 응답의 `sources`는 실제 `/api/wiki-posts/{id}` 문서를 가리킵니다.

## 관리자

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/admin/dashboard` | 관리자 집계 |
| POST | `/api/admin/wiki-promotions/questions/{id}` | 질문을 함께 만든 위키로 선정 |
| POST | `/api/admin/wiki-promotions/answers/{id}` | 답변을 함께 만든 위키로 선정 |
| GET | `/api/admin/official-sources` | 공식 출처 목록 |
| POST | `/api/admin/official-sources` | 공식 출처 등록 |
| POST | `/api/admin/official-sources/{id}/collect` | 출처 즉시 수집 |
| POST | `/api/admin/official-sources/wiki-posts/{id}/reindex` | 특정 위키 벡터 재색인 |
| GET | `/api/admin/official-sources/documents` | 수집 문서 상태 |
| POST | `/api/admin/official-sources/documents/{id}/approve` | 초안 승인 |

관리 API는 JWT 사용자 역할이 `ADMIN`이어야 합니다.

## 크롤러 import

브라우저 수집은 승인된 로컬 환경에서 수행하고 운영에는 결과만 업로드합니다.

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/admin/crawl/everytime/lecture/import` | 강의평 원시 데이터 업로드 |
| POST | `/api/admin/crawl/everytime/community/import` | 게시물·댓글·추천 수 업로드 |
| POST | `/api/admin/crawl/everytime/lecture/process` | 강의평 처리 실행 |
| GET | `/api/admin/crawl/everytime/community/stats` | 커뮤니티 수집 통계 |

import API는 운영 설정에 따라 `X-Crawler-Token`을 요구합니다. 자세한 내용은 [크롤러 가이드](../backend/CRAWLER_GUIDE.md)를 참고하세요.

## AI 서비스 내부 API

| Method | Path | 설명 |
|---|---|---|
| GET | `/health` | 모델과 서비스 상태 |
| POST | `/api/embeddings/wiki-posts` | 문서 청킹과 임베딩 |
| PUT | `/api/vector-store/wiki-posts` | 벡터 문서 저장·교체 |
| GET | `/api/vector-store/wiki-posts/{id}` | 저장 청크 조회 |
| DELETE | `/api/vector-store/wiki-posts/{id}` | 벡터 문서 삭제 |
| GET | `/api/vector-store/stats` | 청크 수 조회 |
| POST | `/api/search/wiki-posts` | 하이브리드 위키 검색 |
| POST | `/api/rag/answers` | 근거 기반 답변 |
| POST | `/api/summaries/wiki-posts/{id}` | 위키 요약 |

AI 내부 API는 기본적으로 백엔드가 호출합니다. OpenAI 키를 프론트엔드에 전달하지 않습니다.
