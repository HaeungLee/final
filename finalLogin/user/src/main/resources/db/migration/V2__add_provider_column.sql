-- 기존 회원 테이블에 provider 컬럼 추가
ALTER TABLE member ADD COLUMN provider VARCHAR(255) NOT NULL DEFAULT 'LOCAL';

-- 기존 데이터를 LOCAL로 설정 (일반 로그인 사용자)
UPDATE member SET provider = 'LOCAL' WHERE provider IS NULL;
