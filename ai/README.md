# UniWiki-AI AI 서비스

위키 문서를 청킹·임베딩하고 ChromaDB에서 검색해 근거 기반 답변과 요약을 제공하는 FastAPI 서비스입니다.

## 처리 흐름

```text
위키 UPSERT
→ 800자 청크와 120자 중첩
→ 다국어 임베딩
→ ChromaDB 저장

사용자 질문
→ 질문 정규화·학사 의도 확장
→ 벡터 + 어휘 하이브리드 검색
→ 최소 점수 검사
→ 관련 청크 확장
→ OpenAI 답변
→ 실제 위키 출처 반환
```

## 로컬 실행

Python 3.10 이상이 필요합니다.

```powershell
cd ai
python -m venv .venv
./.venv/Scripts/python.exe -m pip install -r requirements.txt
./.venv/Scripts/python.exe -m uvicorn app.main:app --reload --port 8000
```

- API 문서: `http://localhost:8000/docs`
- `OPENAI_API_KEY`가 없으면 임베딩과 검색은 가능하지만 LLM 답변 생성은 실패합니다.

## 테스트

```powershell
cd ai
./.venv/Scripts/python.exe -m pytest -q
```

## 주요 API

### 임베딩

`POST /api/embeddings/wiki-posts`

```json
{
  "wikiPostId": 1,
  "title": "수강신청 안내",
  "content": "수강신청은 학교 포털에서 진행합니다.",
  "categoryId": 1
}
```

### 벡터 저장소

```text
PUT    /api/vector-store/wiki-posts
GET    /api/vector-store/wiki-posts/{wikiPostId}
DELETE /api/vector-store/wiki-posts/{wikiPostId}
GET    /api/vector-store/stats
```

같은 위키를 다시 저장하면 현재 청크를 갱신하고 이전 버전에만 존재하는 청크를 삭제합니다.

### 검색

`POST /api/search/wiki-posts`

```json
{
  "query": "수강신청은 어디서 하나요?",
  "topK": 5,
  "categoryId": 1
}
```

`topK`와 `categoryId`는 선택 항목입니다. 벡터 유사도와 제목·본문 어휘 일치를 같은 점수 범위에서 결합합니다.

### RAG 답변

`POST /api/rag/answers`

```json
{
  "question": "소프트웨어학과 졸업하려면 뭐 필요해?"
}
```

질문은 먼저 LLM이 공식 문서에 적합한 검색어로 재작성하며, 원문과 재작성 문장을 함께 검색합니다. 재작성 결과는 최대 256개까지 메모리에 캐시합니다.

`RAG_MIN_SCORE` 이상의 문서는 충분한 근거로 사용합니다. `RAG_PARTIAL_MIN_SCORE` 이상이지만 충분한 근거 기준보다 낮은 문서는 답변에 참고하되 `grounded: false`로 반환해 화면에 `LIMITED SOURCES`로 표시합니다. 부분 근거 기준에도 미치지 못하면 빈 출처와 근거 부족 응답을 반환합니다.

### 위키 요약

`POST /api/summaries/wiki-posts/{wikiPostId}?maxChars=600`

긴 문서는 청크 묶음별로 요약한 뒤 최종 결과로 축약합니다. `maxChars`는 100~2000입니다.

## 기본 설정

| 변수 | 기본값 | 설명 |
|---|---|---|
| `EMBEDDING_MODEL` | `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2` | 다국어 임베딩 모델 |
| `CHUNK_MAX_CHARS` | `800` | 청크 최대 문자 수 |
| `CHUNK_OVERLAP_CHARS` | `120` | 인접 청크 중첩 |
| `SEARCH_TOP_K` | `5` | 검색 결과 수 |
| `RAG_TOP_K` | `5` | RAG 후보 위키 수 |
| `RAG_MIN_SCORE` | `0.35` | 최소 근거 점수 |
| `RAG_PARTIAL_MIN_SCORE` | `0.20` | 제한적 참고 자료의 최소 점수 |
| `RAG_MAX_CONTEXT_CHARS` | `12000` | 최대 문맥 길이 |
| `CHROMA_PERSIST_DIR` | `ai/data/vectorstore/chroma` | 벡터 저장 경로 |
| `OPENAI_MAX_OUTPUT_TOKENS` | `1000` | 답변 최대 출력량 |

전체 환경 변수는 [`.env.example`](.env.example)을 참고하세요.

## 운영 원칙

- MySQL의 위키가 원본이며 ChromaDB는 검색 인덱스입니다.
- 공식 위키와 함께 만든 위키는 같은 컬렉션과 점수 규칙을 사용합니다.
- 답변은 검색된 문서만 근거로 사용합니다.
- 출처 URL은 `/api/wiki-posts/{wikiPostId}` 형식으로 반환합니다.
- OpenAI 키와 내부 AI 서비스 주소는 프론트엔드에 노출하지 않습니다.

검색 개선 과정은 [`docs/AI_TUNING_HISTORY.md`](../docs/AI_TUNING_HISTORY.md), 전체 구조는 [`docs/PRESENTATION_AI_PIPELINE.md`](../docs/PRESENTATION_AI_PIPELINE.md)를 참고하세요.

### 카테고리 전체 검색

질문마다 벡터·키워드 검색으로 관련 카테고리를 동적으로 최대 3개 선택한 뒤, 선택된 카테고리의 모든 문서를 가져와 다시 정렬합니다. 특정 위키 ID, 문서 제목, 졸업·등록금·장학금 전용 키는 사용하지 않습니다. 사용자가 카테고리를 지정한 경우에는 해당 카테고리만 전체 검색합니다.

벡터 저장은 Chroma에 다시 읽기를 수행해 청크 수를 검증하며, 실패하면 오류로 처리되어 자동 동기화 큐 또는 관리자 재색인에서 재시도됩니다. AI 이미지 빌드는 전체 회귀 테스트를 실행하므로 검색 테스트가 실패하면 배포되지 않습니다. RAG 검색어 캐시는 서비스 생성 시 초기화됩니다.

공식 자료를 초기화해야 하는 예외적 복구에서는 MySQL 공식 위키·원문과 Chroma 벡터를 같은 범위로 정리한 뒤 수집 → 벡터 동기화 순서로 진행합니다. 현재 운영 데이터는 유지하며 `OFFICIAL_SOURCE_RESET_ENABLED=false`, `OFFICIAL_SOURCE_COLLECT_ON_STARTUP=false`입니다. 이후 자정·관리자 수집 요청에만 robots 정책을 적용하고 변경된 위키만 벡터 동기화합니다.
