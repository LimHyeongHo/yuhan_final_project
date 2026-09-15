// 학과 코드별로 API에서 검색할 책 제목과 화면에 표시할 설명을 입력합니다.
export const DEPARTMENT_BOOKS = {
  COMPUTER_SOFTWARE: [
    {
      title: '혼자 공부하는 파이썬',
      description: '파이썬 전공교재',
    },
    {
      title: '쉽게 배우는 운영체제',
      description: '운영체제 전공교재',
    },
  ],
};

export const getDepartmentBooks = (category) => (
  Array.isArray(DEPARTMENT_BOOKS[category])
    ? DEPARTMENT_BOOKS[category]
    : []
);
