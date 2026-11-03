import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import SellerProductsPage from './SellerProductsPage';

jest.mock('../../../components/layout/V3SiteHeader', () => () => <div>Header</div>);

const originalFetch = global.fetch;

afterEach(() => {
  global.fetch = originalFetch;
  jest.restoreAllMocks();
});

test('판매 현황에 도서 이미지를 표시한다', async () => {
  global.fetch = jest.fn().mockResolvedValue({
    json: async () => [{
      productId: 1,
      type: 'BOOK',
      title: '자료구조',
      imageUrl: 'https://image.aladin.co.kr/product/coversum/cover.jpg',
      price: 20000,
      currentCount: 1,
      targetCount: 5,
      status: 'OPEN',
      createdAt: '2026-09-28T12:00:00',
    }],
  });

  render(
    <MemoryRouter>
      <SellerProductsPage />
    </MemoryRouter>
  );

  const image = await screen.findByRole('img', { name: '자료구조 이미지' });

  expect(image).toHaveAttribute(
    'src',
    'https://image.aladin.co.kr/product/cover500/cover.jpg'
  );
});

test('도서 이미지가 없으면 ImageOff 아이콘을 표시한다', async () => {
  global.fetch = jest.fn().mockResolvedValue({
    json: async () => [{
      productId: 2,
      type: 'BOOK',
      title: '알고리즘',
      imageUrl: null,
      price: 18000,
      currentCount: 0,
      targetCount: 5,
      status: 'OPEN',
      createdAt: '2026-09-28T12:00:00',
    }],
  });

  render(
    <MemoryRouter>
      <SellerProductsPage />
    </MemoryRouter>
  );

  expect(await screen.findByLabelText('이미지 없음')).toBeInTheDocument();
  expect(screen.queryByRole('img', { name: '알고리즘 이미지' })).not.toBeInTheDocument();
});
