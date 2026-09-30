// 2026학년도 유한대학교 교육과정의 대표 교과목을 기준으로 한 추천 도서입니다.
// 도서명은 해당 수업과 함께 읽기 좋은 검색용 추천이며, 학교 지정 교재가 아닙니다.
const curriculumBook = (grade, course, title) => ({ grade, course, title });

export const DEPARTMENT_BOOKS_CURRICULUM_YEAR = 2026;

export const DEPARTMENT_BOOKS = {
  MECHANICAL_ENGINEERING: [
    curriculumBook(1, '기계공학입문', '기계공학개론'),
    curriculumBook(2, '열역학(2)', '쉽게 배우는 열역학'),
    curriculumBook(3, '자동차공학', '자동차공학'),
  ],
  FIRE_SAFETY: [
    curriculumBook(1, '소방학개론', '소방학개론'),
    curriculumBook(2, '위험물질론', '위험물산업기사 필기'),
    curriculumBook(3, '화재학', '화재공학'),
  ],
  ELECTRICAL_ENGINEERING: [
    curriculumBook(1, '전기자기학', '전기자기학'),
    curriculumBook(2, '전력공학', '전력공학'),
  ],
  ELECTRONICS_ENGINEERING: [
    curriculumBook(1, '회로이론', '회로이론'),
    curriculumBook(2, '반도체공학', '반도체공학'),
  ],
  COMPUTER_SOFTWARE: [
    curriculumBook(1, 'C언어기초', '혼자 공부하는 C 언어'),
    curriculumBook(2, '운영체제', '쉽게 배우는 운영체제'),
    curriculumBook(3, '소프트웨어공학', '소프트웨어 공학의 모든 것'),
  ],
  COMPUTER_INFORMATION_COMMUNICATION: [
    curriculumBook(1, '컴퓨터네트워크', '데이터 통신과 컴퓨터 네트워크'),
    curriculumBook(2, '정보통신', 'TCP IP 소켓 프로그래밍'),
  ],
  GAME_CONTENTS: [
    curriculumBook(1, '게임분석기초', '게임 디자인 레벨업 가이드'),
    curriculumBook(2, '게임엔진응용', '레트로의 유니티 게임 프로그래밍 에센스'),
    curriculumBook(3, '3D게임엔진', '유니티 교과서'),
  ],
  ARTIFICIAL_INTELLIGENCE: [
    curriculumBook(1, '인공지능정보처리', '혼자 공부하는 파이썬'),
    curriculumBook(2, '기계학습(1)', '혼자 공부하는 머신러닝 딥러닝'),
    curriculumBook(3, '생성형AI', '생성형 AI를 위한 프롬프트 엔지니어링'),
  ],
  INDUSTRIAL_DESIGN: [
    curriculumBook(1, '디자인스케치', '디자인 스케치'),
    curriculumBook(2, '리빙제품디자인(1)', '제품디자인'),
    curriculumBook(3, '캡스톤디자인', '디자인 씽킹 바이블'),
  ],
  VISUAL_DESIGN: [
    curriculumBook(1, '타이포그래피', '타이포그래피 교과서'),
    curriculumBook(2, '브랜드패키지디자인', '패키지 디자인'),
    curriculumBook(3, 'UX/UI디자인', 'UX UI의 10가지 심리학 법칙'),
  ],
  FASHION_DESIGN: [
    curriculumBook(1, '의복구성(1)', '의복구성학'),
    curriculumBook(2, '패션상품기획', '패션 머천다이징'),
    curriculumBook(3, 'AI패션디자인실무', '패션 디자인을 위한 AI'),
  ],
  INTERIOR_ARCHITECTURE: [
    curriculumBook(1, '실내건축계획론', '실내건축계획'),
    curriculumBook(2, '기본설계도면', '실내건축제도'),
    curriculumBook(3, '실내공간과AI', 'AI와 공간 디자인'),
  ],
  ADVERTISING_MEDIA: [
    curriculumBook(1, '광고의이해', '광고학개론'),
    curriculumBook(2, '브랜드커뮤니케이션', '브랜드 스토리텔링'),
    curriculumBook(3, '마케팅인사이트분석', '디지털 마케팅 레볼루션'),
  ],
  BROADCAST_VIDEO: [
    curriculumBook(1, '영상촬영기초', '영상 제작의 미학적 원리와 방법'),
    curriculumBook(2, '영상조명심화', '영상조명 기술'),
    curriculumBook(3, '영상연출', '영화 연출'),
  ],
  ANIMATION_WEBTOON: [
    curriculumBook(1, '캐릭터디자인', '캐릭터 디자인'),
    curriculumBook(2, '콘티', '스토리보드의 예술'),
    curriculumBook(3, '웹툰창작', '웹툰 연출'),
  ],
  BROADCAST_CREATIVE_WRITING: [
    curriculumBook(1, '이야기창작과발상', '스토리'),
    curriculumBook(2, '웹소설창작과장르', '웹소설 써서 먹고삽니다'),
    curriculumBook(3, '시나리오작법', '시나리오 어떻게 쓸 것인가'),
  ],
  BROADCAST_ENTERTAINMENT: [
    curriculumBook(1, '액팅테크닉', '배우수업'),
    curriculumBook(2, '카메라연기', '카메라 연기'),
    curriculumBook(3, '드라마워크숍', '연기의 첫걸음'),
  ],
  FOOD_NUTRITION: [
    curriculumBook(1, '영양학', '고급 영양학'),
    curriculumBook(2, '생애주기영양학', '생애주기영양학'),
    curriculumBook(3, '식품가공학및실습', '식품가공학'),
  ],
  HEALTHCARE_ADMINISTRATION: [
    curriculumBook(1, '보건행정학', '보건행정학'),
    curriculumBook(2, '보건의료정보관리학', '보건의료정보관리학'),
    curriculumBook(3, '의무기록정보분석실무', '의무기록정보 분석 실무'),
  ],
  OCCUPATIONAL_THERAPY: [
    curriculumBook(1, '작업치료학개론', '작업치료학개론'),
    curriculumBook(2, '작업치료평가학', '작업치료평가학'),
    curriculumBook(3, '노인작업치료학', '노인작업치료학'),
  ],
  ANIMAL_HEALTH: [
    curriculumBook(1, '동물해부생리학', '동물보건 해부생리학'),
    curriculumBook(2, '동물보건영상학', '동물보건 영상학'),
    curriculumBook(3, '동물공중보건학', '동물공중보건학'),
  ],
  EMERGENCY_MEDICAL_SERVICES: [
    curriculumBook(1, '응급구조학개론', '응급구조학개론'),
    curriculumBook(2, '전문내과응급처치학', '전문응급처치학'),
    curriculumBook(3, '재난관리학', '재난관리론'),
  ],
  DENTAL_HYGIENE: [
    curriculumBook(1, '치위생학개론', '치위생학개론'),
    curriculumBook(2, '구강보건교육학', '구강보건교육학'),
    curriculumBook(3, '공중구강보건학', '공중구강보건학'),
  ],
  YUHAN_BIOPHARMACEUTICAL: [
    curriculumBook(1, '일반화학', '일반화학'),
    curriculumBook(2, '미생물학', '미생물학'),
    curriculumBook(3, '의약품제조', 'GMP 실무'),
  ],
  YUHAN_BIOCHEMICAL: [
    curriculumBook(1, '기초유전자분석', '유전학의 이해'),
    curriculumBook(2, '생화학', '생화학'),
    curriculumBook(3, '화학공정', '화학공학개론'),
  ],
  SKIN_MAKEUP: [
    curriculumBook(1, '피부생명과학', '피부미용학'),
    curriculumBook(2, '메디컬스킨케어', '메디컬 스킨케어'),
  ],
  BEAUTY_COSMETICS: [
    curriculumBook(1, '맞춤형화장품의이해', '화장품학'),
    curriculumBook(2, '화장품성분학과제조실습', '화장품 제조와 품질관리'),
  ],
  SOCIAL_WELFARE: [
    curriculumBook(1, '사회복지학개론', '사회복지학개론'),
    curriculumBook(2, '사회복지실천론', '사회복지실천론'),
    curriculumBook(3, '사회복지정책론', '사회복지정책론'),
  ],
  SPORTS_REHABILITATION: [
    curriculumBook(1, '스포츠해부학', '기능해부학'),
    curriculumBook(2, '노인운동재활', '운동재활'),
    curriculumBook(3, '운동손상및처치', '스포츠 손상과 재활'),
  ],
  PET_INDUSTRY: [
    curriculumBook(1, '반려동물학', '반려동물학'),
    curriculumBook(2, '반려동물해부생리학', '반려동물 해부생리학'),
    curriculumBook(3, '반려동물행동교정및실습', '반려동물 행동학'),
  ],
  HOTEL_CULINARY: [
    curriculumBook(1, '한식조리실습', '한국조리'),
    curriculumBook(2, '식품구매및외식원가관리', '외식 원가관리'),
  ],
  CAFE_BAKERY: [
    curriculumBook(1, '베이킹테크닉', '제과제빵학'),
    curriculumBook(2, '베이커리카페경영관리', '베이커리 카페 창업'),
  ],
  HOTEL_TOURISM: [
    curriculumBook(1, '관광법규', '관광법규'),
    curriculumBook(2, '호텔경영실무', '호텔경영론'),
  ],
  AVIATION_SERVICE: [
    curriculumBook(1, '기내서비스실무(1)', '항공객실서비스실무'),
    curriculumBook(2, '항공서비스마케팅', '항공서비스경영'),
  ],
  JAPANESE_BUSINESS: [
    curriculumBook(1, '일본어문법과작문', '일본어 문법 무작정 따라하기'),
    curriculumBook(2, '통상일본어회화', '비즈니스 일본어 회화 표현사전'),
  ],
  MANAGEMENT_INFORMATION: [
    curriculumBook(1, '경영정보시스템', '경영정보시스템'),
    curriculumBook(2, 'ERP실습(1)', 'ERP 정보관리사'),
  ],
  TAX_ACCOUNTING: [
    curriculumBook(1, '회계원리', '회계원리'),
    curriculumBook(2, '법인세실무', '법인세 실무'),
  ],
  FREE_MAJOR: [
    curriculumBook(1, '전공탐색', '진로설계와 자기계발'),
  ],
};

export const getDepartmentBooks = (category) => (
  Array.isArray(DEPARTMENT_BOOKS[category])
    ? DEPARTMENT_BOOKS[category]
    : []
);
