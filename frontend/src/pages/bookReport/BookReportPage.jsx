import React, { useEffect, useState } from 'react';
import { AlertCircle, BookOpen, ExternalLink, RefreshCw, Sparkles } from 'lucide-react';
import Header from '../../components/layout/Header';
import { PRODUCT_CATEGORIES } from '../../constants/productCategories';

const REPORT_CATEGORIES = PRODUCT_CATEGORIES.filter(
  ({ code }) => code !== 'FREE_MAJOR' && code !== 'GENERAL',
);

const formatDate = (value) => (value ? new Intl.DateTimeFormat('ko-KR').format(new Date(value)) : '정보 없음');
const formatCurrency = (value) => (value == null ? '정보 없음' : `${Number(value).toLocaleString('ko-KR')}원`);

const difficultyLabel = {
  BEGINNER: '입문',
  INTERMEDIATE: '중급',
  ADVANCED: '심화',
  UNKNOWN: '판단 보류',
};

const BookCard = ({ book }) => (
  <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div>
        <h3 className="text-lg font-bold text-slate-950">{book.title}</h3>
        <p className="mt-1 text-sm text-slate-500">
          {[book.authors?.join(', '), book.publisher, formatDate(book.publishedAt)].filter(Boolean).join(' · ')}
        </p>
      </div>
      <div className="flex gap-2 text-xs font-bold">
        <span className="rounded-full bg-blue-50 px-3 py-1 text-blue-700">학과 적합도 {book.relevanceScore}</span>
        <span className="rounded-full bg-emerald-50 px-3 py-1 text-emerald-700">공동구매 주목도 {book.attentionScore}</span>
      </div>
    </div>

    <div className="mt-4 grid gap-3 text-sm text-slate-600 sm:grid-cols-2">
      <p><strong className="text-slate-800">ISBN</strong> {book.isbn || '정보 없음'}</p>
      <p><strong className="text-slate-800">난이도</strong> {difficultyLabel[book.difficulty] || '판단 보류'}</p>
      <p><strong className="text-slate-800">정가</strong> {formatCurrency(book.price)}</p>
      <p><strong className="text-slate-800">판매가</strong> {formatCurrency(book.salePrice)} ({book.discountRate}% 할인)</p>
    </div>

    {book.topics?.length > 0 && (
      <div className="mt-4 flex flex-wrap gap-2">
        {book.topics.map((topic) => (
          <span key={topic} className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">{topic}</span>
        ))}
      </div>
    )}

    <div className="mt-5 space-y-3 border-t border-slate-100 pt-4 text-sm leading-6 text-slate-700">
      <p><strong className="text-slate-950">자동 요약</strong><br />{book.summary}</p>
      <p><strong className="text-slate-950">추천 대상</strong><br />{book.recommendedFor}</p>
      <p><strong className="text-slate-950">자동 추천 이유</strong><br />{book.recommendationReason}</p>
    </div>

    {book.sourceUrl && (
      <a
        href={book.sourceUrl}
        target="_blank"
        rel="noreferrer"
        className="mt-5 inline-flex items-center gap-1 text-sm font-bold text-blue-700 hover:text-blue-900"
      >
        카카오 도서 상세 보기 <ExternalLink size={14} />
      </a>
    )}
  </article>
);

const BookReportPage = () => {
  const [category, setCategory] = useState('COMPUTER_SOFTWARE');
  const [report, setReport] = useState(null);
  const [status, setStatus] = useState('loading');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    const loadReport = async () => {
      setStatus('loading');
      setReport(null);
      try {
        const response = await fetch(
          `http://${window.location.hostname}:8080/api/book-reports/latest?category=${encodeURIComponent(category)}`,
          { credentials: 'include', signal: controller.signal },
        );
        if (response.status === 404) {
          setStatus('empty');
          return;
        }
        if (!response.ok) throw new Error('REPORT_LOAD_FAILED');
        const data = await response.json();
        setReport(data);
        setStatus(data.status === 'PARTIAL' ? 'partial' : 'ready');
      } catch (error) {
        if (error.name !== 'AbortError') setStatus('error');
      }
    };
    loadReport();
    return () => controller.abort();
  }, [category, reloadKey]);

  const isReportEmpty = report?.sections?.every((section) => section.books.length === 0);

  return (
    <>
      <Header />
      <main className="min-h-screen bg-slate-50 px-4 py-10">
        <div className="mx-auto max-w-6xl">
          <div className="rounded-3xl bg-slate-950 p-7 text-white sm:p-10">
            <div className="flex items-center gap-2 text-sm font-bold text-blue-300"><Sparkles size={18} /> 학과별 도서 리포트</div>
            <h1 className="mt-3 text-3xl font-black sm:text-4xl">학과에 맞는 신규 도서 후보를 확인하세요</h1>
            <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">카카오 도서 정보와 학과 적합도 분석을 결합해 공동구매로 검토할 만한 책을 정리합니다.</p>
            <label className="mt-6 block max-w-sm text-sm font-bold" htmlFor="report-category">학과 선택</label>
            <select
              id="report-category"
              value={category}
              onChange={(event) => setCategory(event.target.value)}
              className="mt-2 w-full max-w-sm rounded-xl border border-slate-700 bg-slate-900 px-4 py-3 text-sm text-white"
            >
              {REPORT_CATEGORIES.map(({ code, name }) => <option key={code} value={code}>{name}</option>)}
            </select>
          </div>

          {status === 'loading' && (
            <div className="py-24 text-center text-slate-500"><RefreshCw className="mx-auto mb-3 animate-spin" />리포트를 불러오는 중입니다.</div>
          )}

          {status === 'error' && (
            <div className="my-8 rounded-2xl border border-red-200 bg-red-50 p-6 text-red-800">
              <AlertCircle className="mb-2" />리포트를 불러오지 못했습니다.
              <button type="button" onClick={() => setReloadKey((value) => value + 1)} className="ml-3 font-bold underline">다시 시도</button>
            </div>
          )}

          {status === 'empty' && (
            <div className="my-8 rounded-2xl border border-slate-200 bg-white p-12 text-center text-slate-600">
              <BookOpen className="mx-auto mb-3 text-slate-400" size={34} />아직 생성된 도서 리포트가 없습니다.
            </div>
          )}

          {report && (
            <div className="mt-8 space-y-9">
              <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-slate-500">
                <p>{report.categoryName} · {formatDate(report.reportDate)} 리포트</p>
                <p>출처: 카카오 Daum 책 검색 · 수집 {formatDate(report.source?.fetchedAt)} · 설정 {report.source?.keywordConfigVersion}</p>
              </div>
              {status === 'partial' && (
                <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">일부 도서 분석에 실패해 확인 가능한 결과만 표시합니다.</div>
              )}
              {isReportEmpty ? (
                <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center text-slate-600">오늘 추천할 신규 도서가 없습니다.</div>
              ) : report.sections.map((section) => (
                <section key={section.type}>
                  <h2 className="mb-4 text-2xl font-black text-slate-950">{section.title}</h2>
                  {section.books.length === 0 ? (
                    <p className="rounded-2xl border border-dashed border-slate-300 p-8 text-center text-sm text-slate-500">해당하는 도서가 없습니다.</p>
                  ) : (
                    <div className="grid gap-5 lg:grid-cols-2">{section.books.map((book) => <BookCard key={book.analysisId} book={book} />)}</div>
                  )}
                </section>
              ))}
              <p className="rounded-2xl bg-slate-100 p-5 text-sm leading-6 text-slate-600">{report.notice}</p>
            </div>
          )}
        </div>
      </main>
    </>
  );
};

export default BookReportPage;
