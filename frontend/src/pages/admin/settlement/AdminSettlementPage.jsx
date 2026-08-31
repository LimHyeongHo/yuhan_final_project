// ===== 26.08.31 건우 추가내용 시작 =====
import React, { useState, useEffect } from 'react';
import { Wallet, Landmark, Clock, CheckCircle2 } from 'lucide-react';
import Header from '../../../components/layout/Header';

const API_BASE = 'http://localhost:8080/api';

const WithdrawalRow = ({ item, onApprove, onReject }) => (
  <div className="flex items-center justify-between p-5 border-b border-gray-100 last:border-b-0 hover:bg-gray-50/50 transition">
    <div className="flex flex-col gap-1">
      <span className="font-bold text-gray-900 text-sm">{item.sellerEmail}</span>
      <span className="text-xs text-gray-500">
        {item.bankName} {item.accountNumber} ({item.accountHolder})
      </span>
    </div>

    <div className="flex items-center gap-8">
      <div className="hidden md:flex flex-col text-right">
        <span className="text-xs font-bold text-gray-400">신청 금액</span>
        <span className="text-base font-black text-gray-900">{item.amount.toLocaleString()}원</span>
      </div>
      <div className="flex gap-2">
        <button
          onClick={() => onReject(item.id)}
          className="px-4 py-2 border border-gray-200 text-gray-600 rounded-xl text-sm font-bold hover:bg-gray-100 transition"
        >
          거절
        </button>
        <button
          onClick={() => onApprove(item.id)}
          className="px-4 py-2 bg-emerald-600 text-white rounded-xl text-sm font-bold shadow-md shadow-emerald-200 hover:bg-emerald-700 transition"
        >
          승인
        </button>
      </div>
    </div>
  </div>
);

const AdminSettlementPage = () => {
  const [pendingWithdrawals, setPendingWithdrawals] = useState([]);

  const fetchPendingWithdrawals = () => {
    fetch(`${API_BASE}/admin/settlements`, { credentials: 'include' })
      .then((res) => res.json())
      .then((data) => setPendingWithdrawals(data))
      .catch((err) => console.error('출금 신청 목록 로드 실패:', err));
  };

  useEffect(() => {
    fetchPendingWithdrawals();
  }, []);

  const handleApprove = (id) => {
    fetch(`${API_BASE}/admin/settlements/${id}/approve`, { method: 'POST', credentials: 'include' })
      .then((res) => {
        if (!res.ok) throw new Error('승인 처리에 실패했습니다.');
        fetchPendingWithdrawals();
      })
      .catch((err) => {
        console.error(err);
        alert(err.message);
      });
  };

  const handleReject = (id) => {
    fetch(`${API_BASE}/admin/settlements/${id}/reject`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    })
      .then((res) => {
        if (!res.ok) throw new Error('거절 처리에 실패했습니다.');
        fetchPendingWithdrawals();
      })
      .catch((err) => {
        console.error(err);
        alert(err.message);
      });
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <Header />

      <section className="bg-slate-900 text-white py-12 px-6 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-col gap-2">
          <span className="bg-emerald-500/20 text-emerald-400 text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full w-max border border-emerald-500/30">
            Settlement
          </span>
          <h2 className="text-4xl font-extrabold tracking-tight">출금 신청 관리</h2>
          <p className="text-slate-400 font-medium text-base max-w-2xl mt-1">
            판매자가 신청한 출금 건을 확인하고 승인/거절 처리합니다. 실제 은행 송금 자동화는 없으며,
            승인 시 관리자가 직접 계좌로 입금했다는 것을 확인하는 절차입니다.
          </p>
        </div>
      </section>

      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col gap-8">
        <section className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">승인 대기 중</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">{pendingWithdrawals.length} 건</h3>
              <span className="text-emerald-600 text-xs font-bold mt-1 flex items-center gap-1">
                <Clock size={14} /> ACTION REQUIRED
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-600">
              <Wallet size={24} />
            </div>
          </div>

          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">대기 중 총액</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">
                {pendingWithdrawals.reduce((sum, w) => sum + w.amount, 0).toLocaleString()}원
              </h3>
            </div>
            <div className="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center text-blue-600">
              <Landmark size={24} />
            </div>
          </div>
        </section>

        <div className="bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col">
          <div className="flex justify-between items-center mb-6">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <CheckCircle2 size={18} />
              </div>
              <h3 className="text-xl font-extrabold text-gray-950 tracking-tight">출금 신청 목록</h3>
            </div>
            <span className="bg-emerald-50 text-emerald-600 text-xs font-bold px-3 py-1.5 rounded-lg">
              {pendingWithdrawals.length}건 대기 중
            </span>
          </div>

          <div className="flex flex-col border-t border-gray-100">
            {pendingWithdrawals.length === 0 ? (
              <div className="p-8 text-center text-gray-400 font-bold">승인 대기 중인 출금 신청이 없습니다.</div>
            ) : (
              pendingWithdrawals.map((item) => (
                <WithdrawalRow key={item.id} item={item} onApprove={handleApprove} onReject={handleReject} />
              ))
            )}
          </div>
        </div>
      </main>
    </div>
  );
};

export default AdminSettlementPage;
// ===== 26.08.31 건우 추가내용 끝 =====
