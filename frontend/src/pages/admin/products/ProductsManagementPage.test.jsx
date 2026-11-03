import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProductsManagementPage from './ProductsManagementPage';

jest.mock('../../../components/admin/AdminHeader', () => () => <div>Header</div>);

const originalFetch = global.fetch;

afterEach(() => {
  global.fetch = originalFetch;
  jest.restoreAllMocks();
});

test('관리자 상품 목록에서 도서 이미지와 ImageOff 대체 아이콘을 표시한다', async () => {
  global.fetch = jest.fn()
    .mockResolvedValueOnce({
      json: async () => [
        {
          id: 1,
          type: 'BOOK',
          title: '자료구조',
          imageUrl: 'https://image.aladin.co.kr/product/coversum/cover.jpg',
          price: 20000,
          ratio: 20,
          status: 'OPEN',
          suspicious: false,
          seller: 'seller@example.com',
        },
        {
          id: 2,
          type: 'BOOK',
          title: '알고리즘',
          imageUrl: null,
          price: 18000,
          ratio: 0,
          status: 'OPEN',
          suspicious: false,
          seller: 'seller@example.com',
        },
      ],
    })
    .mockResolvedValueOnce({ json: async () => [] });

  render(
    <MemoryRouter>
      <ProductsManagementPage />
    </MemoryRouter>
  );

  expect(await screen.findByRole('img', { name: '자료구조 이미지' })).toHaveAttribute(
    'src',
    'https://image.aladin.co.kr/product/cover500/cover.jpg'
  );
  expect(screen.getByLabelText('알고리즘 이미지 없음')).toBeInTheDocument();
});
