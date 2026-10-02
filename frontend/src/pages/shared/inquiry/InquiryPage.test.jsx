import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import InquiryPage from './InquiryPage';
import { useSession } from '../../../contexts/SessionContext';

jest.mock('../../../contexts/SessionContext', () => ({
  useSession: jest.fn(),
}));

jest.mock('../../home/homePage_v3', () => ({
  IntegratedHeader: () => <div data-testid="integrated-header" />,
}));

const inquiry = {
  id: 1,
  title: '결제 관련 문의',
  secret: false,
  answered: false,
  notice: false,
  authorNickname: '구매자',
  authorRole: 'ROLE_BUYER',
  commentCount: 0,
  createdAt: '2026-10-02T10:00:00',
};

beforeEach(() => {
  jest.clearAllMocks();
  global.fetch = jest.fn().mockResolvedValue({
    ok: true,
    json: async () => [inquiry],
  });
});

test('buyer can see the inquiry list and writing button without admin controls', async () => {
  useSession.mockReturnValue({ session: { authenticated: true, role: 'ROLE_BUYER' } });

  render(<InquiryPage />);

  expect(await screen.findByText('결제 관련 문의')).toBeInTheDocument();
  expect(screen.getByText('답변 미완료')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /문의 작성/ })).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: '판매자만' })).not.toBeInTheDocument();
});

test('admin can filter inquiries by seller or buyer', async () => {
  useSession.mockReturnValue({ session: { authenticated: true, role: 'ROLE_ADMIN' } });

  render(<InquiryPage />);
  await screen.findByText('결제 관련 문의');
  fireEvent.click(screen.getByRole('button', { name: '판매자만' }));

  await waitFor(() => {
    expect(global.fetch).toHaveBeenLastCalledWith(
      expect.stringContaining('authorType=SELLER'),
      expect.objectContaining({ credentials: 'include' }),
    );
  });
});

test('admin can open the global notice form', async () => {
  useSession.mockReturnValue({ session: { authenticated: true, role: 'ROLE_ADMIN' } });

  render(<InquiryPage />);
  await screen.findByText('결제 관련 문의');
  fireEvent.click(screen.getByRole('button', { name: /전체 공지 작성/ }));

  expect(screen.getByRole('heading', { name: '전체 공지 작성' })).toBeInTheDocument();
  expect(screen.getByRole('checkbox', { name: /전체 공지로 등록/ })).toBeChecked();
  expect(screen.queryByText('비밀글로 작성')).not.toBeInTheDocument();
});

test('global notice uses a notice badge instead of an answer status', async () => {
  useSession.mockReturnValue({ session: { authenticated: true, role: 'ROLE_BUYER' } });
  global.fetch.mockResolvedValue({
    ok: true,
    json: async () => [{
      ...inquiry,
      id: 2,
      title: '시스템 점검 안내',
      authorRole: 'ROLE_ADMIN',
      authorNickname: '관리자',
      answered: true,
      notice: true,
    }],
  });

  render(<InquiryPage />);

  expect(await screen.findByText('시스템 점검 안내')).toBeInTheDocument();
  expect(screen.getByText('공지')).toBeInTheDocument();
  expect(screen.queryByText('답변 완료')).not.toBeInTheDocument();
});
