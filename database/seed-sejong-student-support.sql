USE uniwiki_ai;

INSERT INTO users (email, password, nickname, role)
VALUES ('official-source@local.invalid', SHA2(UUID(), 256), '세종대 공식자료', 'USER')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

INSERT INTO categories (name, description)
SELECT '장학·지원', '국가·교내·교외 장학금과 학생 지원 제도'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '장학·지원');
INSERT INTO categories (name, description)
SELECT '진로·취업', '진로상담, 취업 프로그램, 채용 및 직무 정보'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '진로·취업');
INSERT INTO categories (name, description)
SELECT '현장실습', '학점연계 현장실습과 인턴십 정보'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '현장실습');

SET @source_author_id = (SELECT id FROM users WHERE email = 'official-source@local.invalid');
SET @scholarship_category_id = (SELECT id FROM categories WHERE name = '장학·지원');
SET @career_category_id = (SELECT id FROM categories WHERE name = '진로·취업');
SET @field_category_id = (SELECT id FROM categories WHERE name = '현장실습');
SET @certification_category_id = (SELECT id FROM categories WHERE name = '인증제도');
SET @project_category_id = (SELECT id FROM categories WHERE name = '프로젝트');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @scholarship_category_id, @source_author_id,
       '세종대학교 장학금 신청과 중복수혜 기본 원칙',
       '장학금은 정규 수업연한 내 정규학기를 등록한 재학생을 기본 대상으로 하며 장학금별 자격과 신청기간을 확인해야 한다. 국가장학금, 국가근로, 성적향상, 어학우수, 특기, 국가고시 및 교외재단 장학금 등은 공지에 따라 학생이 직접 신청한다. 성적우수, 에델바이스Ⅱ, 학생회관복지 및 일부 봉사장학 등은 학교가 대상자를 자체 선발할 수 있다.\n\n학비감면 장학금은 수업료 범위 내에서만 중복수혜할 수 있다. 생활비, 근로, 포상 성격의 학업보조비는 수업료 범위와 관계없이 중복수혜할 수 있으며 교외재단 장학금은 해당 기관 원칙을 따른다. 휴학·자퇴·제적, 수업료 초과수혜 또는 중복지원 확인 시 반환이 발생할 수 있다.\n\n장학공지: https://www.sejong.ac.kr/kor/intro/notice7.do\n공식 기준: https://www.sejong.ac.kr/kor/academics/scholarship-keyinformation.do\n문의: 학생지원과 02-3408-4355(국가장학), 3056(국가근로·교외재단), 3054(교내장학)\n확인 기준일: 2026-07-31',
       '장학금별 신청 필요 여부, 지급 방식, 중복수혜와 반환의 공통 원칙이다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '세종대학교 장학금 신청과 중복수혜 기본 원칙');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @career_category_id, @source_author_id,
       '대학일자리플러스센터 진로·취업 지원',
       '대학일자리플러스센터는 저학년 진로탐색과 고학년 직무역량·취업기술 향상을 지원한다. 1:1 진로상담, 입사지원서 컨설팅, 면접 클리닉, 취업박람회, 기업·직무 간담회, 동문 멘토링, 직무적성검사 특강, 취업동아리, 추천채용 및 취업정보를 제공한다.\n\n재학생맞춤형고용서비스는 저학년 대상 빌드업 프로젝트와 고학년 대상 점프업 프로젝트로 운영된다. 프로그램 신청과 개인 상담은 학생경력개발시스템에서 확인한다.\n\n학생경력개발시스템: https://udream.sejong.ac.kr/\n취업공지: https://www.sejong.ac.kr/kor/intro/notice6.do\n문의: 대학일자리플러스센터 학생회관 305호, 02-3408-4153\n공식 출처: https://www.sejong.ac.kr/kor/unilife/career.do\n확인 기준일: 2026-07-31',
       '진로상담, 입사지원서·면접 컨설팅, 취업 프로그램과 학생경력개발시스템 이용 안내다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '대학일자리플러스센터 진로·취업 지원');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @field_category_id, @source_author_id,
       '세종대학교 학점연계 현장실습 기본 안내',
       '현장실습은 학교와 협약을 체결한 기업에서만 가능하며 학생이 개인적으로 섭외한 기업, 기존 근로계약 관계 기업 또는 4촌 이내 친족 기업은 인정되지 않는다. 2026 하계 공지 기준 4주 이상은 전공선택 현장실습 3학점, 8주 이상은 6학점으로 운영되며 재학 중 현장실습과 창의학기제를 합쳐 최대 18학점까지 인정한다.\n\n현장실습지원센터가 일괄 수강신청하며 성적은 P/NP다. 기업 평가, 학과장 평가와 출결을 반영하고 결석 또는 평가서류 미제출 시 NP가 될 수 있다. 지원자격, 학점, 기간은 학기마다 달라질 수 있으므로 최신 모집공지를 우선한다.\n\n현장실습 공지: https://www.sejong.ac.kr/kor/intro/notice6.do\n공식 안내 예시: https://www.sejong.ac.kr/kor/intro/notice6.do?articleNo=866920&mode=view\n문의: 현장실습지원센터 학생회관 308호, 02-3408-4452\n확인 기준일: 2026-07-31',
       '현장실습 인정 기업, 학점, 수강신청, 평가와 신청 시 주의사항의 공통 기준이다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = '세종대학교 학점연계 현장실습 기본 안내');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @field_category_id, @source_author_id,
       'ICT 학점연계 프로젝트 인턴십 안내',
       'ICT 학점연계 프로젝트 인턴십은 정보통신 관련 학과의 전공·복수전공·부전공 재학생 중 인턴십 학점 이수가 가능한 학생을 대상으로 한다. 2026 하반기 국내과정 공지 기준 신청일에 교육과정 50% 이상을 이수하고 실습기간 동안 재학 상태와 학점이수 가능 상태를 유지해야 한다. 소프트웨어 개발·구현, 하드웨어 설계·개발, 정보통신 서비스 등의 프로젝트 직무에 지원할 수 있다.\n\n지원은 ICT 인턴십 공식 홈페이지에서 진행하며 회차별 자격, 기업, 서류, 기간 및 지원금이 달라질 수 있다.\n\nICT 인턴십: https://www.ictintern.or.kr\n공식 출처: https://www.sejong.ac.kr/kor/intro/notice6.do?articleNo=886745&mode=view\n문의: 현장실습지원센터 02-3408-4452\n확인 기준일: 2026-07-31',
       'IT계열 학생이 지원할 수 있는 ICT 학점연계 인턴십의 기본 자격과 지원 경로다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = 'ICT 학점연계 프로젝트 인턴십 안내');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @certification_category_id, @source_author_id,
       'TOSC 시험 접수와 결과 확인',
       'TOSC는 세종대학교 산학협력단 SW역량평가인증원이 운영하는 SW코딩역량평가로 C 또는 Python을 선택해 응시한다. 시험 접수기간, 시험일과 고사장은 회차별 공지에서 확인한다. 세종대 학내 구성원은 신분에 따라 학번 또는 사번으로 로그인하며 시험 결과는 집현캠퍼스에서 확인한다.\n\n콘텐츠소프트웨어학과를 포함한 SW 전공자의 졸업인증 기준은 TOSC 3급 이상 또는 고급C프로그래밍및실습 B0 이상이다. 시험 운영과 졸업인증 판정은 별도이므로 졸업자가진단도 함께 확인한다.\n\nTOSC 공식 사이트: https://tosc.sejong.ac.kr/\n공지: https://tosc.sejong.ac.kr/ko/cusomter_support/notice\n문의: 02-3409-9883\n확인 기준일: 2026-07-31',
       'TOSC 시험 언어, 접수·결과 확인 경로와 콘텐츠소프트웨어학과 졸업인증 기준이다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = 'TOSC 시험 접수와 결과 확인');

INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)
SELECT @project_category_id, @source_author_id,
       'SW중심대학사업단 학생 프로그램 안내',
       'SW중심대학사업단은 TOSC·TOPCIT 단체평가, SW·AI 해커톤, 창의 SW 기초설계 경진대회, 생성형AI 융합 콘텐츠 공모전, 프리인턴십, 기업인턴십 성과공유회, 최신기술 콜로키움, 창업 프로그램 및 해외연수를 운영한다. 프로그램별 대상, 모집기간, 제출물과 지원내용은 회차마다 다르므로 공식 공지에서 확인한다.\n\n사업단 홈페이지: https://sw.sejong.ac.kr/sw/index.do\n공지사항: https://sw.sejong.ac.kr/sw/notice.do\n문의: sw_sejong@sejong.ac.kr\n확인 기준일: 2026-07-31',
       '소프트웨어 전공 학생이 참여할 수 있는 평가, 경진대회, 인턴십, 특강, 창업 및 해외연수 프로그램이다.',
       'APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = 'SW중심대학사업단 학생 프로그램 안내');
