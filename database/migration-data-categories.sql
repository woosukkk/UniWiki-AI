USE uniwiki_ai;

INSERT INTO categories (name, description)
SELECT '인증제도', '졸업, 공학교육, 마이크로디그리 등 인증 정보'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '인증제도');

INSERT INTO categories (name, description)
SELECT '장학·지원', '국가·교내·교외 장학금과 학생 지원 제도'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '장학·지원');

INSERT INTO categories (name, description)
SELECT '진로·취업', '진로상담, 취업 프로그램, 채용 및 직무 정보'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '진로·취업');

INSERT INTO categories (name, description)
SELECT '현장실습', '학점연계 현장실습과 인턴십 정보'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '현장실습');
