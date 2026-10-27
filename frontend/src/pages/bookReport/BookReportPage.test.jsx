import React from 'react';
import { render, screen } from '@testing-library/react';
import BookReportPage from './BookReportPage';

jest.mock('../../components/layout/Header', () => () => <div data-testid="header" />);

describe('BookReportPage', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('shows an empty state when no report has been generated', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue({ status: 404, ok: false });

    render(<BookReportPage />);

    expect(await screen.findByText('아직 생성된 도서 리포트가 없습니다.')).toBeInTheDocument();
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
