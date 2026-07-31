# 세종대학교 정규화 데이터

`ai/scripts/normalize_sejong_data.py`가 공식 XLSX 원문에서 생성한다.

`ai/scripts/build_faculty_course_index.py`는 공식 교수진 프로필과 강의시간표를 교수명으로 연결해
`software-faculty-course-index.json`을 생성한다. 동명이인이 생기거나 시간표의 교수 표기가 바뀌면
이름 연결 결과를 수동으로 확인해야 한다.

- `software-course-schedules.json`: 학기별 소프트웨어학과·콘텐츠소프트웨어학과 개설 강좌
- `curriculum-comparison.json`: 2024~2026 교과과정과 연도 간 추가·제거·이수구분·학점 변경 비교

원문 파일을 수정하지 않으며 스크립트를 다시 실행하면 동일 경로에 최신 결과를 생성한다.
