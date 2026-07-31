# 세종대학교 연도별 위키 초안

`wiki-drafts-by-year.json`은 공식 원천자료를 연도와 학기별로 분리한 검토용 위키 초안이다.

- 강의시간표는 `YYYY-1`, `YYYY-2` 학기별로 분리한다.
- 교과과정과 공지는 한 문서에 하나의 연도만 포함한다.
- 모든 초안 상태는 `DRAFT`이며 검토 전에는 공개 문서나 벡터 검색 데이터로 사용하지 않는다.
- 공지는 게시일의 연도를 기준으로 묶고 원문 링크를 유지한다.
- 진로·취업 공지와 현장실습 선별 자료처럼 원천 간 중복이 있을 수 있다.

재생성:

```powershell
cd ai
.\.venv\Scripts\python.exe scripts\create_yearly_wiki_drafts.py
```

생성되는 SQL은 `database/seed-sejong-yearly-wiki-drafts.sql`이다. 현재 백엔드는 `DRAFT`도 일반 위키 목록에 노출하므로 검토 및 공개 상태 필터 구현 전에는 SQL을 DB에 적용하지 않는다.
