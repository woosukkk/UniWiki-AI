# UniWiki AI Service

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
