import React, { useState, useEffect, useCallback } from 'react';
import { Package, ChevronRight, Clock, Star, X, ThumbsUp, Minus, ThumbsDown } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const API_BASE = 'http://localhost:8080/api';

// 3단계 감정 메타 (백엔드 Sentiment enum 과 1:1) — 아이콘은 lucide 라인 아이콘(초록/노랑/빨강)
const SENTIMENTS = [
  { key: 'LIKE', Icon: ThumbsUp, label: '좋아요', color: 'text-emerald-600', on: 'border-emerald-500 bg-emerald-50 text-emerald-700' },
  { key: 'SOSO', Icon: Minus, label: '보통이에요', color: 'text-amber-500', on: 'border-amber-500 bg-amber-50 text-amber-700' },
  { key: 'DISLIKE', Icon: ThumbsDown, label: '싫어요', color: 'text-red-500', on: 'border-red-500 bg-red-50 text-red-700' },
];
const sentimentMeta = (key) => SENTIMENTS.find((s) => s.key === key) || SENTIMENTS[1];

// 후기 작성/수정 모달
const ReviewFormModal = ({ target, onClose, onSaved }) => {
  const isEdit = target.mode === 'edit';
  const [sentiment, setSentiment] = useState(isEdit ? target.review.sentiment : null);
  const [content, setContent] = useState(isEdit ? target.review.content || '' : '');
  const [submitting, setSubmitting] = useState(false);

  const submit = async () => {
    if (!sentiment) {
      alert('이번 거래가 어땠는지 선택해주세요.');
      return;
    }
    setSubmitting(true);
    try {
      const url = isEdit ? `${API_BASE}/reviews/${target.review.id}` : `${API_BASE}/reviews`;
      const method = isEdit ? 'PUT' : 'POST';
      const body = isEdit
        ? { sentiment, content }
        : { productId: target.productId, sentiment, content };
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(body),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.message || '후기 저장에 실패했습니다.');
      onSaved();
    } catch (err) {
      alert(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <div className="bg-white rounded-3xl p-7 w-full max-w-md flex flex-col gap-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-start justify-between">
          <div>
            <h3 className="text-lg font-black text-gray-900">거래 후기 {isEdit ? '수정' : '작성'}</h3>
            <p className="text-xs font-bold text-gray-400 mt-1">{target.title}</p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600"><X size={20} /></button>
        </div>

        <div>
          <div className="text-xs font-extrabold text-gray-700 mb-2">이번 거래는 어떠셨나요?</div>
          <div className="grid grid-cols-3 gap-2.5">
            {SENTIMENTS.map((s) => {
              const Icon = s.Icon;
              return (
                <button
                  key={s.key}
                  type="button"
                  onClick={() => setSentiment(s.key)}
                  className={`rounded-2xl border-2 py-4 flex flex-col items-center gap-2 transition ${
                    sentiment === s.key ? s.on : 'border-gray-200 bg-white text-gray-500 hover:border-gray-300'
                  }`}
                >
                  <Icon size={22} strokeWidth={2.25} className={s.color} />
                  <span className="text-xs font-black">{s.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        <div>
          <div className="text-xs font-extrabold text-gray-700 mb-2">한 줄 후기 <span className="text-gray-400 font-bold">(선택)</span></div>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            maxLength={500}
            rows={3}
            placeholder="모집 진행·금액 안내·마감 관리 등 공동구매 진행 경험을 적어주세요. 상품평이 아니라 판매자의 진행에 대한 후기예요."
            className="w-full rounded-xl border border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-700 resize-none focus:outline-none focus:border-blue-300"
          />
        </div>

        <div className="flex gap-2.5 mt-1">
          <button
            onClick={submit}
            disabled={submitting}
            className="flex-1 py-3.5 rounded-2xl font-black text-sm bg-blue-600 text-white hover:bg-blue-700 transition disabled:opacity-60"
          >
            {isEdit ? '수정 저장' : '후기 등록'}
          </button>
          <button onClick={onClose} className="py-3.5 px-5 rounded-2xl font-bold text-sm text-gray-700 bg-white border-2 border-gray-200 hover:bg-gray-50 transition">
            취소
          </button>
        </div>
      </div>
    </div>
  );
};

const MyPageOrders = () => {
  const navigate = useNavigate();
  const [orderList, setOrderList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [reviewMap, setReviewMap] = useState({}); // { [productId]: { eligible, myReview, reason } }
  const [activeForm, setActiveForm] = useState(null); // { productId, title, mode, review }

  const loadOrders = useCallback(() => {
    fetch(`${API_BASE}/products/participations/me`, { credentials: 'include' })
      .then((res) => {
        if (res.status === 401) throw new Error('로그인이 필요합니다.');
        if (!res.ok) throw new Error('주문 내역을 불러오는데 실패했습니다.');
        return res.json();
      })
      .then((data) => {
        const formatted = data.map((part) => {
          const product = part.product || {};
          let statusLabel = '진행중';
          let colorClass = 'text-blue-700 bg-blue-100 border-blue-200';

          if (product.status === 'CLOSED_SUCCESS') {
            statusLabel = '모집 성공(정산 중)';
            colorClass = 'text-emerald-700 bg-emerald-100 border-emerald-200';
          } else if (product.status === 'CLOSED_FAIL') {
            statusLabel = '모집 실패(환불)';
            colorClass = 'text-red-700 bg-red-100 border-red-200';
          } else if (product.status === 'OPEN' && product.currentCount >= product.targetCount) {
            statusLabel = '목표 달성';
            colorClass = 'text-emerald-700 bg-emerald-100 border-emerald-200';
          }

          return {
            id: part.id,
            productId: product.productId ?? product.id,
            rawStatus: product.status,
            title: product.title || '알 수 없는 상품',
            date: new Date(part.joinDate).toLocaleDateString('ko-KR'),
            status: statusLabel,
            price: (product.price || 0).toLocaleString() + '원',
            color: colorClass,
          };
        });
        setOrderList(formatted);
        return formatted;
      })
      .then((formatted) => {
        // 정상 종료된 건에 대해서만 후기 작성 자격 조회
        formatted
          .filter((o) => o.rawStatus === 'CLOSED_SUCCESS' && o.productId != null)
          .forEach((o) => {
            fetch(`${API_BASE}/reviews/eligibility?productId=${o.productId}`, { credentials: 'include' })
              .then((res) => (res.ok ? res.json() : null))
              .then((elig) => {
                if (!elig) return;
                setReviewMap((prev) => ({ ...prev, [o.productId]: elig }));
              })
              .catch(() => {});
          });
      })
      .catch((err) => {
        console.error(err);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  const refreshEligibility = (productId) => {
    fetch(`${API_BASE}/reviews/eligibility?productId=${productId}`, { credentials: 'include' })
      .then((res) => (res.ok ? res.json() : null))
      .then((elig) => elig && setReviewMap((prev) => ({ ...prev, [productId]: elig })))
      .catch(() => {});
  };

  const handleDelete = async (review, productId) => {
    if (!window.confirm('이 후기를 삭제할까요? 삭제하면 이 공동구매에 후기를 다시 작성할 수 있어요.')) return;
    try {
      const res = await fetch(`${API_BASE}/reviews/${review.id}`, { method: 'DELETE', credentials: 'include' });
      if (!res.ok && res.status !== 204) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message || '후기 삭제에 실패했습니다.');
      }
      refreshEligibility(productId);
    } catch (err) {
      alert(err.message);
    }
  };

  const renderReviewControls = (order) => {
    const state = reviewMap[order.productId];
    if (order.rawStatus !== 'CLOSED_SUCCESS' || !state) return null;

    if (state.myReview) {
      const m = sentimentMeta(state.myReview.sentiment);
      const MIcon = m.Icon;
      return (
        <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
          <span className="inline-flex items-center gap-1 text-[11px] font-black px-2 py-1 rounded-md bg-gray-100 text-gray-600 border border-gray-200">
            내 후기 <MIcon size={11} strokeWidth={2.5} className={m.color} /> {m.label}
          </span>
          <button
            onClick={() => setActiveForm({ productId: order.productId, title: order.title, mode: 'edit', review: state.myReview })}
            className="text-xs font-bold text-gray-600 border border-gray-200 rounded-lg px-3 py-1.5 hover:bg-gray-50"
          >
            수정
          </button>
          <button
            onClick={() => handleDelete(state.myReview, order.productId)}
            className="text-xs font-bold text-red-600 border border-red-200 rounded-lg px-3 py-1.5 hover:bg-red-50"
          >
            삭제
          </button>
        </div>
      );
    }
    if (state.eligible) {
      return (
        <button
          onClick={(e) => { e.stopPropagation(); setActiveForm({ productId: order.productId, title: order.title, mode: 'create' }); }}
          className="flex items-center gap-1.5 text-xs font-black text-white bg-blue-600 hover:bg-blue-700 rounded-xl px-3.5 py-2 transition"
        >
          <Star size={13} /> 후기 작성
        </button>
      );
    }
    return null;
  };

  return (
    <div className="bg-white rounded-[32px] p-8 border border-gray-200 shadow-sm flex flex-col gap-6">
      <div className="flex items-center gap-3 pb-6 border-b border-gray-100">
        <Package className="text-blue-500" size={24} />
        <h4 className="text-lg font-bold text-gray-900">최근 참여 내역</h4>
      </div>

      <div className="flex flex-col gap-4">
        {loading ? (
          <div className="text-center text-gray-500 py-8 text-sm font-bold">참여 내역을 불러오는 중입니다...</div>
        ) : orderList.length === 0 ? (
          <div className="text-center text-gray-400 py-8 text-sm font-bold">아직 참여한 공동구매 내역이 없습니다.</div>
        ) : (
          orderList.map((order) => (
            <div
              key={order.id}
              onClick={() => navigate(`/buyer/products/${order.productId}`)}
              className="flex items-center justify-between p-5 rounded-2xl border border-gray-100 hover:border-blue-200 hover:shadow-sm transition cursor-pointer group gap-4"
            >
              <div className="flex flex-col gap-2 min-w-0">
                <span className={`text-[11px] font-black w-max px-2.5 py-1 rounded-md ${order.color}`}>
                  {order.status}
                </span>
                <h5 className="text-base font-extrabold text-gray-900 group-hover:text-blue-600 transition truncate">{order.title}</h5>
                <div className="flex items-center gap-2 text-xs font-medium text-gray-400">
                  <Clock size={12} />
                  <span>결제일: {order.date}</span>
                  <span className="text-gray-300">|</span>
                  <span className="text-gray-700 font-bold">{order.price}</span>
                </div>
              </div>
              <div className="flex items-center gap-3 shrink-0">
                {renderReviewControls(order)}
                <ChevronRight className="text-gray-300 group-hover:text-blue-500 transition" size={20} />
              </div>
            </div>
          ))
        )}
      </div>

      {activeForm && (
        <ReviewFormModal
          target={activeForm}
          onClose={() => setActiveForm(null)}
          onSaved={() => { setActiveForm(null); refreshEligibility(activeForm.productId); }}
        />
      )}
    </div>
  );
};

export default MyPageOrders;
