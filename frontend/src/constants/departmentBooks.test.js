import { getDepartmentBooks } from './departmentBooks';

test('a department without hardcoded books returns an empty list', () => {
  expect(getDepartmentBooks('GENERAL')).toEqual([]);
  expect(getDepartmentBooks('UNKNOWN_CATEGORY')).toEqual([]);
});

test('a department book includes a title and display description', () => {
  expect(getDepartmentBooks('COMPUTER_SOFTWARE')[0]).toEqual({
    title: '혼자 공부하는 파이썬',
    description: '파이썬 전공교재',
  });
});
