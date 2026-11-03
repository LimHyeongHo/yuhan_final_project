import { render, screen, waitFor } from '@testing-library/react';
import { SessionProvider, useSession } from './SessionContext';

const SessionView = ({ label }) => {
  const { session, loading } = useSession();
  return <div>{loading ? '확인 중' : `${label}:${session?.role || 'ANONYMOUS'}`}</div>;
};

afterEach(() => {
  jest.restoreAllMocks();
});

test('keeps the verified session when children change', async () => {
  global.fetch = jest.fn().mockResolvedValue({
    ok: true,
    json: async () => ({ authenticated: true, role: 'ROLE_SELLER', nickname: '판매자' }),
  });

  const { rerender } = render(
    <SessionProvider>
      <SessionView label="첫 화면" />
    </SessionProvider>
  );

  expect(await screen.findByText('첫 화면:ROLE_SELLER')).toBeInTheDocument();

  rerender(
    <SessionProvider>
      <SessionView label="다음 화면" />
    </SessionProvider>
  );

  expect(screen.getByText('다음 화면:ROLE_SELLER')).toBeInTheDocument();
  await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
});
