import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PrivateRoute from './PrivateRoute';
import { SessionProvider } from '../contexts/SessionContext';

const renderRoute = (session, allowedRoles) => {
  global.fetch = jest.fn().mockResolvedValue({
    ok: session.authenticated,
    json: async () => session,
  });

  render(
    <MemoryRouter>
      <SessionProvider>
        <PrivateRoute allowedRoles={allowedRoles}>
          <div>보호된 화면</div>
        </PrivateRoute>
      </SessionProvider>
    </MemoryRouter>
  );
};

afterEach(() => {
  jest.restoreAllMocks();
});

test('blocks an unauthenticated user', async () => {
  renderRoute({ authenticated: false }, ['ROLE_SELLER']);

  expect(await screen.findByText('로그인이 필요한 서비스입니다')).toBeInTheDocument();
  expect(screen.queryByText('보호된 화면')).not.toBeInTheDocument();
});

test('blocks a logged-in user with the wrong server role', async () => {
  renderRoute({ authenticated: true, role: 'ROLE_BUYER' }, ['ROLE_SELLER']);

  expect(await screen.findByText('접근 권한이 없는 페이지입니다')).toBeInTheDocument();
  expect(screen.queryByText('보호된 화면')).not.toBeInTheDocument();
});

test('renders the page for an allowed server role', async () => {
  renderRoute({ authenticated: true, role: 'ROLE_SELLER' }, ['ROLE_SELLER']);

  expect(await screen.findByText('보호된 화면')).toBeInTheDocument();
});
