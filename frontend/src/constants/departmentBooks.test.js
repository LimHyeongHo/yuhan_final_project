import { PRODUCT_CATEGORIES } from './productCategories';
import { getDepartmentBooks } from './departmentBooks';

test('a department without hardcoded books returns an empty list', () => {
  expect(getDepartmentBooks('GENERAL')).toEqual([]);
  expect(getDepartmentBooks('UNKNOWN_CATEGORY')).toEqual([]);
});

test('a department book includes its grade, curriculum course, and title', () => {
  expect(getDepartmentBooks('COMPUTER_SOFTWARE')[0]).toEqual({
    grade: 1,
    course: 'C언어기초',
    title: '혼자 공부하는 C 언어',
  });
});

test('every selectable department has curriculum-based book recommendations', () => {
  PRODUCT_CATEGORIES
    .filter(({ code }) => code !== 'GENERAL')
    .forEach(({ code }) => {
      expect(getDepartmentBooks(code).length).toBeGreaterThan(0);
      getDepartmentBooks(code).forEach((book) => {
        expect(book).toEqual(expect.objectContaining({
          grade: expect.any(Number),
          course: expect.any(String),
          title: expect.any(String),
        }));
      });
    });
});

test('every configured grade has exactly two distinct book recommendations', () => {
  PRODUCT_CATEGORIES
    .filter(({ code }) => code !== 'GENERAL')
    .forEach(({ code }) => {
      const booksByGrade = getDepartmentBooks(code).reduce((groups, book) => {
        const books = groups.get(book.grade) || [];
        books.push(book);
        groups.set(book.grade, books);
        return groups;
      }, new Map());

      booksByGrade.forEach((books) => {
        expect(books).toHaveLength(2);
        expect(new Set(books.map(({ title }) => title)).size).toBe(2);
      });
    });
});
