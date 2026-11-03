import React, { useEffect, useState } from 'react';
import {
  AlertCircle,
  BookOpen,
  CheckCircle2,
  Clock3,
  ExternalLink,
  RefreshCw,
  Sparkles,
} from 'lucide-react';
import V3SiteHeader from '../../components/layout/V3SiteHeader';
import { useSession } from '../../contexts/SessionContext';
import { PRODUCT_CATEGORIES } from '../../constants/productCategories';
import { API_BASE } from '../../config/api';
import './BookReportPage.css';

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

const MANUAL_JOB_STATUS = {
  QUEUED: { label: '대기 중', description: '생성 작업이 곧 시작됩니다.' },
  RUNNING: { label: '생성 중', description: '도서 정보를 수집하고 리포트를 분석하고 있습니다.' },
  COMPLETED: { label: '생성 완료', description: '최신 리포트를 다시 불러왔습니다.' },
  FAILED: { label: '생성 실패', description: '작업 결과를 확인한 후 다시 시도해 주세요.' },
};

const getCategoryName = (category) => (
  REPORT_CATEGORIES.find(({ code }) => code === category)?.name || category
);

const BookCard = ({ book }) => (
  <article className="book-report-card">
    <div className="book-report-card__heading">
      <div className="book-report-card__title">
        <h3>{book.title}</h3>
        <p>
          {[book.authors?.join(', '), book.publisher, formatDate(book.publishedAt)]
            .filter(Boolean)
            .map((item) => <span key={item}>{item}</span>)}
        </p>
      </div>
      <div className="book-report-card__scores" aria-label="도서 분석 점수">
        <span>학과 적합도 <strong>{book.relevanceScore}</strong></span>
        <span>공동구매 주목도 <strong>{book.attentionScore}</strong></span>
      </div>
    </div>

    <dl className="book-report-card__facts">
      <div><dt>ISBN</dt><dd>{book.isbn || '정보 없음'}</dd></div>
      <div><dt>난이도</dt><dd>{difficultyLabel[book.difficulty] || '판단 보류'}</dd></div>
      <div><dt>정가</dt><dd>{formatCurrency(book.price)}</dd></div>
      <div><dt>판매가</dt><dd>{formatCurrency(book.salePrice)} ({book.discountRate}% 할인)</dd></div>
    </dl>

    {book.topics?.length > 0 && (
      <div className="book-report-card__topics" aria-label="도서 주제">
        {book.topics.map((topic) => <span key={topic}>{topic}</span>)}
      </div>
    )}

    <div className="book-report-card__analysis">
      <div><strong>자동 요약</strong><p>{book.summary}</p></div>
      <div><strong>추천 대상</strong><p>{book.recommendedFor}</p></div>
      <div><strong>자동 추천 이유</strong><p>{book.recommendationReason}</p></div>
    </div>

    {book.sourceUrl && (
      <a
        href={book.sourceUrl}
        target="_blank"
        rel="noreferrer"
        className="book-report-card__link"
      >
        카카오 도서 상세 보기 <ExternalLink size={14} />
      </a>
    )}
  </article>
);

const BookReportPage = () => {
  const { session } = useSession();
  const [category, setCategory] = useState('COMPUTER_SOFTWARE');
  const [report, setReport] = useState(null);
  const [status, setStatus] = useState('loading');
  const [reloadKey, setReloadKey] = useState(0);
  const [manualJob, setManualJob] = useState(null);
  const [manualSubmitting, setManualSubmitting] = useState(false);
  const [manualError, setManualError] = useState('');
  const userRole = session?.role || localStorage.getItem('user_role');
  const isAdmin = userRole === 'ROLE_ADMIN';
  const manualJobActive = ['QUEUED', 'RUNNING'].includes(manualJob?.status);

  useEffect(() => {
    const controller = new AbortController();
    const loadReport = async () => {
      setStatus('loading');
      setReport(null);
      try {
        const response = await fetch(
          `${API_BASE}/book-reports/latest?category=${encodeURIComponent(category)}`,
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

  useEffect(() => {
    if (!manualJob?.jobId || !['QUEUED', 'RUNNING'].includes(manualJob.status)) return undefined;

    const controller = new AbortController();
    let pollTimer;
    const pollJob = async () => {
      try {
        const response = await fetch(
          `${API_BASE}/admin/book-reports/jobs/${encodeURIComponent(manualJob.jobId)}`,
          { credentials: 'include', signal: controller.signal },
        );
        if (!response.ok) throw new Error('MANUAL_JOB_LOAD_FAILED');
        const nextJob = await response.json();
        setManualJob(nextJob);
        if (nextJob.status === 'COMPLETED') {
          setReloadKey((value) => value + 1);
        } else if (['QUEUED', 'RUNNING'].includes(nextJob.status)) {
          pollTimer = window.setTimeout(pollJob, 2000);
        }
      } catch (error) {
        if (error.name !== 'AbortError') {
          setManualError('작업 상태를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
        }
      }
    };

    pollTimer = window.setTimeout(pollJob, 1200);
    return () => {
      controller.abort();
      window.clearTimeout(pollTimer);
    };
  }, [manualJob?.jobId, manualJob?.status]);

  const handleManualGenerate = async () => {
    setManualSubmitting(true);
    setManualError('');
    try {
      const response = await fetch(`${API_BASE}/admin/book-reports/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ category }),
      });
      const data = await response.json().catch(() => null);
      if (response.status === 409 && data) {
        setManualJob(data);
        return;
      }
      if (!response.ok || !data) throw new Error('MANUAL_GENERATE_FAILED');
      setManualJob(data);
    } catch {
      setManualError('수동 생성 요청을 시작하지 못했습니다. 관리자 세션을 확인해 주세요.');
    } finally {
      setManualSubmitting(false);
    }
  };

  const isReportEmpty = report?.sections?.every((section) => section.books.length === 0);

  return (
    <div className="book-report-page v3-design-page">
      <V3SiteHeader />
      <main className="v3-unified-page-card book-report-shell">
        <header className="v3-unified-page-card__header book-report-heading">
          <div>
            <h1 className="v3-unified-page-card__title"><Sparkles size={28} />학과별 도서 리포트</h1>
            <p>학과 적합도 분석을 바탕으로 공동구매를 검토할 신규 도서를 확인합니다.</p>
          </div>
          <label className="book-report-category" htmlFor="report-category">
            <span>학과 선택</span>
            <select
              id="report-category"
              value={category}
              onChange={(event) => setCategory(event.target.value)}
            >
              {REPORT_CATEGORIES.map(({ code, name }) => <option key={code} value={code}>{name}</option>)}
            </select>
          </label>
        </header>

        {isAdmin && (
          <section className="book-report-admin-panel" aria-labelledby="manual-report-title">
            <div className="book-report-admin-panel__copy">
              <h2 id="manual-report-title">관리자 수동 생성</h2>
              <p>선택한 학과의 오늘 리포트를 백그라운드에서 다시 생성합니다.</p>
            </div>
            <button
              type="button"
              className="book-report-admin-panel__action"
              onClick={handleManualGenerate}
              disabled={manualSubmitting || manualJobActive}
            >
              {manualSubmitting || manualJobActive
                ? <RefreshCw aria-hidden="true" />
                : <Sparkles aria-hidden="true" />}
              {manualSubmitting ? '요청 중' : manualJobActive ? '생성 중' : '리포트 생성'}
            </button>

            <div
              className={`book-report-manual-status book-report-manual-status--${manualJob?.status?.toLowerCase() || 'idle'}`}
              role="status"
              aria-live="polite"
            >
              {manualJob?.status === 'COMPLETED' ? (
                <CheckCircle2 aria-hidden="true" />
              ) : manualJob?.status === 'FAILED' ? (
                <AlertCircle aria-hidden="true" />
              ) : manualJob?.status === 'QUEUED' || manualJob?.status === 'RUNNING' ? (
                <RefreshCw className="book-report-manual-status__working" aria-hidden="true" />
              ) : (
                <Clock3 aria-hidden="true" />
              )}
              <div>
                <strong>{manualJob ? MANUAL_JOB_STATUS[manualJob.status]?.label || manualJob.status : '실행 대기'}</strong>
                <p>
                  {manualJob
                    ? `${getCategoryName(manualJob.category)}: ${manualJob.message || MANUAL_JOB_STATUS[manualJob.status]?.description || '상태를 확인하고 있습니다.'}`
                    : '학과를 선택한 후 리포트 생성 버튼을 눌러 주세요.'}
                </p>
              </div>
            </div>

            {manualError && (
              <p className="book-report-admin-panel__error" role="alert">{manualError}</p>
            )}
          </section>
        )}

        <section className="v3-unified-page-card__body book-report-body" aria-live="polite">
          {status === 'loading' && (
            <div className="book-report-state book-report-loading" role="status">
              <RefreshCw aria-hidden="true" />
              <strong>리포트를 불러오는 중입니다.</strong>
              <div className="book-report-loading__skeleton" aria-hidden="true">
                <span /><span /><span />
              </div>
            </div>
          )}

          {status === 'error' && (
            <div className="book-report-state book-report-state--error" role="alert">
              <AlertCircle aria-hidden="true" />
              <div><strong>리포트를 불러오지 못했습니다.</strong><p>잠시 후 다시 시도해 주세요.</p></div>
              <button type="button" onClick={() => setReloadKey((value) => value + 1)}>다시 시도</button>
            </div>
          )}

          {status === 'empty' && (
            <div className="book-report-state book-report-state--empty">
              <BookOpen aria-hidden="true" size={34} />
              <div><strong>아직 생성된 도서 리포트가 없습니다.</strong><p>선택한 학과의 리포트가 생성되면 이곳에 표시됩니다.</p></div>
            </div>
          )}

          {report && (
            <div className="book-report-content">
              <div className="book-report-meta">
                <strong>{report.categoryName}</strong>
                <span>{formatDate(report.reportDate)} 리포트</span>
                <span>출처: 카카오 Daum 책 검색</span>
                <span>수집: {formatDate(report.source?.fetchedAt)}</span>
                <span>설정: {report.source?.keywordConfigVersion}</span>
              </div>
              {status === 'partial' && (
                <div className="book-report-notice book-report-notice--warning">일부 도서 분석에 실패해 확인 가능한 결과만 표시합니다.</div>
              )}
              {isReportEmpty ? (
                <div className="book-report-state book-report-state--empty"><BookOpen aria-hidden="true" /><strong>오늘 추천할 신규 도서가 없습니다.</strong></div>
              ) : report.sections.map((section) => (
                <section className="book-report-section" key={section.type}>
                  <h2>{section.title}</h2>
                  {section.books.length === 0 ? (
                    <p className="book-report-section__empty">해당하는 도서가 없습니다.</p>
                  ) : (
                    <div className="book-report-grid">{section.books.map((book) => <BookCard key={book.analysisId} book={book} />)}</div>
                  )}
                </section>
              ))}
              <p className="book-report-notice">{report.notice}</p>
            </div>
          )}
        </section>
      </main>
    </div>
  );
};

export default BookReportPage;
