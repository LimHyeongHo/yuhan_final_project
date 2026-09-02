// ===== 26.08.31 건우 추가내용 시작 =====
// 정산/출금 기능 실제 연동: 하드코딩된 값(125,000원, 동작 없는 버튼)을 실제 API 호출로 교체.
// 실제 은행 송금 자동화는 없음 - 신청 접수 후 관리자(/admin/settlements)가 수동 승인하는 방식.
import React, { useState, useEffect } from 'react';
import { CreditCard, Landmark, AlertCircle, CheckCircle2, Clock, XCircle } from 'lucide-react';

const API_BASE = 'http://localhost:8080/api';

const STATUS_LABEL = {
  REQUESTED: { text: '신청중', className: 'bg-amber-50 text-amber-700', icon: Clock },
  COMPLETED: { text: '완료', className: 'bg-emerald-50 text-emerald-700', icon: CheckCircle2 },
  REJECTED: { text: '거절', className: 'bg-red-50 text-red-700', icon: XCircle },
};

const MyPageSettlement = () => {
  const [account, setAccount] = useState(null);
  const [withdrawable, setWithdrawable] = useState(0);
  const [history, setHistory] = useState([]);
  const [editingAccount, setEditingAccount] = useState(false);
  const [accountForm, setAccountForm] = useState({ bankName: '', accountNumber: '', accountHolder: '' });
  const [withdrawAmount, setWithdrawAmount] = useState('');
  const [requesting, setRequesting] = useState(false);

  const fetchAll = () => {
    fetch(`${API_BASE}/settlement/account`, { credentials: 'include' })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => setAccount(data))
      .catch((err) => console.error('정산 계좌 조회 실패:', err));

    fetch(`${API_BASE}/settlement/withdrawable`, { credentials: 'include' })
      .then((res) => (res.ok ? res.json() : Promise.reject(new Error(`출금 가능액 조회 실패 (HTTP ${res.status})`))))
      .then((data) => setWithdrawable(data.withdrawableAmount ?? 0))
      .catch((err) => {
        console.error(err);
        setWithdrawable(0);
      });

    fetch(`${API_BASE}/settlement/history`, { credentials: 'include' })
      .then((res) => (res.ok ? res.json() : Promise.reject(new Error(`출금 내역 조회 실패 (HTTP ${res.status})`))))
      .then((data) => setHistory(Array.isArray(data) ? data : []))
      .catch((err) => {
        console.error(err);
        setHistory([]);
      });
  };

  useEffect(() => {
    fetchAll();
  }, []);

  const startEditAccount = () => {
    setAccountForm(account ?? { bankName: '', accountNumber: '', accountHolder: '' });
    setEditingAccount(true);
  };

  const saveAccount = () => {
    if (!accountForm.bankName.trim() || !accountForm.accountNumber.trim() || !accountForm.accountHolder.trim()) {
      alert('은행명, 계좌번호, 예금주를 모두 입력해주세요.');
      return;
    }
    fetch(`${API_BASE}/settlement/account`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(accountForm),
    })
      .then((res) => {
        if (!res.ok) throw new Error('계좌 등록/변경에 실패했습니다.');
        return res.json();
      })
      .then((data) => {
        setAccount(data);
        setEditingAccount(false);
      })
      .catch((err) => {
        console.error(err);
        alert(err.message);
      });
  };

  const handleWithdraw = () => {
    const amount = Number(withdrawAmount);
    if (!amount || amount <= 0) {
      alert('출금할 금액을 입력해주세요.');
      return;
    }
    if (amount > withdrawable) {
      alert('출금 가능 금액을 초과했습니다.');
      return;
    }
    if (!account) {
      alert('먼저 정산 계좌를 등록해주세요.');
      return;
    }

    setRequesting(true);
    fetch(`${API_BASE}/settlement/withdraw`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ amount }),
    })
      .then((res) => {
        if (!res.ok) throw new Error('출금 신청에 실패했습니다.');
        alert('출금 신청이 접수되었습니다. 관리자 승인 후 처리됩니다.');
        setWithdrawAmount('');
        fetchAll();
      })
      .catch((err) => {
        console.error(err);
        alert(err.message);
      })
      .finally(() => setRequesting(false));
  };

  return (
    <div className="bg-white rounded-[32px] p-8 border border-gray-200 shadow-sm flex flex-col gap-8">
      <div className="pb-6 border-b border-gray-100 flex justify-between items-end">
        <div>
          <h4 className="text-lg font-bold text-gray-900 flex items-center gap-2">
            <CreditCard className="text-emerald-500" size={24} /> 정산 관리
          </h4>
          <p className="text-xs text-gray-400 font-medium mt-1">안전한 거래를 위한 정산 계좌와 누적 수익을 확인하세요.</p>
        </div>
      </div>

      {/* 출금 가능 정산금 카드 */}
      <div className="bg-emerald-50 rounded-2xl p-6 border border-emerald-100 flex flex-col gap-2">
        <span className="text-sm font-bold text-emerald-800">출금 가능 정산금</span>
        <div className="flex items-baseline gap-1">
          <h2 className="text-3xl font-black text-emerald-900">{withdrawable.toLocaleString()}</h2>
          <span className="text-lg font-bold text-emerald-700">원</span>
        </div>
        <div className="mt-2 flex items-center gap-2">
          <input
            type="number"
            value={withdrawAmount}
            onChange={(e) => setWithdrawAmount(e.target.value)}
            placeholder="출금할 금액"
            className="w-40 px-3 py-2 border border-emerald-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-300"
          />
          <button
            onClick={handleWithdraw}
            disabled={requesting}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-bold rounded-xl transition shadow-sm disabled:opacity-50"
          >
            출금 신청
          </button>
        </div>
        <div className="flex items-start gap-1.5 mt-1">
          <AlertCircle size={12} className="text-emerald-500 mt-0.5 shrink-0" />
          <p className="text-[11px] text-emerald-700">
            승인 후 처리됩니다. 실제 은행 송금은 관리자 확인을 거쳐 이뤄지며, 테스트 환경에서는 실제 입금이 발생하지 않습니다.
          </p>
        </div>
      </div>

      {/* 등록된 정산 계좌 */}
      <div className="flex flex-col gap-4 mt-2">
        <h5 className="text-sm font-bold text-gray-900 flex items-center gap-1.5">
          <Landmark size={16} className="text-gray-500" /> 내 정산 계좌
        </h5>

        {editingAccount ? (
          <div className="p-5 border border-gray-200 rounded-2xl flex flex-col gap-3">
            <input
              type="text"
              placeholder="은행명 (예: 카카오뱅크)"
              value={accountForm.bankName}
              onChange={(e) => setAccountForm({ ...accountForm, bankName: e.target.value })}
              className="px-3 py-2 border border-gray-200 rounded-lg text-sm"
            />
            <input
              type="text"
              placeholder="계좌번호"
              value={accountForm.accountNumber}
              onChange={(e) => setAccountForm({ ...accountForm, accountNumber: e.target.value })}
              className="px-3 py-2 border border-gray-200 rounded-lg text-sm font-mono"
            />
            <input
              type="text"
              placeholder="예금주"
              value={accountForm.accountHolder}
              onChange={(e) => setAccountForm({ ...accountForm, accountHolder: e.target.value })}
              className="px-3 py-2 border border-gray-200 rounded-lg text-sm"
            />
            <div className="flex gap-2 justify-end">
              <button
                onClick={() => setEditingAccount(false)}
                className="px-3 py-1.5 text-xs font-bold text-gray-500 border border-gray-200 rounded-lg hover:bg-gray-50"
              >
                취소
              </button>
              <button
                onClick={saveAccount}
                className="px-3 py-1.5 text-xs font-bold text-white bg-emerald-600 rounded-lg hover:bg-emerald-700"
              >
                저장
              </button>
            </div>
          </div>
        ) : account ? (
          <div className="p-5 border border-gray-200 rounded-2xl flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center">
                <Landmark size={20} className="text-gray-400" />
              </div>
              <div className="flex flex-col">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-extrabold text-gray-900">{account.bankName}</span>
                  <span className="bg-gray-100 text-gray-600 text-[10px] px-1.5 py-0.5 rounded font-bold">주계좌</span>
                </div>
                <span className="text-xs text-gray-500 font-medium font-mono mt-0.5">{account.accountNumber}</span>
              </div>
            </div>
            <button
              onClick={startEditAccount}
              className="text-xs font-bold text-gray-500 hover:text-gray-900 px-3 py-1.5 border border-gray-200 rounded-lg transition"
            >
              계좌 변경
            </button>
          </div>
        ) : (
          <div className="p-5 border border-dashed border-gray-200 rounded-2xl flex items-center justify-between">
            <span className="text-sm text-gray-400 font-medium">등록된 정산 계좌가 없습니다.</span>
            <button
              onClick={startEditAccount}
              className="text-xs font-bold text-white bg-gray-900 px-3 py-1.5 rounded-lg transition hover:bg-gray-700"
            >
              계좌 등록
            </button>
          </div>
        )}

        <div className="flex items-start gap-1.5 mt-2 bg-gray-50 p-3 rounded-xl">
          <AlertCircle size={14} className="text-gray-400 mt-0.5 shrink-0" />
          <p className="text-[11px] text-gray-500 leading-relaxed">
            정산 계좌는 본인 명의의 계좌만 등록 가능하며, 계좌 변경 시 관리자의 승인이 필요할 수 있습니다. 출금 신청 후 입금까지 영업일 기준 1~2일이 소요됩니다.
          </p>
        </div>
      </div>

      {/* 출금 신청 내역 */}
      <div className="flex flex-col gap-4 mt-2">
        <h5 className="text-sm font-bold text-gray-900 flex items-center gap-1.5">
          <Clock size={16} className="text-gray-500" /> 출금 신청 내역
        </h5>

        {history.length === 0 ? (
          <div className="p-6 text-center text-gray-400 text-sm font-medium border border-dashed border-gray-200 rounded-2xl">
            출금 신청 내역이 없습니다.
          </div>
        ) : (
          <div className="flex flex-col border border-gray-200 rounded-2xl overflow-hidden">
            {history.map((item) => {
              const statusInfo = STATUS_LABEL[item.status] ?? STATUS_LABEL.REQUESTED;
              const StatusIcon = statusInfo.icon;
              return (
                <div
                  key={item.id}
                  className="flex items-center justify-between p-4 border-b border-gray-100 last:border-b-0"
                >
                  <span className="text-xs text-gray-500 font-mono">
                    {item.requestedAt ? new Date(item.requestedAt).toLocaleDateString() : '-'}
                  </span>
                  <span className="text-sm font-bold text-gray-900">{item.amount.toLocaleString()}원</span>
                  <span
                    className={`flex items-center gap-1 text-[11px] font-bold px-2.5 py-1 rounded-full ${statusInfo.className}`}
                  >
                    <StatusIcon size={12} /> {statusInfo.text}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default MyPageSettlement;
// ===== 26.08.31 건우 추가내용 끝 =====
