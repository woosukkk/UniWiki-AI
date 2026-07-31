USE uniwiki_ai;

INSERT INTO users (email, password, nickname, role)
VALUES ('official-source@local.invalid', SHA2(UUID(), 256), '세종대 공식자료', 'USER')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

SET @source_author_id = (SELECT id FROM users WHERE email = 'official-source@local.invalid');
SET @academic_category_id = (SELECT id FROM categories WHERE name = '학사');
SET @course_category_id = (SELECT id FROM categories WHERE name = '교과목');
SET @graduation_category_id = (SELECT id FROM categories WHERE name = '졸업요건');
SET @project_category_id = (SELECT id FROM categories WHERE name = '프로젝트');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @project_category_id, @source_author_id,
       '세종대학교 소프트웨어학과 안내',
       '세종대학교 소프트웨어학과는 2025학년도부터 VR·AR 등 첨단 콘텐츠 기술을 강화한 콘텐츠소프트웨어학과로 개편되었다. 소프트웨어 기초, 문제 해결 능력, VR·AR 전문지식과 AI 응용 역량을 갖춘 인재 양성을 목표로 한다. 주요 진로는 소프트웨어 개발, 보안, 빅데이터, IT 컨설팅, 데이터베이스 및 시스템 관리, 교육·연구 분야다.\n\n행정실: 대양AI센터 401호\n전화: 02-3408-3667\n이메일: softwaredpt@sejong.ac.kr\n\n공식 출처: https://www.sejong.ac.kr/kor/college/software.do\n확인 기준일: 2026-07-29',
       '소프트웨어학과의 교육 방향, 진로, 행정실 연락처를 정리한 공식 안내다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '세종대학교 소프트웨어학과 안내');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @graduation_category_id, @source_author_id,
       '2026 소프트웨어학과 졸업 이수학점 안내',
       '2026학년도 수강편람에 표시된 소프트웨어학과 기준은 졸업학점 130학점, 교양필수 14학점, 교양선택 지정과목 21학점, 학문기초교양 9학점, 전공 72학점이다. 전공 72학점은 전공필수 36학점과 전공선택 36학점으로 구성된다.\n\n입학연도, 복수전공 여부, 교과과정 개편에 따라 개인별 기준이 다를 수 있다. 졸업 전 반드시 본인 입학연도 수강편람과 학사정보시스템의 졸업자가진단을 함께 확인해야 한다. 영어·고전독서·소프트웨어코딩 등 졸업인증제 적용 여부도 개인별로 확인한다.\n\n공식 출처: https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=862579&attachNo=231858&mode=download\n관련 수강편람: https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=862579&attachNo=232292&mode=download\n확인 기준일: 2026-07-29',
       '2026 수강편람 기준 소프트웨어학과 졸업·교양·전공 이수학점과 확인 시 주의사항이다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '2026 소프트웨어학과 졸업 이수학점 안내');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @course_category_id, @source_author_id,
       '세종대학교 수강신청 핵심 안내',
       '수강신청 전 본인의 전공·교양 이수현황과 졸업요건을 확인한다. 특히 3·4학년은 남은 전공필수와 졸업요건을 점검해야 한다. 수강신청 일정과 세부 절차는 매 학기 학사공지와 수강편람을 기준으로 한다.\n\n일부 과목은 수강신청 학점 제한에서 제외될 수 있으며, 폐강·철회·학점이월 기준도 학기별 수강편람에서 확인한다. 소프트웨어학과 전공 과목의 별도 제한이나 문의 사항은 학과 행정실(02-3408-3667)에 확인한다.\n\n공식 출처: https://www.sejong.ac.kr/kor/academics/register-for-class.do\n2026-1 수강편람: https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=862579&attachNo=232292&mode=download\n확인 기준일: 2026-07-29',
       '수강신청 전 점검사항과 공식 일정·수강편람 확인 경로를 정리했다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '세종대학교 수강신청 핵심 안내');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @academic_category_id, @source_author_id,
       '세종대학교 등록금 납부와 등록 안내',
       '1학기 등록은 보통 2월 중순 이후, 2학기 등록은 8월 중순 이후 진행되며 정확한 기간은 매 학기 공지에서 확인한다. 등록금 납부와 수강신청을 모두 완료해야 해당 학기 등록이 완료된다. 전액 장학생도 0원 등록 처리를 해야 한다.\n\n등록금 고지서는 세종대학교 학사정보시스템에서 출력한다. 정규학기 초과자는 수강신청 후 수강학점에 따라 등록금이 생성된다. 등록 관련 문의는 학사지원과(02-3408-3038, reg@sejong.ac.kr)로 한다.\n\n공식 출처: https://www.sejong.ac.kr/kor/academics/registration.do\n확인 기준일: 2026-07-29',
       '학기 등록 시기, 0원 등록, 초과학기 등록금과 문의처를 정리했다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '세종대학교 등록금 납부와 등록 안내');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @academic_category_id, @source_author_id,
       '세종대학교 재학생 장학금 신청 기본 안내',
       '장학금은 정해진 기간에 신청해야 하는 유형과 학교가 대상자를 자체 선발하는 유형으로 나뉜다. 국가장학금, 국가근로, 성적향상, 어학우수, 특기, 국가고시, 보훈, 교외재단 장학금 등은 공지에 따라 별도 신청이 필요할 수 있다. 성적우수, 에델바이스Ⅱ 등 일부 장학금은 학교가 대상자를 선발한다.\n\n일반적으로 수업연한 내 정규학기 재학생이어야 하며 휴학·자퇴·수료·초과학기 여부 등에 따라 제한될 수 있다. 장학금별 세부 기준과 신청 기간은 장학 공지사항을 우선한다. 문의: 학생지원과 student@sejong.ac.kr.\n\n공식 출처: https://www.sejong.ac.kr/kor/academics/scholarship-keyinformation.do\n교내장학금: https://www.sejong.ac.kr/kor/academics/internal-scholarship.do\n확인 기준일: 2026-07-29',
       '재학생 장학금의 신청 필요 여부, 기본 자격과 공식 확인 경로를 정리했다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '세종대학교 재학생 장학금 신청 기본 안내');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @academic_category_id, @source_author_id,
       '2026학년도 주요 학사일정',
       '2026학년도 1학기 주요 일정은 다음과 같다. 휴·복학 신청은 1월 26일부터 2월 1일, 수강신청은 2월 10일부터 13일, 등록은 2월 23일부터 26일, 개강은 3월 3일이다. 수강신청 과목 확인·변경은 3월 4일부터 9일, 수강 철회는 3월 25일부터 27일, 중간고사는 4월 21일부터 27일이다. 하계 계절학기 수강신청은 6월 1일부터 4일이다.\n\n일정은 학교 사정에 따라 바뀔 수 있으므로 신청 직전 공식 학사일정과 학사공지를 다시 확인한다.\n\n공식 출처: https://www.sejong.ac.kr/kor/academics/academic-calendar.do\n확인 기준일: 2026-07-29',
       '2026학년도 1학기 수강신청, 등록, 개강, 변경·철회와 시험 일정을 정리했다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '2026학년도 주요 학사일정');
