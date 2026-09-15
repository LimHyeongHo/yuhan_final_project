import { getDisplayProductImageUrl } from './productImageUrl';

describe('getDisplayProductImageUrl', () => {
  test('uses the larger legacy Aladin cover', () => {
    expect(getDisplayProductImageUrl(
      'https://image.aladin.co.kr/product/1/2/coversum/book.jpg'
    )).toBe('https://image.aladin.co.kr/product/1/2/cover500/book.jpg');
  });

  test('requests a larger Kakao thumbnail', () => {
    expect(getDisplayProductImageUrl(
      'https://search1.kakaocdn.net/thumb/R120x174.q85/?fname=cover.jpg'
    )).toBe('https://search1.kakaocdn.net/thumb/R500x0.q85/?fname=cover.jpg');
  });

  test('upgrades a Google thumbnail without changing other parameters', () => {
    expect(getDisplayProductImageUrl(
      'https://books.google.com/cover.jpg?zoom=1&source=gbs_api'
    )).toBe('https://books.google.com/cover.jpg?zoom=3&source=gbs_api');
  });

  test('leaves local and invalid URLs unchanged', () => {
    expect(getDisplayProductImageUrl('http://localhost:8080/uploads/cover.jpg'))
      .toBe('http://localhost:8080/uploads/cover.jpg');
    expect(getDisplayProductImageUrl('/uploads/cover.jpg')).toBe('/uploads/cover.jpg');
  });
});
