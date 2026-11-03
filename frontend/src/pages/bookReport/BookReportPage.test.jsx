import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import BookReportPage from './BookReportPage';

jest.mock('../../components/layout/V3SiteHeader', () => () => <div data-testid="header" />);
jest.mock('../../config/api', () => ({ API_BASE: 'https://api.example.com/api' }));

let mockSession = { authenticated: true, role: 'ROLE_BUYER' };
jest.mock('../../contexts/SessionContext', () => ({
  useSession: () => ({ session: mockSession }),
}));

describe('BookReportPage', () => {
  beforeEach(() => {
    mockSession = { authenticated: true, role: 'ROLE_BUYER' };
  });

  test('does not show manual generation controls to buyers', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue({ status: 404, ok: false });

    render(<BookReportPage />);

    expect(await screen.findByTestId('header')).toBeInTheDocument();
    await waitFor(() => expect(document.querySelector('.book-report-state--empty')).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: '리포트 생성' })).not.toBeInTheDocument();
  });

  test('lets an admin start a manual report job and shows its status', async () => {
    mockSession = { authenticated: true, role: 'ROLE_ADMIN' };
    jest.spyOn(global, 'fetch')
      .mockResolvedValueOnce({ status: 404, ok: false })
      .mockResolvedValueOnce({
        status: 202,
        ok: true,
        json: async () => ({
          jobId: 'job-1',
          category: 'COMPUTER_SOFTWARE',
          status: 'QUEUED',
        }),
      });

    render(<BookReportPage />);

    expect(await screen.findByRole('heading', { name: '관리자 수동 생성' })).toBeInTheDocument();
    await waitFor(() => expect(document.querySelector('.book-report-state--empty')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: '리포트 생성' }));

    expect(await screen.findByText('대기 중')).toBeInTheDocument();
    expect(global.fetch).toHaveBeenLastCalledWith(
      'https://api.example.com/api/admin/book-reports/generate',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ category: 'COMPUTER_SOFTWARE' }),
      }),
    );
  });

  test('polls a manual job until completion and refreshes the report', async () => {
    jest.useFakeTimers();
    mockSession = { authenticated: true, role: 'ROLE_ADMIN' };
    jest.spyOn(global, 'fetch')
      .mockResolvedValueOnce({ status: 404, ok: false })
      .mockResolvedValueOnce({
        status: 202,
        ok: true,
        json: async () => ({
          jobId: 'job-2',
          category: 'COMPUTER_SOFTWARE',
          status: 'QUEUED',
        }),
      })
      .mockResolvedValueOnce({
        status: 200,
        ok: true,
        json: async () => ({
          jobId: 'job-2',
          category: 'COMPUTER_SOFTWARE',
          status: 'COMPLETED',
          reportDate: '2026-11-03',
        }),
      })
      .mockResolvedValueOnce({
        status: 200,
        ok: true,
        json: async () => ({
          reportDate: '2026-11-03',
          categoryName: '갱신된 리포트',
          status: 'READY',
          source: { fetchedAt: '2026-11-03T07:10:00', keywordConfigVersion: 'test' },
          sections: [],
          notice: '테스트 안내',
        }),
      });

    render(<BookReportPage />);
    await waitFor(() => expect(document.querySelector('.book-report-state--empty')).toBeInTheDocument());
    fireEvent.click(await screen.findByRole('button', { name: '리포트 생성' }));
    expect(await screen.findByText('대기 중')).toBeInTheDocument();

    await act(async () => {
      jest.advanceTimersByTime(1200);
    });
    jest.useRealTimers();

    expect(screen.getByText('생성 완료')).toBeInTheDocument();
    expect(await screen.findByText('갱신된 리포트')).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith(
      'https://api.example.com/api/admin/book-reports/jobs/job-2',
      expect.objectContaining({ credentials: 'include' }),
    );
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  test('shows an empty state when no report has been generated', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue({ status: 404, ok: false });

    render(<BookReportPage />);

    expect(screen.getByTestId('header')).toBeInTheDocument();
    expect(screen.getByRole('main')).toHaveClass('v3-unified-page-card');
    expect(screen.getByRole('heading', { name: '학과별 도서 리포트' })).toBeInTheDocument();
    expect(await screen.findByText('아직 생성된 도서 리포트가 없습니다.')).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith(
      'https://api.example.com/api/book-reports/latest?category=COMPUTER_SOFTWARE',
      expect.objectContaining({ credentials: 'include' }),
    );
  });

  test('always displays the source limitation from a generated report', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue({
      status: 200,
      ok: true,
      json: async () => ({
        reportDate: '2026-10-27',
        categoryName: '컴퓨터소프트웨어공학과',
        status: 'READY',
        source: {
          provider: 'KAKAO_DAUM_BOOK_SEARCH',
          fetchedAt: '2026-10-27T04:00:00',
          keywordConfigVersion: '2026-10-27',
        },
        sections: [
          { type: 'NEWLY_DISCOVERED', title: '오늘 새로 발견된 도서', books: [] },
          { type: 'RECENTLY_PUBLISHED', title: '최근 출판 주목 도서', books: [] },
        ],
        notice: '판매량이나 베스트셀러 순위를 의미하지 않습니다.',
      }),
    });

    render(<BookReportPage />);

    expect(await screen.findByText('판매량이나 베스트셀러 순위를 의미하지 않습니다.')).toBeInTheDocument();
    expect(screen.getByText(/출처: 카카오 Daum 책 검색/)).toBeInTheDocument();
  });
});
