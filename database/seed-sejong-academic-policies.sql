USE uniwiki_ai;

INSERT INTO users (email, password, nickname, role)
VALUES ('official-source@local.invalid', SHA2(UUID(), 256), '세종대 공식자료', 'USER')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

INSERT INTO categories (name, description)
SELECT '인증제도', '졸업, 공학교육, 마이크로디그리 등 인증 정보'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '인증제도');

SET @source_author_id = (SELECT id FROM users WHERE email = 'official-source@local.invalid');
SET @certification_category_id = (SELECT id FROM categories WHERE name = '인증제도');
SET @course_category_id = (SELECT id FROM categories WHERE name = '교과목');
SET @academic_category_id = (SELECT id FROM categories WHERE name = '학사');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @certification_category_id, @source_author_id,
       '세종대학교 졸업인증제 적용 기준',
       '2012~2022학년도 입학자는 영어졸업인증과 고전독서졸업인증을 적용한다. 2023학년도 이후 입학자는 영어졸업인증, 고전독서졸업인증, 소프트웨어코딩졸업인증 중 2개 이상을 통과해야 졸업 자격을 취득한다. 한국어 자격을 기준으로 입학한 외국인 학생은 영어졸업인증 대신 한국어졸업인증을 적용한다.\n\n입학전형, 편입, 초과학기, 외국인 여부 및 학과에 따라 면제·대체 조건이 다를 수 있다. 졸업인증 외 학점 요건을 충족해도 인증이 남으면 수료 처리될 수 있으므로 학사정보시스템의 졸업자가진단과 최신 수강편람을 함께 확인해야 한다.\n\n공식 출처: https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=863048&attachNo=232296&mode=download\n졸업자가진단: https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=803472&mode=view\n확인 기준일: 2026-07-31',
       '입학연도별 영어·고전독서·소프트웨어코딩 졸업인증 적용 방식과 확인 시 주의사항이다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '세종대학교 졸업인증제 적용 기준');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @certification_category_id, @source_author_id,
       '영어졸업인증 기준과 대체 이수',
       '2023학년도 이후 일반 전공 입학자의 영어졸업인증 기준은 TOEIC 800점, TOEFL iBT 80점, TEPS 348점, OPIc IM1, TOEIC Speaking IM1, G-TELP Level 2 77점 또는 G-TELP Speaking Level 4 이상이다. 영어영문학 전공자는 더 높은 별도 기준이 적용된다.\n\n학교가 공지한 신청 기간에 공인영어 성적으로 신청한다. 미통과자는 대상 학년과 면제 조건을 충족하는 경우 교양선택 Intensive English 이수로 대체할 수 있다. 이미 영어인증을 받았거나 면제된 학생은 해당 과목을 신청할 수 없다.\n\n문의: 교양영어실(군자관 503호, 02-3408-3971, tas518@sejong.ac.kr)\n공식 출처: https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=863048&attachNo=232296&mode=download\n신청 공지 예시: https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=805930&mode=view\n확인 기준일: 2026-07-31',
       '2023학년도 이후 일반 전공자의 공인영어 기준, 신청 방식과 Intensive English 대체 이수 안내다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '영어졸업인증 기준과 대체 이수');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @certification_category_id, @source_author_id,
       '고전독서졸업인증 기준과 대체 이수',
       '고전독서졸업인증은 2012학년도 이후 입학자가 대상이며 원칙적으로 7학기 시작 전까지 완료한다. 서양의 역사와 사상 4권, 동양의 역사와 사상 2권, 동·서양의 문학 3권, 과학 사상 1권으로 총 10권을 인증해야 한다. 독서당 CBT는 책당 10문항이며 50점 이상이 통과 기준이다.\n\n7학기 시작 전까지 10권을 완료하지 못했더라도 영역별 기준에 맞춰 5권 이상 인증한 4~5학년은 고전특강으로 대체할 수 있다. 고전강독, 독서토론 및 일부 교과 연계 인증도 가능하므로 최신 수강편람과 고전독서인증센터에서 개인별 인정 현황을 확인한다.\n\n공식 출처: https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=863048&attachNo=232296&mode=download\n관련 안내: https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=805959&mode=view\n확인 기준일: 2026-07-31',
       '고전독서 영역별 10권 기준, CBT 통과 기준과 고전특강 대체 이수 조건이다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '고전독서졸업인증 기준과 대체 이수');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @certification_category_id, @source_author_id,
       '소프트웨어코딩졸업인증 기준',
       '소프트웨어코딩졸업인증은 2023학년도 이후 입학자가 대상이다. 콘텐츠소프트웨어학과를 포함한 전공자는 TOSC 3급 이상을 취득하거나 전공필수 고급C프로그래밍및실습(3학점)을 B0 이상 취득하면 인증 조건을 충족한다. TOSC 기준을 충족하면 자동 인증된다.\n\n복수학위생, 군위탁생, 일정 기준 이상의 초과학기 재학생, 수료 후 2학기 경과생 및 외국인 유학생 등은 면제 기준이 있을 수 있다. 개인별 적용 여부는 졸업자가진단과 SW역량평가인증원에서 확인한다.\n\n문의: SW역량평가인증원(광개토관 1014A호, 02-6935-2740)\n공식 출처: https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=863048&attachNo=232296&mode=download\n확인 기준일: 2026-07-31',
       '콘텐츠소프트웨어학과 학생의 TOSC 또는 고급C프로그래밍및실습 성적을 통한 코딩 졸업인증 기준이다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '소프트웨어코딩졸업인증 기준');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @course_category_id, @source_author_id,
       '2024~2026 학기별 강의시간표 확인 안내',
       '2024학년도 1학기부터 2026학년도 2학기까지 세종대학교 공식 한국어 강의시간표 원문을 학기별로 보관한다. 강의시간표에는 개설대학, 개설학과, 학수번호, 분반, 교과목명, 이수구분, 학점, 요일과 강의시간, 강의실, 담당 교수, 주관학과 및 수강 유의사항이 포함된다.\n\n소프트웨어학과는 2025학년도부터 콘텐츠소프트웨어학과로 개편되었으므로 과거 자료 검색 시 두 학과명을 모두 확인해야 한다. 첨부 시간표는 게시 시점 자료이고 시간, 강의실, 교수, 추가 개설 및 폐강이 바뀔 수 있으므로 실제 수강신청 전에는 학사정보시스템을 최종 기준으로 한다.\n\n공식 학사공지: https://www.sejong.ac.kr/kor/intro/notice3.do\n2026-2 안내: https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=891086&mode=view\n확인 기준일: 2026-07-31',
       '2024-1부터 2026-2까지 공식 강의시간표의 포함 정보와 소프트웨어학과 검색 시 주의사항이다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '2024~2026 학기별 강의시간표 확인 안내');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @certification_category_id, @source_author_id,
       '소프트웨어학과 공학교육인증 이수 주의사항',
       '소프트웨어학과 공학교육인증 공지에 따르면 2016학년도 이후 학생은 전입생 등 예외를 제외하고 특별한 사유가 없는 한 공학인증에 참여해야 하며, 인증 요건을 충족하지 않으면 졸업할 수 있다. 학과 졸업요건과 공학인증 요건은 서로 별개이므로 두 기준을 각각 확인해야 한다.\n\n설계교과목은 기초설계, 요소설계, 종합설계 순서로 수강해야 설계학점이 인정된다. 기초설계 없이 요소설계를 듣거나 요소설계 학점을 채우지 않고 종합설계를 들으면 설계학점이 인정되지 않을 수 있다. 요소설계와 종합설계를 함께 수강하다 요소설계에서 F를 받으면 두 과목을 다시 수강해야 할 수도 있다. 2022학번부터 전문교양은 지정과목만 인정된다. 교과목은 본인 입학연도 기준, 최소이수학점은 최신 연도 기준으로 확인한다.\n\n문의: 공학교육센터(02-3408-2982, eerc@sejong.ac.kr)\n공식 출처: https://dept.sejong.ac.kr/softwaredpt/board/notice.do?articleNo=24978&mode=view\n확인 기준일: 2026-07-31',
       '소프트웨어학과 공학인증 대상, 졸업과의 관계, 설계교과목 이수 순서 및 전문교양 인정 기준이다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '소프트웨어학과 공학교육인증 이수 주의사항');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @certification_category_id, @source_author_id,
       'AI 마이크로디그리 이수증 신청 안내',
       'AI 마이크로디그리는 지정 교과목의 이수 내역이 인증 페이지에 반영된 뒤 학생이 직접 이수증을 신청하는 방식이다. 지정 과목을 모두 수강한 학생은 세종대학교 비교과통합지원센터의 마이크로디그리 인증 페이지에서 이수 상태를 확인하고 이수증을 신청한다.\n\n신청 페이지: https://do.sejong.ac.kr/ko/certificate/microdegree/apply\n공식 출처: https://dept.sejong.ac.kr/softwaredpt/board/notice.do?articleNo=24985&mode=view\n확인 기준일: 2026-07-31',
       '지정 과목 이수 후 AI 마이크로디그리 인증 상태를 확인하고 이수증을 신청하는 경로다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = 'AI 마이크로디그리 이수증 신청 안내');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @academic_category_id, @source_author_id,
       '연구실 법정 안전교육과 성적열람 제한',
       '소프트웨어학과 공지에 따르면 2025학년도 2학기부터 연구실 법정 안전교육 미이수자는 정해진 성적열람 기간에 성적 열람이 제한될 수 있다. 연구실 또는 실험·실습 활동 대상자는 학교가 안내하는 기간 내 안전교육 이수 여부를 확인하고 미수료 상태라면 교육을 완료해야 한다. 학과 게시판에는 미수료자와 재교육 희망자를 위한 교육 자료 안내가 제공된다.\n\n학생 명단과 학번은 개인정보이므로 이 문서에 저장하지 않는다. 적용 학기와 제한 기간은 매 학기 최신 공지를 확인한다.\n\n공식 출처: https://dept.sejong.ac.kr/softwaredpt/board/notice.do?articleNo=24987&mode=view\n교육 자료 안내: https://dept.sejong.ac.kr/softwaredpt/board/notice.do?articleNo=24986&mode=view\n확인 기준일: 2026-07-31',
       '연구실 법정 안전교육 미이수 시 성적열람이 제한될 수 있다는 학과 안내와 개인정보 처리 원칙이다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '연구실 법정 안전교육과 성적열람 제한');
