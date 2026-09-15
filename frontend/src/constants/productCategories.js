export const PRODUCT_CATEGORIES = [
  { code: 'MECHANICAL_ENGINEERING', name: '기계공학전공' },
  { code: 'FIRE_SAFETY', name: '소방설비안전전공' },
  { code: 'ELECTRICAL_ENGINEERING', name: '전기공학전공' },
  { code: 'ELECTRONICS_ENGINEERING', name: '전자공학전공' },
  { code: 'COMPUTER_SOFTWARE', name: '컴퓨터소프트웨어전공' },
  { code: 'COMPUTER_INFORMATION_COMMUNICATION', name: '컴퓨터정보통신전공' },
  { code: 'GAME_CONTENTS', name: '게임콘텐츠전공' },
  { code: 'ARTIFICIAL_INTELLIGENCE', name: '인공지능전공' },
  { code: 'INDUSTRIAL_DESIGN', name: '산업디자인전공' },
  { code: 'VISUAL_DESIGN', name: '시각디자인전공' },
  { code: 'FASHION_DESIGN', name: '패션디자인전공' },
  { code: 'INTERIOR_ARCHITECTURE', name: '실내건축전공' },
  { code: 'ADVERTISING_MEDIA', name: '광고미디어전공' },
  { code: 'BROADCAST_VIDEO', name: '방송영상전공' },
  { code: 'ANIMATION_WEBTOON', name: '애니메이션웹툰전공' },
  { code: 'BROADCAST_CREATIVE_WRITING', name: '방송문예창작전공' },
  { code: 'BROADCAST_ENTERTAINMENT', name: '방송연예전공' },
  { code: 'FOOD_NUTRITION', name: '식품영양학과' },
  { code: 'HEALTHCARE_ADMINISTRATION', name: '보건의료행정학과' },
  { code: 'OCCUPATIONAL_THERAPY', name: '작업치료과' },
  { code: 'ANIMAL_HEALTH', name: '반려동물보건학과' },
  { code: 'EMERGENCY_MEDICAL_SERVICES', name: '응급구조과' },
  { code: 'DENTAL_HYGIENE', name: '치위생과' },
  { code: 'YUHAN_BIOPHARMACEUTICAL', name: '유한바이오제약전공' },
  { code: 'YUHAN_BIOCHEMICAL', name: '유한생명화공전공' },
  { code: 'SKIN_MAKEUP', name: '피부메이크업전공' },
  { code: 'BEAUTY_COSMETICS', name: '뷰티화장품전공' },
  { code: 'SOCIAL_WELFARE', name: '사회복지전공' },
  { code: 'SPORTS_REHABILITATION', name: '스포츠재활전공' },
  { code: 'PET_INDUSTRY', name: '반려동물산업전공' },
  { code: 'HOTEL_CULINARY', name: '호텔조리전공' },
  { code: 'CAFE_BAKERY', name: '카페베이커리전공' },
  { code: 'HOTEL_TOURISM', name: '호텔관광전공' },
  { code: 'AVIATION_SERVICE', name: '항공서비스학과' },
  { code: 'JAPANESE_BUSINESS', name: '일본비즈니스전공' },
  { code: 'MANAGEMENT_INFORMATION', name: '경영정보전공' },
  { code: 'TAX_ACCOUNTING', name: '세무회계전공' },
  { code: 'FREE_MAJOR', name: '자유전공학과' },
  { code: 'GENERAL', name: '기타 교재' },
];

const CATEGORY_NAMES = Object.fromEntries(
  PRODUCT_CATEGORIES.map(({ code, name }) => [code, name]),
);

export const normalizeProductCategory = (category) => (
  CATEGORY_NAMES[category] ? category : 'GENERAL'
);

export const getProductCategoryName = (category) => (
  CATEGORY_NAMES[normalizeProductCategory(category)]
);
