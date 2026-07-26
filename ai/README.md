# UniWiki AI Service

## RAG answer generation

`POST /api/rag/answers`

```json
{
  "question": "수강신청은 어디서 하나요?",
  "categoryId": 1
}
```

Only retrieved chunks at or above `RAG_MIN_SCORE` are passed to the language model. When evidence is insufficient, the API skips the model call and returns `grounded: false`. Set `OPENAI_API_KEY` locally to enable answer generation; never commit the key.

위키 문서를 청킹하고 다국어 임베딩 벡터를 생성하는 FastAPI 서비스입니다.

## 로컬 실행

Python 3.10 이상이 필요합니다.

```powershell
cd ai
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn app.main:app --reload --port 8000
```

API 문서: `http://localhost:8000/docs`

## 테스트

```powershell
cd ai
.\.venv\Scripts\python.exe -m pytest -q
```

## 위키 문서 임베딩

`POST /api/embeddings/wiki-posts`

```json
{
  "wikiPostId": 1,
  "title": "수강신청 안내",
  "content": "수강신청은 학교 포털에서 진행합니다.",
  "categoryId": 1
}
```

응답의 각 청크에는 다음 정보가 포함됩니다.

- 안정적인 `chunkId`
- 원문 청크 `text`
- 정규화된 `embedding`
- `wikiPostId`, `title`, `categoryId`, `chunkIndex` 메타데이터

기본 모델은 `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`이며 384차원 벡터를 생성합니다. 모델은 첫 임베딩 요청 시 로딩됩니다.

청크 크기, 오버랩 및 모델은 환경 변수로 변경할 수 있습니다. 지원 항목은 `.env.example`을 참고하세요.

## Vector DB

ChromaDB 데이터를 기본적으로 `ai/data/vectorstore/chroma`에 영속 저장합니다. 이 경로는 Git에서 제외됩니다.

```text
PUT    /api/vector-store/wiki-posts
GET    /api/vector-store/wiki-posts/{wikiPostId}
DELETE /api/vector-store/wiki-posts/{wikiPostId}
GET    /api/vector-store/stats
```

저장 API의 요청 형식은 위키 문서 임베딩 API와 같습니다. 동일 문서를 다시 저장하면 같은 청크는 갱신되고 이전 버전에만 존재하는 청크는 제거됩니다.

## 유사 문서 검색

`POST /api/search/wiki-posts`

```json
{
  "query": "수강신청은 어디서 하나요?",
  "topK": 5,
  "categoryId": 1
}
```

`categoryId`와 `topK`는 선택 항목입니다. 결과는 코사인 유사도 내림차순이며 각 항목에 `wikiPostId`, `title`, `content`, `score`, 청크 메타데이터가 포함됩니다.
