import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import GuidePage from './GuidePage';
import { useSession } from '../../contexts/SessionContext';

jest.mock('../../components/layout/V3SiteHeader', () => () => <div>Header</div>);
jest.mock('../../contexts/SessionContext', () => ({
  useSession: jest.fn(),
}));

const renderForRole = (role) => {
  useSession.mockReturnValue({
    session: { authenticated: true, role },
    loading: false,
  });

  return render(
    <MemoryRouter>
      <GuidePage />
    </MemoryRouter>
  );
};

afterEach(() => {
  jest.clearAllMocks();
});

test('구매자에게는 구매자 이용 흐름만 표시한다', () => {
  renderForRole('ROLE_BUYER');

  expect(screen.getByRole('heading', { name: '구매자 이용 방법' })).toBeInTheDocument();
  expect(screen.queryByRole('heading', { name: '판매자 이용 방법' })).not.toBeInTheDocument();
});

test('판매자에게는 판매자 이용 흐름만 표시한다', () => {
  renderForRole('ROLE_SELLER');

  expect(screen.queryByRole('heading', { name: '구매자 이용 방법' })).not.toBeInTheDocument();
  expect(screen.getByRole('heading', { name: '판매자 이용 방법' })).toBeInTheDocument();
});

test('관리자에게는 두 이용 흐름을 모두 표시한다', () => {
  renderForRole('ROLE_ADMIN');

  expect(screen.getByRole('heading', { name: '구매자 이용 방법' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: '판매자 이용 방법' })).toBeInTheDocument();
});

test('가이드 내용이 화면 높이를 강제로 늘리지 않는다', () => {
  renderForRole('ROLE_SELLER');

  expect(screen.getByRole('main')).not.toHaveClass('flex-grow');
});
