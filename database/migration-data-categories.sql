USE uniwiki_ai;

INSERT INTO categories (name, description)
SELECT '인증제도', '졸업, 공학교육, 마이크로디그리 등 인증 정보'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '인증제도');
