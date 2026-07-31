USE uniwiki_ai;

INSERT INTO categories (name, description)
SELECT '인증제도', '영어, 고전독서, 소프트웨어코딩 등 졸업 인증 정보'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '인증제도');
