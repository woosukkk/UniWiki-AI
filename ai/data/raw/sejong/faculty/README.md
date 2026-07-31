# 콘텐츠소프트웨어학과 교수진

세종대학교 콘텐츠소프트웨어학과 공식 교수소개 페이지에 공개된 교수진 12명의 업무용 프로필을 수집했다.

- 출처: https://dept.sejong.ac.kr/softwaredpt/intro/professor.do
- 확인일: 2026-07-31
- 항목: 이름, 직위, 학과장 여부, 전공 요약, 이메일, 전화, 연구실, 최종학위, 연구분야, 연구자 포털, 홈페이지
- `-`로 공개된 값은 JSON에서 `null` 또는 빈 배열로 저장했다.
- 연락처는 공식 학과 페이지에 공개된 업무용 연락처이며 개인 연락처는 포함하지 않는다.

`ai/scripts/build_faculty_course_index.py`를 실행하면 공식 강의시간표의 담당교수명과 연결한
`ai/data/normalized/sejong/software-faculty-course-index.json`을 다시 생성할 수 있다.
