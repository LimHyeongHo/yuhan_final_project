import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Header from './Header';
import { SessionProvider } from '../../contexts/SessionContext';

jest.mock('../../contexts/CertificateTimerContext', () => ({
  useCertificateTimer: () => ({ remainingSeconds: null }),
}));

jest.mock('../../contexts/ChatNotificationContext', () => ({
  useChatNotifications: () => ({ chatRooms: [] }),
}));

afterEach(() => {
  localStorage.clear();
  jest.restoreAllMocks();
});

const renderHeader = () => render(
  <MemoryRouter>
    <SessionProvider>
      <Header />
    </SessionProvider>
  </MemoryRouter>
);

test('ignores a forged localStorage role when the server session is absent', async () => {
  localStorage.setItem('user_nickname', '위조 사용자');
  localStorage.setItem('user_role', 'ROLE_SELLER');
  global.fetch = jest.fn().mockResolvedValue({ ok: false });

  renderHeader();

  expect(await screen.findByRole('link', { name: '로그인' })).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: '대시보드' })).not.toBeInTheDocument();
});

test.each([
  ['ROLE_ADMIN', '관리자', '관리자 홈'],
  ['ROLE_SELLER', '판매자', '대시보드'],
  ['ROLE_BUYER', '구매자', '공구 찾기'],
])('renders the correct header for %s', async (role, nickname, expectedLink) => {
  global.fetch = jest.fn((url) => {
    if (String(url).endsWith('/api/member/session')) {
      return Promise.resolve({
        ok: true,
        json: async () => ({ authenticated: true, role, nickname }),
      });
    }
    return Promise.resolve({ ok: true, json: async () => [] });
  });

  renderHeader();

  expect(await screen.findByRole('link', { name: expectedLink })).toBeInTheDocument();
  expect(screen.getByText(`${nickname} 님`)).toBeInTheDocument();
});
