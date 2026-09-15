import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Header from './Header';

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

test('ignores a forged localStorage role when the server session is absent', async () => {
  localStorage.setItem('user_nickname', '위조 사용자');
  localStorage.setItem('user_role', 'ROLE_SELLER');
  global.fetch = jest.fn().mockResolvedValue({ ok: false });

  render(
    <MemoryRouter>
      <Header />
    </MemoryRouter>
  );

  await waitFor(() => expect(global.fetch).toHaveBeenCalled());
  expect(screen.getByRole('link', { name: '로그인' })).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: '대시보드' })).not.toBeInTheDocument();
});
