import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Eye,
  Package,
  Star,
  Users,
  Wallet,
  X,
} from 'lucide-react';
import AdminHeader from '../../../components/admin/AdminHeader';

const REPORTS_PER_PAGE = 10;
const getProductReportCount = (seller) => Number(seller.postReportCount || 0);
const getChatReportCount = (seller) => Number(seller.chatReportCount || 0);
const REPORT_STATUS = {
  PENDING: { label: '검토 대기', className: 'bg-red-50 text-red-600' },
  RECEIVED: { label: '신고 접수', className: 'bg-amber-50 text-amber-700' },
  RESOLVED: { label: '처리 완료', className: 'bg-emerald-50 text-emerald-700' },
  REJECTED: { label: '반려', className: 'bg-gray-100 text-gray-600' },
};

const UserAuthorization = () => {
  const [sellers, setSellers] = useState([]);
  const [reportLogs, setReportLogs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [sort, setSort] = useState({ key: null, direction: null });
  const [isReportLogOpen, setIsReportLogOpen] = useState(false);
  const [selectedSeller, setSelectedSeller] = useState(null);
  const [selectedReport, setSelectedReport] = useState(null);
  const [isUpdatingReport, setIsUpdatingReport] = useState(false);
  const [reportPage, setReportPage] = useState(1);

  useEffect(() => {
    Promise.all([
      fetch('http://localhost:8080/api/admin/sellers/stats-list', { credentials: 'include' }),
      fetch('http://localhost:8080/api/admin/product-reports', { credentials: 'include' }),
    ])
      .then(async ([sellersResponse, reportsResponse]) => {
        if (!sellersResponse.ok || !reportsResponse.ok) {
          throw new Error('신고 데이터를 불러오지 못했습니다.');
        }
        const [sellerData, reportData] = await Promise.all([
          sellersResponse.json(),
          reportsResponse.json(),
        ]);
        setSellers(sellerData);
        setReportLogs(reportData);
      })
      .catch((error) => console.error(error))
      .finally(() => setIsLoading(false));
  }, []);

  const sortedSellers = useMemo(() => {
    if (!sort.key || !sort.direction) return sellers;

    return [...sellers].sort((a, b) => {
      const aValue = sort.key === 'name'
        ? (a.nickname || '')
        : Number(a[sort.key] || 0);
      const bValue = sort.key === 'name'
        ? (b.nickname || '')
        : Number(b[sort.key] || 0);
      const comparison = typeof aValue === 'string'
        ? aValue.localeCompare(bValue, 'ko')
        : aValue - bValue;
      return sort.direction === 'asc' ? comparison : -comparison;
    });
  }, [sellers, sort]);

  const visibleReportLogs = useMemo(() => (
    selectedSeller
      ? reportLogs.filter((report) => report.sellerEmail === selectedSeller.sellerEmail)
      : reportLogs
  ), [reportLogs, selectedSeller]);
  const totalReportPages = Math.max(1, Math.ceil(visibleReportLogs.length / REPORTS_PER_PAGE));
  const pagedReportLogs = visibleReportLogs.slice((reportPage - 1) * REPORTS_PER_PAGE, reportPage * REPORTS_PER_PAGE);

  const changeSort = (key) => {
    setSort((current) => {
      if (current.key !== key) return { key, direction: 'asc' };
      if (current.direction === 'asc') return { key, direction: 'desc' };
      return { key: null, direction: null };
    });
  };

  const openReportLog = (seller = null) => {
    setSelectedSeller(seller);
    setReportPage(1);
    setIsReportLogOpen(true);
  };

  const closeReportLog = () => {
    setIsReportLogOpen(false);
    setSelectedSeller(null);
    setSelectedReport(null);
  };

  const updateReportStatus = async (report, status) => {
    if (!window.confirm(`이 신고를 ${REPORT_STATUS[status].label}(으)로 처리하시겠습니까?`)) return;

    setIsUpdatingReport(true);
    try {
      const response = await fetch(`http://localhost:8080/api/admin/product-reports/${report.id}/status`, {
        method: 'PATCH',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
      });
      if (!response.ok) throw new Error('신고 처리에 실패했습니다.');

      setReportLogs((current) => current.map((item) => (
        item.id === report.id
          ? { ...item, statusCode: status, status: REPORT_STATUS[status].label }
          : item
      )));
      setSelectedReport((current) => (
        current?.id === report.id
          ? { ...current, statusCode: status, status: REPORT_STATUS[status].label }
          : current
      ));
    } catch (error) {
      alert(error.message);
    } finally {
      setIsUpdatingReport(false);
    }
  };

  const renderSortIcon = (key) => {
    if (sort.key !== key || !sort.direction) return <ArrowUpDown size={15} aria-hidden="true" />;
    return sort.direction === 'asc'
      ? <ArrowUp size={15} aria-label="오름차순" />
      : <ArrowDown size={15} aria-label="내림차순" />;
  };

  const SortButton = ({ sortKey, children, align = 'left' }) => (
    <button
      type="button"
      onClick={() => changeSort(sortKey)}
      className={`flex w-full items-center gap-1 font-bold transition-colors hover:text-gray-900 ${align === 'right' ? 'justify-end' : 'justify-start'}`}
      title="클릭: 오름차순 → 내림차순 → 기본순"
    >
      {children}
      {renderSortIcon(sortKey)}
    </button>
  );

  return (
    <div className="v3-page admin-v3-page min-h-screen flex flex-col text-gray-900">
      <AdminHeader />

      <div className="v3-unified-page-card">
        <header className="v3-unified-page-card__header">
          <h1 className="v3-unified-page-card__title"><Users size={28} />판매자 관리</h1>
        </header>

        <main className="v3-unified-page-card__body admin-v3-main flex-grow">
          <section className="grid grid-cols-1 gap-6 md:grid-cols-[repeat(3,minmax(0,1fr))_auto]">
            <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
              <div>
                <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">판매자 수</span>
                <p className="mt-1 text-3xl font-black text-gray-950">{sellers.length}명</p>
              </div>
              <Users size={24} className="text-blue-600" />
            </div>
            <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
              <div>
                <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">등록 상품</span>
                <p className="mt-1 text-3xl font-black text-gray-950">{sellers.reduce((sum, seller) => sum + Number(seller.productCount || 0), 0)}건</p>
              </div>
              <Package size={24} className="text-emerald-600" />
            </div>
            <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
              <div>
                <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">누적 수익</span>
                <p className="mt-1 text-3xl font-black text-gray-950">{sellers.reduce((sum, seller) => sum + Number(seller.totalRevenue || 0), 0).toLocaleString()}원</p>
              </div>
              <Wallet size={24} className="text-violet-600" />
            </div>
            <button
              type="button"
              onClick={() => openReportLog()}
              className="rounded-[24px] border border-red-200 bg-red-50 px-6 py-4 font-bold text-red-700 transition-colors hover:bg-red-100 active:scale-[0.98] md:self-stretch"
            >
              <span className="flex items-center justify-center gap-2"><ClipboardList size={19} />신고 로그</span>
              <span className="mt-1 block text-sm font-medium text-red-500">{reportLogs.length}건 전체 보기</span>
            </button>
          </section>

          <section className="mt-6 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-xl font-extrabold tracking-tight">판매자 목록</h2>
              <span className="text-xs font-medium text-gray-400">신고 데이터는 현재 임시 데이터입니다.</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full min-w-[900px] text-left">
                <thead>
                  <tr className="border-y border-gray-200 bg-gray-50 text-sm text-gray-500">
                    <th className="px-5 py-4"><SortButton sortKey="name">판매자 이름</SortButton></th>
                    <th className="px-5 py-4 text-right"><SortButton sortKey="productCount" align="right">상품 개수</SortButton></th>
                    <th className="px-5 py-4 text-right"><SortButton sortKey="rating" align="right">평점</SortButton></th>
                    <th className="px-5 py-4 text-right"><SortButton sortKey="totalRevenue" align="right">수익</SortButton></th>
                    <th className="px-5 py-4 text-right font-bold text-red-600">상품 신고 횟수</th>
                    <th className="px-5 py-4 text-right font-bold text-red-600">채팅 신고 횟수</th>
                  </tr>
                </thead>
                <tbody>
                  {isLoading ? (
                    <tr><td colSpan="6" className="px-5 py-10 text-center font-medium text-gray-400">판매자 정보를 불러오는 중입니다.</td></tr>
                  ) : sellers.length === 0 ? (
                    <tr><td colSpan="6" className="px-5 py-10 text-center font-medium text-gray-400">등록된 판매자가 없습니다.</td></tr>
                  ) : (
                    sortedSellers.map((seller) => {
                      const sourceIndex = sellers.indexOf(seller);
                      const productReportCount = getProductReportCount(seller);
                      const chatReportCount = getChatReportCount(seller);
                      return (
                        <tr key={`${seller.email}-${sourceIndex}`} className="border-b border-gray-100 hover:bg-gray-50/70">
                          <td className="px-5 py-4">
                            <p className="font-bold text-gray-900">{seller.nickname || '이름 없음'}</p>
                            <p className="mt-0.5 text-xs text-gray-400">{seller.email}</p>
                          </td>
                          <td className="px-5 py-4 text-right font-bold text-gray-700">{Number(seller.productCount || 0)}개</td>
                          <td className="px-5 py-4">
                            <span className="flex justify-end items-center gap-1 font-bold text-amber-600"><Star size={15} fill="currentColor" />{Number(seller.rating || 0).toFixed(1)} / 5.0</span>
                          </td>
                          <td className="px-5 py-4 text-right font-black text-emerald-600">{Number(seller.totalRevenue || 0).toLocaleString()}원</td>
                          <td className="px-5 py-4 text-right">
                            <button
                              type="button"
                              onClick={() => openReportLog(seller)}
                              className="inline-flex font-bold text-red-600 underline-offset-4 hover:underline"
                            >
                              {productReportCount}건
                            </button>
                          </td>
                          <td className="px-5 py-4 text-right">
                            <button
                              type="button"
                              onClick={() => openReportLog(seller)}
                              className="inline-flex font-bold text-red-600 underline-offset-4 hover:underline"
                            >
                              {chatReportCount}건
                            </button>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </main>
      </div>

      {isReportLogOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-950/45 p-4" role="dialog" aria-modal="true" aria-labelledby="report-log-title">
          <section className="report-log-modal w-full max-w-4xl rounded-[24px] bg-white shadow-2xl">
            <header className="flex items-start justify-between border-b border-gray-200 px-6 py-5">
              <div>
                <h2 id="report-log-title" className="pl-1 flex items-center gap-2 text-xl font-extrabold text-gray-900"><ClipboardList size={22} className="text-red-600" />신고 로그</h2>
                <p className="pl-1 mt-1 text-sm text-gray-500">{selectedSeller ? `${selectedSeller.nickname || '이름 없음'} 판매자의 신고 내역` : '전체 판매자 신고 내역'}</p>
              </div>
              <button type="button" onClick={closeReportLog} className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-800" aria-label="신고 로그 닫기"><X size={20} /></button>
            </header>

            <div className="max-h-[55vh] overflow-auto px-6 py-4">
              {pagedReportLogs.length === 0 ? (
                <p className="py-10 text-center text-sm font-medium text-gray-400">표시할 신고 내역이 없습니다.</p>
              ) : (
                <table className="w-full min-w-[760px] text-left text-sm">
                  <thead className="text-gray-500" >
                    <tr className="border-b border-gray-200">
                      {!selectedSeller && <th className="py-3 pr-4 font-bold px-4">판매자</th>}
                      <th className="py-4 pr-3 px-4 font-bold">신고 대상 상품</th>
                      <th className="py-3 pr-4 px-4 font-bold">신고 사유</th>
                      <th className="py-3 pr-4 px-4 font-bold">상세 내용</th>
                      <th className="py-3 pr-4 px-4 text-center font-bold">신고 일자</th>
                      <th className="py-3 px-4 text-center font-bold">상태</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagedReportLogs.map((report) => (
                      <tr key={report.id} onClick={() => setSelectedReport(report)} className="cursor-pointer border-b border-gray-100 last:border-0 hover:bg-gray-50">
                        {!selectedSeller && <td className="py-4 pr-4 px-4 font-bold text-gray-800">{report.sellerName}</td>}
                        <td className="py-4 pr-4 px-4 font-bold text-gray-800">{report.productName}</td>
                        <td className="py-4 pr-4 px-4 text-gray-600">{report.reason}</td>
                        <td className="py-4 pr-4 px-4 text-gray-600">{report.detail || '-'}</td>
                        <td className="py-4 pr-4 px-4 text-center text-gray-500">{report.reportedAt}</td>
                        <td className="py-4 px-4 text-center"><span className={`rounded-full px-2.5 py-1 text-xs font-bold ${(REPORT_STATUS[report.statusCode] || REPORT_STATUS.PENDING).className}`}>{(REPORT_STATUS[report.statusCode] || REPORT_STATUS.PENDING).label}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            <footer className="flex items-center justify-between border-t border-gray-200 px-7 py-4 ">
              <span className="text-sm text-gray-500">총 {visibleReportLogs.length}건 · {reportPage} / {totalReportPages} 페이지</span>
              <div className="flex items-center gap-2">
                <button type="button" onClick={() => setReportPage((page) => Math.max(1, page - 1))} disabled={reportPage === 1} className="rounded-lg border border-gray-200 p-2 text-gray-600 disabled:cursor-not-allowed disabled:opacity-40" aria-label="이전 페이지"><ChevronLeft size={18} /></button>
                <button type="button" onClick={() => setReportPage((page) => Math.min(totalReportPages, page + 1))} disabled={reportPage === totalReportPages} className="rounded-lg border border-gray-200 p-2 text-gray-600 disabled:cursor-not-allowed disabled:opacity-40" aria-label="다음 페이지"><ChevronRight size={18} /></button>
              </div>
            </footer>
          </section>
        </div>
      )}

      {selectedReport && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-gray-950/45 p-4" role="dialog" aria-modal="true" aria-labelledby="report-detail-title">
          <section className="w-full max-w-xl rounded-[24px] bg-white shadow-2xl">
            <header className="flex items-start justify-between border-b border-gray-200 px-6 py-5">
              <div>
                <h2 id="report-detail-title" className="flex items-center gap-2 text-xl font-extrabold text-gray-900"><Eye size={22} className="text-red-600" />신고 상세</h2>
                <p className="mt-1 text-sm text-gray-500">신고 접수 내용과 처리 상태를 확인합니다.</p>
              </div>
              <button type="button" onClick={() => setSelectedReport(null)} className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-800" aria-label="신고 상세 닫기"><X size={20} /></button>
            </header>

            <div className="space-y-5 px-6 py-5">
              <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="rounded-xl bg-gray-50 p-4">
                  <dt className="text-xs font-bold text-gray-400">신고당한 판매자</dt>
                  <dd className="mt-1 font-extrabold text-gray-900">{selectedReport.sellerName}</dd>
                </div>
                <div className="rounded-xl bg-gray-50 p-4">
                  <dt className="text-xs font-bold text-gray-400">신고 상품</dt>
                  <dd className="mt-1 font-extrabold text-gray-900">{selectedReport.productName}</dd>
                </div>
                <div className="rounded-xl bg-gray-50 p-4">
                  <dt className="text-xs font-bold text-gray-400">신고 사유</dt>
                  <dd className="mt-1 font-bold text-gray-800">{selectedReport.reason}</dd>
                </div>
                <div className="rounded-xl bg-gray-50 p-4">
                  <dt className="text-xs font-bold text-gray-400">현재 상태</dt>
                  <dd className="mt-2"><span className={`rounded-full px-2.5 py-1 text-xs font-bold ${(REPORT_STATUS[selectedReport.statusCode] || REPORT_STATUS.PENDING).className}`}>{(REPORT_STATUS[selectedReport.statusCode] || REPORT_STATUS.PENDING).label}</span></dd>
                </div>
              </dl>

              <div>
                <p className="text-sm font-extrabold text-gray-800">상세 내용</p>
                <div className="mt-2 min-h-28 whitespace-pre-wrap rounded-xl border border-gray-200 bg-gray-50 p-4 text-sm leading-6 text-gray-700">
                  {selectedReport.detail || '작성된 상세 내용이 없습니다.'}
                </div>
              </div>

              <p className="text-right text-xs font-medium text-gray-400">신고 일자 {selectedReport.reportedAt}</p>
            </div>

            <footer className="flex justify-end gap-2 border-t border-gray-200 px-6 py-4">
              <button type="button" onClick={() => updateReportStatus(selectedReport, 'RECEIVED')} disabled={isUpdatingReport} className="rounded-xl bg-amber-600 px-5 py-2.5 text-sm font-bold text-white transition hover:bg-amber-700 disabled:cursor-not-allowed disabled:opacity-50">신고 접수</button>
              {selectedReport.statusCode === 'PENDING' && (
                <>
                  <button type="button" onClick={() => updateReportStatus(selectedReport, 'REJECTED')} disabled={isUpdatingReport} className="rounded-xl border border-red-200 bg-white px-5 py-2.5 text-sm font-bold text-red-600 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50">반려</button>
                  <button type="button" onClick={() => setSelectedReport(null)} disabled={isUpdatingReport} className="rounded-xl bg-gray-100 px-5 py-2.5 text-sm font-bold text-gray-700 transition hover:bg-gray-200 disabled:cursor-not-allowed disabled:opacity-50">닫기</button>
                </>
              )}
            </footer>
          </section>
        </div>
      )}
    </div>
  );
};

export default UserAuthorization;
