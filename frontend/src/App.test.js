import { render, screen } from '@testing-library/react';
import App from './App';

jest.mock('./contexts/CertificateTimerContext', () => ({
  CertificateTimerProvider: ({ children }) => children,
  useCertificateTimer: () => ({ remainingSeconds: null }),
}));

jest.mock('./contexts/ChatNotificationContext', () => ({
  ChatNotificationProvider: ({ children }) => children,
  useChatNotifications: () => ({ chatRooms: [], totalChatUnread: 0 }),
}));

beforeEach(() => {
  global.fetch = jest.fn().mockResolvedValue({
    ok: true,
    json: async () => [],
  });
});

afterEach(() => {
  jest.restoreAllMocks();
});

test('renders the N-BBANG home page', async () => {
  render(<App />);

  expect(screen.getAllByRole('link', { name: 'N-BBANG 홈' }).length).toBeGreaterThan(0);
  expect(screen.getByRole('button', { name: '검색' })).toBeInTheDocument();
  expect(await screen.findByText('현재 모집 중인 공동구매가 없습니다.')).toBeInTheDocument();
  expect(global.fetch).toHaveBeenCalledWith('http://localhost:8080/api/products');
});
