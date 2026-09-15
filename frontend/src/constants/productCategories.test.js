import {
  PRODUCT_CATEGORIES,
  getProductCategoryName,
  normalizeProductCategory,
} from './productCategories';

test('product category codes are unique and include GENERAL', () => {
  const codes = PRODUCT_CATEGORIES.map(({ code }) => code);

  expect(new Set(codes).size).toBe(codes.length);
  expect(codes).toContain('GENERAL');
});

test('unknown legacy categories are treated as GENERAL', () => {
  expect(normalizeProductCategory('컴퓨터/IT')).toBe('GENERAL');
  expect(getProductCategoryName('컴퓨터/IT')).toBe('기타 교재');
});
