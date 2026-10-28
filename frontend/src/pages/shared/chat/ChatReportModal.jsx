import React, { useState } from 'react';
import { Flag, X } from 'lucide-react';

// 상대 메시지 1개를 골라 사유와 함께 신고하는 창
const API_BASE = `http://${window.location.hostname}:8080/api`;

// 서버의 신고 사유 목록과 같아야 함
const REPORT_REASONS = ['개인 거래 유도', '선입금 요청', '욕설·비방', '기타'];
const MAX_DETAIL_LENGTH = 1000;

const formatSentAt = (dateTimeStr) =>
  dateTimeStr
    ? new Date(dateTimeStr).toLocaleString('ko-KR', {
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
    })
    : '';

const ChatReportModal = ({ roomId, targetName, message, onClose, onSubmitted }) => {
  const [reason, setReason] = useState(REPORT_REASONS[0]);
  const [detail, setDetail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async () => {
    if (reason === '기타' && !detail.trim()) {
      setError('기타 사유는 상세 내용을 적어주세요.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      // 신고 접수 요청 (신고당한 사람은 보내지 않고 서버가 정함)
      const res = await fetch(`${API_BASE}/chat/rooms/${roomId}/reports`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ messageId: message.id, reason, detail: detail.trim() }),
      });
      if (res.status === 409) throw new Error('이미 신고한 메시지예요.');
      if (!res.ok) {
        const body = await res.json().catch(() => null);
        throw new Error(body?.message || '신고 접수에 실패했습니다.');
      }
      onSubmitted();
    } catch (e) {
      setError(e.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4" onClick={onClose}>
      <div
        className="relative w-full max-w-md bg-white rounded-3xl p-6 flex flex-col gap-4 max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <button onClick={onClose} className="absolute top-4 right-4 text-gray-300 hover:text-gray-500 transition">
          <X size={22} />
        </button>

        <div className="flex items-center gap-2">
          <Flag size={20} className="text-red-500" />
          <h3 className="text-lg font-extrabold text-gray-900">메시지 신고</h3>
        </div>

        {/* 선택한 메시지 미리보기 */}
        <div>
          <p className="text-xs font-bold text-gray-500 mb-1.5">{targetName}님의 메시지</p>
          <div className="bg-gray-50 border border-gray-100 rounded-2xl p-3 text-sm text-gray-700 break-all max-h-36 overflow-y-auto">
            <span className="text-[10px] text-gray-400 mr-1.5">{formatSentAt(message.sentAt)}</span>
            {message.type === 'IMAGE' ? '[사진]' : message.content}
          </div>
        </div>

        {/* 신고 사유 */}
        <div>
          <p className="text-xs font-bold text-gray-500 mb-1.5">신고 사유</p>
          <div className="flex flex-wrap gap-2">
            {REPORT_REASONS.map(r => (
              <button
                key={r}
                type="button"
                onClick={() => setReason(r)}
                className={`px-3 py-1.5 rounded-full text-xs font-bold border transition ${
                  reason === r
                    ? 'bg-red-50 border-[#a04b3c]/40 text-red-600'
                    : 'bg-white border-gray-200 text-gray-500 hover:bg-gray-50'
                }`}
              >
                {r}
              </button>
            ))}
          </div>
        </div>

        {/* 상세 내용 */}
        <div>
          <p className="text-xs font-bold text-gray-500 mb-1.5">
            상세 내용 {reason === '기타' ? '(필수)' : '(선택)'}
          </p>
          <textarea
            value={detail}
            onChange={(e) => setDetail(e.target.value.slice(0, MAX_DETAIL_LENGTH))}
            rows={4}
            placeholder="어떤 상황이었는지 적어주세요"
            className="w-full p-3 text-sm border border-gray-200 rounded-2xl resize-none outline-none focus:ring-2 focus:ring-red-100"
          />
          <p className="text-right text-[10px] text-gray-400">{detail.length}/{MAX_DETAIL_LENGTH}</p>
        </div>

        <p className="text-[11px] text-gray-400 leading-relaxed">
          신고 내용은 운영진만 확인해요. 반복된 허위 신고는 이용이 제한될 수 있어요.
        </p>

        {error && <p className="text-xs font-bold text-red-600">{error}</p>}

        <div className="flex gap-2">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 py-3 rounded-2xl text-sm font-bold text-gray-500 bg-gray-100 hover:bg-gray-200 transition"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={submitting}
            className="flex-1 py-3 rounded-2xl text-sm font-bold text-white bg-red-600 hover:bg-red-700 transition disabled:opacity-50"
          >
            {submitting ? '접수 중...' : '신고하기'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ChatReportModal;
