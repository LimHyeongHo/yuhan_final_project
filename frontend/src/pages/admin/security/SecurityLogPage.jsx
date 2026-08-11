import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Shield, CheckCircle, AlertTriangle, Clock, RefreshCw, Download, Filter, UserCircle } from 'lucide-react';

/// [*] 헤더 컴포넌트 연결
import Header from '../../../components/layout/Header';

const SecurityLogPage = () => {
  const [logs, setLogs] = useState([]);

  useEffect(() => {
    fetch('http://localhost:8080/api/admin/logs?type=SECURITY')
      .then(res => res.json())
      .then(data => setLogs(data))
      .catch(error => console.error("보안 로그를 가져오는데 실패했습니다:", error));
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
    {/* [*] 헤더 내용 수정 */}
    <Header />
      {/* 2. 상단 묵직한 다크 보안 배너 (메인 홈 배너 디자인 계승 + 다크 테마 변환) */}
      <section className="bg-slate-900 text-white py-12 px-6 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-col gap-2">
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
      </section>

      {/* 3. 메인 콘텐츠 영역 */}
      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col gap-8">
        
        {/* 상단 3종 요약 통계 카드 */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* 카드 1: 보안 상태 개요 */}
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">보안 상태 개요</span>
              <h3 className="text-2xl font-black text-gray-950 mt-1">Hash Integrity</h3>
              <span className="text-blue-600 font-extrabold text-lg mt-1 flex items-center gap-1.5">
                <Shield size={18} /> SECURED
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center text-blue-600">
              <CheckCircle size={24} />
            </div>
          </div>

          {/* 카드 2: 최근 24시간 검증 */}
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">최근 24시간 검증</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">12,842 건</h3>
              <span className="text-emerald-600 text-xs font-bold mt-1">
                ▲ +4.2% VS PREV HOUR
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-600">
              <RefreshCw size={22} />
            </div>
          </div>

          {/* 카드 3: 위변조 감지 건수 (경고 테마) */}
          <div className="bg-white rounded-[24px] p-6 border-2 border-red-100 shadow-sm bg-red-50/10 flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-red-400 uppercase tracking-wider">위변조 감지 건수</span>
              <h3 className="text-3xl font-black text-red-600 mt-1">03 건</h3>
              <span className="text-red-500 text-xs font-extrabold mt-1 uppercase tracking-tight">
                CRITICAL INCIDENTS
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center text-red-600 animate-pulse">
              <AlertTriangle size={22} />
            </div>
          </div>
        </section>

        {/* 중간 테이블 섹션: 보안 및 감사 실시간 로그 */}
        <section className="bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm">
          <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4 mb-6">
            <div className="flex flex-col gap-1">
              <h3 className="text-xl font-extrabold text-gray-950 tracking-tight">보안 및 감사 실시간 로그</h3>
              <p className="text-xs text-gray-400">모든 거래 데이터의 해시 실시간 검증 결과 목록입니다.</p>
            </div>
            <div className="flex gap-2">
              <button className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-xl text-sm font-semibold text-gray-600 bg-white hover:bg-gray-50 transition">
                <Filter size={16} /> 필터링
              </button>
              <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 rounded-xl text-sm font-semibold text-white hover:bg-blue-700 transition shadow-md shadow-blue-100">
                <Download size={16} /> 로그 내보내기
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
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50/80 transition-colors">
                    <td className="py-4 px-4 font-bold text-sm text-gray-900 tracking-wide">{log.displayId}</td>
                    <td className="py-4 px-4 text-sm text-gray-500">{log.timestamp}</td>
                    <td className="py-4 px-4 text-center">
                      <span className={`inline-block text-[10px] font-black px-2.5 py-1 rounded-full uppercase ${
                        log.status === 'SUCCESS' 
                          ? 'bg-blue-50 text-blue-600 border border-blue-100' 
                          : 'bg-red-50 text-red-600 border border-red-100 animate-pulse'
                      }`}>
                        {log.status}
                      </span>
                    </td>
                    <td className="py-4 px-4 text-xs font-mono text-gray-600">{log.diff}</td>
                    <td className="py-4 px-4 text-right">
                      <button className={`text-xs font-bold transition ${
                        log.status === 'TAMPERED' ? 'text-red-600 hover:underline' : 'text-blue-600 hover:underline'
                      }`}>
                        {log.status === 'TAMPERED' ? '위변조 추적' : '상세 정보'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        {/* 하단 상세 추적 레이아웃 */}
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          
          {/* 좌측: FORENSIC TRACK 디테일 패널 (8/12) */}
          <div className="lg:col-span-8 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col gap-6">
            <div>
              <h4 className="text-xs font-bold text-red-500 uppercase tracking-widest">Forensic Track</h4>
              <h3 className="text-xl font-extrabold text-gray-950 mt-1">위변조 정밀 추적 데이터: TX-99712-F</h3>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="bg-gray-50 p-4 rounded-xl border border-gray-200">
                <span className="text-xs font-bold text-gray-400">STORED BLOCKCHAIN HASH (정상 등록 해시)</span>
                <p className="text-xs font-mono text-blue-600 mt-2 break-all bg-white p-2 rounded border border-gray-100">
                  SHA256: 8f7e2c9a4b1d6f3e0c5a7b9d8e2f4a6b1c3d5e7f9a0b2c4d6e8f0a2b4c6d8e0f
                </p>
              </div>
              <div className="bg-red-50/30 p-4 rounded-xl border border-red-100">
                <span className="text-xs font-bold text-red-400">COMPUTED VERIFICATION HASH (현재 변조 해시)</span>
                <p className="text-xs font-mono text-red-600 mt-2 break-all bg-white p-2 rounded border border-red-100">
                  SHA256: 1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b
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
                  해당 거래의 원본 판매 가격(₩42,000)이 데이터베이스 외부 비인가 접근에 의해 <strong className="text-red-600 font-bold">₩32,000</strong>으로 임의 변조 및 수정된 내역이 감지되었습니다. 시스템 백엔드 해시 엔진이 이를 0.002초 만에 즉각 차단하고 무효화 조치했습니다.
                </p>
              </div>
            </div>
          </div>

          {/* 우측: 보안 활동 타임라인 (4/12) */}
          <div className="lg:col-span-4 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col justify-between">
            <div>
              <h4 className="text-sm font-bold text-gray-900 mb-6">보안 활동 타임라인</h4>
              
              <div className="flex flex-col gap-6 pl-2 relative border-l-2 border-gray-100 ml-2">
                {/* 타임라인 항목 1 */}
                <div className="relative">
                  <span className="absolute -left-[15px] top-1.5 w-2 h-2 rounded-full bg-blue-600 border-4 border-white box-content"></span>
                  <div className="flex flex-col gap-0.5 pl-4">
                    <span className="text-xs font-bold text-gray-400 flex items-center gap-1"><Clock size={12} /> 14:30:01</span>
                    <p className="text-sm font-bold text-gray-800">해시 엔진 정기 무결성 검증 통과 (v2.4.1)</p>
                  </div>
                </div>

                {/* 타임라인 항목 2 */}
                <div className="relative">
                  <span className="absolute -left-[15px] top-1.5 w-2 h-2 rounded-full bg-red-500 border-4 border-white box-content"></span>
                  <div className="flex flex-col gap-0.5 pl-4">
                    <span className="text-xs font-bold text-red-400 flex items-center gap-1"><Clock size={12} /> 13:05:44</span>
                    <p className="text-sm font-bold text-red-600">TX-99712-F 거래 외부 변조 공격 즉각 차단 성공</p>
                  </div>
                </div>

                {/* 타임라인 항목 3 */}
                <div className="relative">
                  <span className="absolute -left-[15px] top-1.5 w-2 h-2 rounded-full bg-gray-400 border-4 border-white box-content"></span>
                  <div className="flex flex-col gap-0.5 pl-4">
                    <span className="text-xs font-bold text-gray-400 flex items-center gap-1"><Clock size={12} /> 12:00:00</span>
                    <p className="text-sm font-medium text-gray-500">시스템 전체 데이터 무결성 체크 완료 (Daily Check)</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-8 pt-4 border-t border-gray-100 flex items-center justify-between text-xs font-bold text-gray-600">
              <span>실시간 모니터링 상태</span>
              <span className="text-blue-600">ACTIVE 100%</span>
            </div>
          </div>

        </section>
      </main>
    </div>
  );
};

export default SecurityLogPage;