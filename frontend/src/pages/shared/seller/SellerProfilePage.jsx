import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Store, Package, Clock, ThumbsUp, Minus, ThumbsDown, Info, ShieldCheck } from 'lucide-react';
import Header from '../../../components/layout/Header';

const API_BASE = 'http://localhost:8080/api';

// 3단계 감정 표시 메타
const SENTIMENT = {
  LIKE:    { label: '좋아요',     pill: 'text-emerald-700 bg-emerald-100 border-emerald-200', Icon: ThumbsUp },
  SOSO:    { label: '보통이에요', pill: 'text-amber-700 bg-amber-100 border-amber-200',       Icon: Minus },
  DISLIKE: { label: '싫어요',     pill: 'text-red-700 bg-red-100 border-red-200',             Icon: ThumbsDown },
};

const STATUS_LABEL = {
  OPEN: { text: '진행중', cls: 'text-blue-700 bg-blue-100 border-blue-200' },
  CLOSED_SUCCESS: { text: '모집 성공', cls: 'text-emerald-700 bg-emerald-100 border-emerald-200' },
  CLOSED_FAIL: { text: '모집 실패', cls: 'text-red-700 bg-red-100 border-red-200' },
};

const formatDate = (value) =>
  value ? new Date(value).toLocaleDateString('ko-KR') : '-';

const SellerProfilePage = () => {
  const { email } = useParams();
  const navigate = useNavigate();
  const sellerEmail = decodeURIComponent(email || '');

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showSatisfactionInfo, setShowSatisfactionInfo] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError(null);
    fetch(`${API_BASE}/sellers/${encodeURIComponent(sellerEmail)}/profile`)
      .then((res) => {
        if (res.status === 404) throw new Error('존재하지 않는 판매자입니다.');
        if (!res.ok) throw new Error('판매자 프로필을 불러오지 못했습니다.');
        return res.json();
      })
      .then(setProfile)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [sellerEmail]);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <Header />

      <main className="flex-grow max-w-4xl w-full mx-auto p-6 md:p-8 flex flex-col gap-6 mt-4">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-sm font-bold text-gray-400 hover:text-gray-700 transition w-max"
        >
          <ArrowLeft size={16} /> 뒤로
        </button>

        {loading ? (
          <div className="text-center text-gray-500 py-20 text-sm font-bold">판매자 정보를 불러오는 중입니다...</div>
        ) : error ? (
          <div className="text-center text-red-500 py-20 text-sm font-bold">{error}</div>
        ) : (
          <>
            {/* 프로필 헤더 */}
            <div className="bg-white rounded-[32px] p-8 border border-gray-200 shadow-sm flex flex-col gap-6">
              <div className="flex items-center gap-4">
                <div className="w-14 h-14 rounded-full bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600 font-black text-xl">
                  {(profile.nickname || '?').charAt(0)}
                </div>
                <div className="flex flex-col gap-1">
                  <h2 className="text-xl font-extrabold text-gray-900 flex items-center gap-2">
                    {profile.nickname}
                    <span className="text-sm font-bold text-gray-400 flex items-center gap-1">
                      <Store size={14} /> 판매자
                    </span>
                  </h2>
                  <span className="text-xs font-bold text-gray-400">가입일 {formatDate(profile.joinedAt)}</span>
                  <p className="flex items-start gap-1.5 text-[11px] font-medium text-gray-400 leading-relaxed mt-0.5 max-w-lg break-keep text-pretty">
                    <ShieldCheck size={13} className="text-gray-400 shrink-0 mt-0.5" />
                    <span>
                      N빵의 모든 계정은 <b className="font-bold text-gray-600">휴대폰 본인확인</b>과 <b className="font-bold text-gray-600">CA 인증서</b>로 검증되며, 결제·영수증 내역은 블록체인에 기록됩니다.
                    </span>
                  </p>
                </div>
              </div>

              <div className="border-t border-gray-100 pt-6 flex flex-col gap-3">
                <div className="grid grid-cols-3 gap-3">
                  <div className="bg-gray-50 border border-gray-100 rounded-2xl p-4">
                    <button
                      type="button"
                      onClick={() => setShowSatisfactionInfo((v) => !v)}
                      className="flex items-center gap-1 text-[11px] font-extrabold text-gray-400 hover:text-blue-600 transition"
                    >
                      거래 만족도 <Info size={12} />
                    </button>
                    <div className="text-2xl font-black text-gray-900 mt-0.5">
                      {profile.satisfactionRate}<span className="text-sm font-extrabold text-gray-500">%</span>
                    </div>
                  </div>
                  <div className="bg-gray-50 border border-gray-100 rounded-2xl p-4">
                    <div className="text-[11px] font-extrabold text-gray-400">거래 횟수</div>
                    <div className="text-2xl font-black text-gray-900 mt-0.5">
                      {profile.tradeCount}<span className="text-sm font-extrabold text-gray-500">회</span>
                    </div>
                  </div>
                  <div className="bg-gray-50 border border-gray-100 rounded-2xl p-4">
                    <div className="text-[11px] font-extrabold text-gray-400">정상 종료율</div>
                    <div className="text-2xl font-black text-gray-900 mt-0.5">
                      {profile.successRate == null ? '-' : profile.successRate}
                      {profile.successRate != null && <span className="text-sm font-extrabold text-gray-500">%</span>}
                    </div>
                  </div>
                </div>

                {showSatisfactionInfo && (
                  <div className="bg-blue-50/60 border border-blue-100 rounded-2xl p-4 text-xs font-medium text-blue-900 leading-relaxed flex gap-2.5">
                    <Info size={15} className="text-blue-500 shrink-0 mt-0.5" />
                    <span>
                      거래 만족도는 해당 공동구매 참여자로부터 받은 <b className="font-extrabold">좋아요·보통이에요·싫어요</b>로 구분된 평가와
                      후기를 포함해 계산한 신뢰 지표입니다.
                    </span>
                  </div>
                )}
              </div>
            </div>

            {/* 진행한 공동구매 */}
            <div className="bg-white rounded-[32px] p-8 border border-gray-200 shadow-sm flex flex-col gap-4">
              <div className="flex items-center gap-3 pb-4 border-b border-gray-100">
                <Package className="text-blue-500" size={22} />
                <h3 className="text-lg font-bold text-gray-900">진행한 공동구매</h3>
              </div>
              {profile.purchases.length === 0 ? (
                <div className="text-center text-gray-400 py-8 text-sm font-bold">진행한 공동구매가 없습니다.</div>
              ) : (
                profile.purchases.map((p) => {
                  const st = STATUS_LABEL[p.status] || { text: p.status, cls: 'text-gray-600 bg-gray-100 border-gray-200' };
                  const pct = p.targetCount > 0 ? Math.min(100, Math.round((p.currentCount / p.targetCount) * 100)) : 0;
                  return (
                    <div
                      key={p.productId}
                      onClick={() => navigate(`/buyer/products/${p.productId}`)}
                      className="p-5 rounded-2xl border border-gray-100 hover:border-blue-200 hover:shadow-sm transition cursor-pointer flex flex-col gap-2"
                    >
                      <span className={`text-[11px] font-black w-max px-2.5 py-1 rounded-md border ${st.cls}`}>{st.text}</span>
                      <h4 className="text-base font-extrabold text-gray-900">{p.title}</h4>
                      <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full ${p.status === 'CLOSED_SUCCESS' ? 'bg-emerald-500' : p.status === 'CLOSED_FAIL' ? 'bg-red-400' : 'bg-blue-600'}`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                      <div className="flex items-center gap-2 text-xs font-medium text-gray-400">
                        <span>{p.currentCount} / {p.targetCount}명</span>
                        <span className="text-gray-300">|</span>
                        <Clock size={12} />
                        <span>{formatDate(p.deadline)}</span>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* 거래 후기 */}
            <div className="bg-white rounded-[32px] p-8 border border-gray-200 shadow-sm flex flex-col gap-4">
              <div className="flex items-center justify-between pb-4 border-b border-gray-100">
                <h3 className="text-lg font-bold text-gray-900">거래 후기 <span className="text-gray-400">{profile.totalReviews}</span></h3>
              </div>
              {profile.reviews.length === 0 ? (
                <div className="text-center text-gray-400 py-8 text-sm font-bold">아직 등록된 거래 후기가 없습니다.</div>
              ) : (
                profile.reviews.map((r, idx) => {
                  const meta = SENTIMENT[r.sentiment] || SENTIMENT.SOSO;
                  const MIcon = meta.Icon;
                  return (
                    <div key={idx} className="p-5 rounded-2xl border border-gray-100 flex flex-col gap-2">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className={`inline-flex items-center gap-1 text-[11px] font-black px-2.5 py-1 rounded-md border ${meta.pill}`}>
                          <MIcon size={12} strokeWidth={2.5} />{meta.label}
                        </span>
                        <span className="text-xs font-bold text-gray-400">익명 (구매자)</span>
                        <span className="text-xs font-medium text-gray-300">· {formatDate(r.createdAt)}</span>
                      </div>
                      {r.content && <p className="text-sm text-gray-600 leading-relaxed">{r.content}</p>}
                    </div>
                  );
                })
              )}
            </div>
          </>
        )}
      </main>
    </div>
  );
};

export default SellerProfilePage;
