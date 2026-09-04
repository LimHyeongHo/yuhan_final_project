import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { Shield, CheckCircle, AlertTriangle, Clock, RefreshCw, Download, Filter, BellRing } from 'lucide-react';
import Header from '../../../components/layout/Header';

const SecurityLogPage = () => {
  const [logs, setLogs] = useState([]);
  const [tamperedCount, setTamperedCount] = useState(0);
  const [latestTampered, setLatestTampered] = useState(null);
  const [showAlarm, setShowAlarm] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;
  const prevLogsRef = useRef([]);

  const fetchLogs = () => {
    fetch('http://localhost:8080/api/admin/logs?type=SECURITY', { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        const newLogs = Array.isArray(data) ? data : [];
        setLogs(newLogs);
        
        const currentTampered = newLogs.filter(log => log.status === 'TAMPERED' || log.status === 'FORGED');
        setTamperedCount(currentTampered.length);
        
        if (currentTampered.length > 0) {
          setLatestTampered(currentTampered[0]); // 최신 위변조 로그
        }
        
        // 이전 로그 개수와 비교하여 새로운 TAMPERED 로그가 추가되었는지 확인 (알람 트리거)
        const prevTampered = prevLogsRef.current.filter(log => log.status === 'TAMPERED' || log.status === 'FORGED');
        if (currentTampered.length > prevTampered.length && prevLogsRef.current.length > 0) {
          setShowAlarm(true);
          // 5초 후 알람 끄기
          setTimeout(() => setShowAlarm(false), 5000);
        }
        
        prevLogsRef.current = newLogs;
      })
      .catch(error => console.error("보안 로그를 가져오는데 실패했습니다:", error));
  };

  useEffect(() => {
    fetchLogs(); // 초기 로드
    
    // 3초마다 실시간 폴링
    const interval = setInterval(fetchLogs, 3000);
    return () => clearInterval(interval);
  }, []);

  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentLogs = logs.slice(indexOfFirstItem, indexOfLastItem);
  const totalPages = Math.ceil(logs.length / itemsPerPage);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900 relative">
      <Header />
      
      {/* 실시간 알람 배너 */}
      {showAlarm && (
        <div className="fixed top-0 left-0 w-full z-50 animate-bounce">
          <div className="bg-red-600 text-white font-bold text-center py-3 flex items-center justify-center gap-2 shadow-2xl">
            <BellRing size={24} className="animate-pulse" />
            [긴급 알림] 데이터베이스 위변조(해킹) 공격이 실시간으로 감지되어 즉시 차단되었습니다!
          </div>
        </div>
      )}

      {/* 2. 상단 묵직한 다크 보안 배너 */}
      <section className="bg-slate-900 text-white py-12 px-6 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div className="flex flex-col gap-2">
            <span className="bg-blue-500/20 text-blue-400 text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full w-max border border-blue-500/30">
              Security Operations Hub
            </span>
            <h2 className="text-4xl font-extrabold tracking-tight">
              시스템 보안 및 감사 로그
            </h2>
            <p className="text-slate-400 font-medium text-base max-w-2xl">
              블록체인 고유 해시 기반 실시간 검증 엔진이 가동 중입니다. 플랫폼 내의 모든 공동구매 계약 데이터 무결성을 실시간으로 보호하고 모니터링합니다.
            </p>
          </div>
          <Link to="/admin/simulator" className="px-6 py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl transition-all shadow-lg flex items-center gap-2 whitespace-nowrap">
            <AlertTriangle size={20} />
            보안 검증 시뮬레이터 실행
          </Link>
        </div>
      </section>

      {/* 3. 메인 콘텐츠 영역 */}
      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col gap-8">
        
        {/* 상단 3종 요약 통계 카드 */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">보안 상태 개요</span>
              <h3 className="text-2xl font-black text-gray-950 mt-1">Hash Integrity</h3>
              <span className="text-blue-600 font-extrabold text-lg mt-1 flex items-center gap-1.5">
                <Shield size={18} /> {tamperedCount === 0 ? 'SECURED' : 'ALERT'}
              </span>
            </div>
            <div className={`w-12 h-12 rounded-full flex items-center justify-center ${tamperedCount === 0 ? 'bg-blue-50 text-blue-600' : 'bg-red-50 text-red-600 animate-pulse'}`}>
              <CheckCircle size={24} />
            </div>
          </div>

          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">누적 검증 로그</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">{logs.length} 건</h3>
              <span className="text-emerald-600 text-xs font-bold mt-1">
                실시간 데이터 연동 중
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-600">
              <RefreshCw size={22} className={logs.length > 0 ? "animate-spin" : ""} style={{ animationDuration: '3s' }} />
            </div>
          </div>

          <div className={`rounded-[24px] p-6 border-2 shadow-sm flex justify-between items-center ${tamperedCount > 0 ? 'bg-red-50/10 border-red-200' : 'bg-white border-gray-200'}`}>
            <div className="flex flex-col gap-1">
              <span className={`text-xs font-bold uppercase tracking-wider ${tamperedCount > 0 ? 'text-red-400' : 'text-gray-400'}`}>위변조 감지 건수</span>
              <h3 className={`text-3xl font-black mt-1 ${tamperedCount > 0 ? 'text-red-600' : 'text-gray-950'}`}>{tamperedCount < 10 ? `0${tamperedCount}` : tamperedCount} 건</h3>
              <span className="text-red-500 text-xs font-extrabold mt-1 uppercase tracking-tight">
                CRITICAL INCIDENTS
              </span>
            </div>
            <div className={`w-12 h-12 rounded-full flex items-center justify-center ${tamperedCount > 0 ? 'bg-red-100 text-red-600 animate-pulse' : 'bg-gray-100 text-gray-400'}`}>
              <AlertTriangle size={22} />
            </div>
          </div>
        </section>

        {/* 중간 테이블 섹션: 보안 및 감사 실시간 로그 */}
        <section className="bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm">
          <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4 mb-6">
            <div className="flex flex-col gap-1">
              <h3 className="text-xl font-extrabold text-gray-950 tracking-tight flex items-center gap-2">
                보안 및 감사 실시간 로그 
                <span className="relative flex h-3 w-3">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-3 w-3 bg-blue-500"></span>
                </span>
              </h3>
              <p className="text-xs text-gray-400">모든 거래 데이터의 해시 실시간 검증 결과 목록입니다.</p>
            </div>
            <div className="flex gap-2">
              <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 rounded-xl text-sm font-semibold text-white hover:bg-blue-700 transition shadow-md shadow-blue-100">
                <Download size={16} /> 실시간 로그 내보내기
              </button>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[700px]">
              <thead>
                <tr className="text-left text-xs font-bold text-gray-400 uppercase tracking-widest border-b border-gray-100 bg-gray-50/50">
                  <th className="py-3 px-4">거래 ID</th>
                  <th className="py-3 px-4">타임스탬프</th>
                  <th className="py-3 px-4 text-center">검증 상태</th>
                  <th className="py-3 px-4">원본 대비 차이</th>
                  <th className="py-3 px-4 text-right">상세 분석</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {logs.length === 0 && (
                  <tr>
                    <td colSpan="5" className="py-8 text-center text-gray-400 text-sm font-medium">
                      기록된 보안 로그가 없습니다.
                    </td>
                  </tr>
                )}
                {currentLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50/80 transition-colors">
                    <td className="py-4 px-4 font-bold text-sm text-gray-900 tracking-wide">{log.displayId}</td>
                    <td className="py-4 px-4 text-sm text-gray-500">{log.timestamp}</td>
                    <td className="py-4 px-4 text-center">
                      <span className={`inline-block text-[10px] font-black px-2.5 py-1 rounded-full uppercase ${
                        (log.status === 'SUCCESS' || log.status === 'VALID') 
                          ? 'bg-blue-50 text-blue-600 border border-blue-100' 
                          : 'bg-red-50 text-red-600 border border-red-100 animate-pulse'
                      }`}>
                        {log.status}
                      </span>
                    </td>
                    <td className="py-4 px-4 text-xs font-mono text-gray-600">{log.diff}</td>
                    <td className="py-4 px-4 text-right">
                      <button className={`text-xs font-bold transition ${
                        (log.status === 'TAMPERED' || log.status === 'FORGED') ? 'text-red-600 hover:underline' : 'text-blue-600 hover:underline'
                      }`}>
                        {(log.status === 'TAMPERED' || log.status === 'FORGED') ? '위변조 추적' : '상세 정보'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          
          {/* 페이지네이션 컨트롤 */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 mt-6">
              <button 
                onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                disabled={currentPage === 1}
                className="px-3 py-1 border border-gray-200 rounded-md text-sm disabled:opacity-50 hover:bg-gray-50"
              >
                이전
              </button>
              <span className="text-sm text-gray-600 font-medium px-2">
                {currentPage} / {totalPages}
              </span>
              <button 
                onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                disabled={currentPage === totalPages}
                className="px-3 py-1 border border-gray-200 rounded-md text-sm disabled:opacity-50 hover:bg-gray-50"
              >
                다음
              </button>
            </div>
          )}
        </section>

        {/* 하단 상세 추적 레이아웃 */}
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          
          {/* 좌측: FORENSIC TRACK 디테일 패널 (8/12) */}
          <div className="lg:col-span-8 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col gap-6">
            <div>
              <h4 className="text-xs font-bold text-red-500 uppercase tracking-widest">Forensic Track</h4>
              <h3 className="text-xl font-extrabold text-gray-950 mt-1">위변조 정밀 추적 데이터: {latestTampered ? latestTampered.displayId : '분석 대기 중...'}</h3>
            </div>

            {latestTampered ? (
              <>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="bg-gray-50 p-4 rounded-xl border border-gray-200">
                    <span className="text-xs font-bold text-gray-400">STORED BLOCKCHAIN HASH (정상 등록 해시)</span>
                    <p className="text-xs font-mono text-blue-600 mt-2 break-all bg-white p-2 rounded border border-gray-100">
                      [안전 보관됨] 블록체인에서 원본 해시 참조 대기 중...
                    </p>
                  </div>
                  <div className="bg-red-50/30 p-4 rounded-xl border border-red-100">
                    <span className="text-xs font-bold text-red-400">COMPUTED VERIFICATION HASH (현재 변조 해시)</span>
                    <p className="text-xs font-mono text-red-600 mt-2 break-all bg-white p-2 rounded border border-red-100">
                      [불일치 감지] {latestTampered.diff}
                    </p>
                  </div>
                </div>

                <div className="bg-red-50/50 p-4 rounded-xl border border-red-100 flex items-start gap-3">
                  <div className="w-8 h-8 rounded-full bg-red-100 text-red-600 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <AlertTriangle size={16} />
                  </div>
                  <div className="flex flex-col gap-1">
                    <span className="text-sm font-bold text-gray-900">비정상적인 데이터 실시간 차단 알림</span>
                    <p className="text-xs text-gray-600 leading-relaxed">
                      해당 거래({latestTampered.displayId})의 데이터가 외부 비인가 접근에 의해 임의 변조 및 수정된 내역이 감지되었습니다. 
                      시스템 백엔드 해시 엔진이 이를 {latestTampered.timestamp}에 즉각 차단하고 무효화 조치했습니다.
                    </p>
                  </div>
                </div>
              </>
            ) : (
              <div className="flex flex-col items-center justify-center py-12 text-gray-400 bg-gray-50 rounded-xl border border-dashed border-gray-200">
                <Shield size={48} className="mb-4 text-gray-300" />
                <p className="text-sm font-bold">감지된 위변조 내역이 없습니다.</p>
                <p className="text-xs mt-1">시스템이 안전하게 보호되고 있습니다.</p>
              </div>
            )}
          </div>

          {/* 우측: 보안 활동 타임라인 (4/12) */}
          <div className="lg:col-span-4 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col justify-between">
            <div>
              <h4 className="text-sm font-bold text-gray-900 mb-6">최근 보안 활동 타임라인</h4>
              
              <div className="flex flex-col gap-6 pl-2 relative border-l-2 border-gray-100 ml-2">
                {logs.slice(0, 3).map((log, index) => (
                  <div key={index} className="relative">
                    <span className={`absolute -left-[15px] top-1.5 w-2 h-2 rounded-full border-4 border-white box-content ${
                      (log.status === 'TAMPERED' || log.status === 'FORGED') ? 'bg-red-500' : 'bg-blue-600'
                    }`}></span>
                    <div className="flex flex-col gap-0.5 pl-4">
                      <span className={`text-xs font-bold flex items-center gap-1 ${
                        (log.status === 'TAMPERED' || log.status === 'FORGED') ? 'text-red-400' : 'text-gray-400'
                      }`}>
                        <Clock size={12} /> {log.timestamp.split(' ')[1] || log.timestamp}
                      </span>
                      <p className={`text-sm font-bold ${
                        (log.status === 'TAMPERED' || log.status === 'FORGED') ? 'text-red-600' : 'text-gray-800'
                      }`}>
                        {log.displayId} 검증 {(log.status === 'TAMPERED' || log.status === 'FORGED') ? '불일치(위변조 적발)' : '통과'}
                      </p>
                    </div>
                  </div>
                ))}
                
                {logs.length === 0 && (
                  <div className="text-xs text-gray-400 pl-4">최근 활동 내역이 없습니다.</div>
                )}
              </div>
            </div>

            <div className="mt-8 pt-4 border-t border-gray-100 flex items-center justify-between text-xs font-bold text-gray-600">
              <span>실시간 모니터링 상태</span>
              <span className="text-blue-600 flex items-center gap-1">
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
                </span>
                ACTIVE 100%
              </span>
            </div>
          </div>

        </section>
      </main>
    </div>
  );
};

export default SecurityLogPage;