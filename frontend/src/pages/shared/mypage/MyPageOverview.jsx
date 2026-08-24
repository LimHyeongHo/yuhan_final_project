import React, { useEffect, useState } from 'react';
import { User, ShieldCheck, Store, Edit3, KeyRound, RefreshCw, Clock, Eye, EyeOff, Info, X, Building2, BadgeCheck, ShieldAlert } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useCertificateTimer } from '../../../contexts/CertificateTimerContext';

const DB_NAME = 'PKI_KeyStore';
const STORE_NAME = 'privateKeys';

const formatRemaining = (totalSeconds) => {
  const clamped = Math.max(totalSeconds, 0);
  const m = Math.floor(clamped / 60).toString().padStart(2, '0');
  const s = Math.floor(clamped % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
};

// 실제 값 길이만큼 별표로 마스킹 (자릿수 노출도 최소화하고 싶으면 고정 길이로 바꿔도 됨)
const maskSerial = (serial) => '•'.repeat(String(serial).length);

const formatDateTime = (date) =>
  date.toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' });

const formatDate = (date) =>
  date.toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' });

// 서버가 내려준 LocalDateTime 문자열(오프셋 없음, 서버 로컬 시간 기준)을 그대로 로컬 시간으로 파싱
const parseServerDateTime = (value) => (value ? new Date(value) : null);

const formatDday = (expiresAt) => {
  const diffDays = Math.ceil((expiresAt - new Date()) / (1000 * 60 * 60 * 24));
  if (diffDays < 0) return '만료됨';
  if (diffDays === 0) return 'D-Day';
  return `D-${diffDays}`;
};

// [신규] "인증서 상세보기" 팝업: 발급 상태에 따라 상태 배지 색상을 결정
const getCertStatus = (remainingSeconds) => {
  if (remainingSeconds === null || remainingSeconds === undefined) {
    return { label: '세션 정보 없음', className: 'bg-gray-100 text-gray-500' };
  }
  if (remainingSeconds <= 0) {
    return { label: '만료됨', className: 'bg-red-100 text-red-700' };
  }
  if (remainingSeconds <= 60) {
    return { label: '만료 임박', className: 'bg-red-100 text-red-700' };
  }
  return { label: '정상 사용 가능', className: 'bg-emerald-100 text-emerald-700' };
};

// [신규] CA 인증서 상세 정보 팝업
const CertificateDetailModal = ({ info, remainingSeconds, showSerial, onToggleSerial, onClose }) => {
  const status = getCertStatus(remainingSeconds);
  const sessionExpiresAt = remainingSeconds !== null && remainingSeconds !== undefined
    ? formatDateTime(new Date(Date.now() + remainingSeconds * 1000))
    : null;

  const certIssuedAt = parseServerDateTime(info?.certificateIssuedAt);
  const certExpiresAt = parseServerDateTime(info?.certificateExpiresAt);

  return (
    <div
      className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl shadow-xl border border-gray-200 w-full max-w-md p-6 flex flex-col gap-5"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-extrabold text-gray-900 flex items-center gap-2">
            <BadgeCheck size={20} className="text-purple-600" /> 인증서 정보 상세
          </h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition" aria-label="닫기">
            <X size={20} />
          </button>
        </div>

        <div className="flex flex-col divide-y divide-gray-100 border border-gray-100 rounded-2xl overflow-hidden">
          {[
            ['구분', '개인 (PKI 기기 인증서)'],
            ['발급자 (Issuer)', 'N빵 자체인증기관 (Nbbang CA)'],
            ['가입자 (Subject)', `${info?.nickname ?? '-'} (${info?.email ?? '-'})`],
            ['용도', '로그인 인증, 전자서명 검증'],
          ].map(([label, value]) => (
            <div key={label} className="flex items-start justify-between gap-4 px-4 py-3">
              <span className="text-xs font-bold text-gray-400 shrink-0 pt-0.5">{label}</span>
              <span className="text-sm font-semibold text-gray-900 text-right">{value}</span>
            </div>
          ))}

          <div className="flex items-start justify-between gap-4 px-4 py-3">
            <span className="text-xs font-bold text-gray-400 shrink-0 pt-0.5">유효기간 (인증서)</span>
            <span className="text-sm font-semibold text-gray-900 text-right">
              {certIssuedAt && certExpiresAt ? (
                <>
                  {formatDate(certIssuedAt)} ~ {formatDate(certExpiresAt)}
                  <span className="text-gray-500 font-bold"> ({formatDday(certExpiresAt)} 남음)</span>
                </>
              ) : '-'}
            </span>
          </div>

          <div className="flex items-start justify-between gap-4 px-4 py-3">
            <span className="text-xs font-bold text-gray-400 shrink-0 pt-0.5">로그인 세션</span>
            <span className="text-sm font-semibold text-gray-900 text-right">
              {sessionExpiresAt ? (
                <>
                  {sessionExpiresAt}까지
                  <span className={remainingSeconds <= 60 ? 'text-red-600 font-bold' : 'text-amber-700 font-bold'}>
                    {' '}({formatRemaining(remainingSeconds)} 남음)
                  </span>
                </>
              ) : '-'}
            </span>
          </div>

          <div className="flex items-start justify-between gap-4 px-4 py-3">
            <span className="text-xs font-bold text-gray-400 shrink-0 pt-0.5">일련번호 (Serial)</span>
            <span className="text-sm font-mono font-semibold text-gray-900 text-right break-all flex items-center gap-2 justify-end">
              {info?.certificateSerialNumber
                ? (showSerial ? info.certificateSerialNumber : maskSerial(info.certificateSerialNumber))
                : '-'}
              {info?.certificateSerialNumber && (
                <button type="button" onClick={onToggleSerial} className="text-gray-400 hover:text-gray-600 transition" aria-label={showSerial ? '시리얼 번호 숨기기' : '시리얼 번호 표시'}>
                  {showSerial ? <EyeOff size={14} /> : <Eye size={14} />}
                </button>
              )}
            </span>
          </div>

          <div className="flex items-center justify-between gap-4 px-4 py-3">
            <span className="text-xs font-bold text-gray-400 shrink-0">상태</span>
            <span className={`text-xs font-black px-2.5 py-1 rounded-md flex items-center gap-1 ${status.className}`}>
              {(remainingSeconds !== null && remainingSeconds !== undefined && remainingSeconds <= 60)
                ? <ShieldAlert size={12} /> : <Building2 size={12} />}
              {status.label}
            </span>
          </div>
        </div>

        <p className="text-[11px] text-gray-400 flex items-center gap-1.5">
          <Info size={12} /> 인증서 자체는 발급일로부터 1년간 유효하며, 보안을 위해 로그인 세션은 10분(기본)마다 별도로 만료됩니다.
        </p>
      </div>
    </div>
  );
};

const MyPageOverview = ({ userRole = 'BUYER' }) => {
  const navigate = useNavigate();
  const [info, setInfo] = useState(null);
  const [isWithdrawing, setIsWithdrawing] = useState(false);
  const [isReissuing, setIsReissuing] = useState(false);
  const [showSerial, setShowSerial] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showCertModal, setShowCertModal] = useState(false);
  // [신규] CA 인증서 옆에 남은 유효시간(mm:ss)을 보여주기 위한 전역 타이머 상태
  const { remainingSeconds, syncStatus } = useCertificateTimer();

  const fetchInfo = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/member/info', {
        credentials: 'include',
      });
      const data = await res.json();
      if (res.ok) {
        setInfo(data);
      } else {
        throw new Error(data.error || '회원 정보를 불러오지 못했습니다.');
      }
    } catch (e) {
      alert('회원 정보 조회 오류: ' + e.message);
    }
  };

  useEffect(() => {
    fetchInfo();
  }, []);

  const getDeviceId = () => {
    let deviceId = localStorage.getItem('pki_device_id');
    if (!deviceId) {
      deviceId = 'dev-' + crypto.randomUUID();
      localStorage.setItem('pki_device_id', deviceId);
    }
    return deviceId;
  };

  const openDB = () => {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, 1);
      request.onupgradeneeded = (e) => {
        const db = e.target.result;
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          db.createObjectStore(STORE_NAME);
        }
      };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  };

  const savePrivateKey = async (userId, key) => {
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, 'readwrite');
    tx.objectStore(STORE_NAME).put(key, userId.trim());
    return new Promise((resolve, reject) => {
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  };

  const generateKeyPair = async () => {
    return await window.crypto.subtle.generateKey(
      { name: 'RSASSA-PKCS1-v1_5', modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' },
      true, ['sign', 'verify']
    );
  };

  const exportPublicKey = async (key) => {
    const exported = await window.crypto.subtle.exportKey('spki', key);
    return btoa(String.fromCharCode(...new Uint8Array(exported)));
  };

  // [신규] 본인인증 재실행 없이, 로그인된 상태 그대로 새 키쌍을 만들어 인증서를 즉시 재발급
  const handleReissue = async () => {
    if (!info?.email) return;
    if (!window.confirm('인증서를 재발급하시겠습니까? 기존 인증서는 폐기되고 로그인 세션이 새로 시작됩니다.')) return;

    setIsReissuing(true);
    try {
      const keyPair = await generateKeyPair();
      const pubKey = await exportPublicKey(keyPair.publicKey);
      const deviceId = getDeviceId();

      const res = await fetch('http://localhost:8080/api/member/certificate/reissue', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ publicKey: pubKey, deviceId }),
      });
      const data = await res.json();

      if (res.ok) {
        await savePrivateKey(info.email, keyPair.privateKey);
        setInfo((prev) => ({ ...prev, certificateSerialNumber: data.certificateSerialNumber }));
        await syncStatus(); // 헤더/마이페이지 타이머를 새 10분으로 즉시 반영
        alert('인증서가 재발급되었습니다.');
      } else {
        throw new Error(data.error || '인증서 재발급 중 오류가 발생했습니다.');
      }
    } catch (e) {
      alert('오류: ' + e.message);
    } finally {
      setIsReissuing(false);
    }
  };

  const handleWithdraw = async () => {
    if (!window.confirm('정말로 회원 탈퇴 하시겠습니까?')) return;

    setIsWithdrawing(true);
    try {
      const res = await fetch('http://localhost:8080/api/member/withdraw', {
        method: 'DELETE',
        credentials: 'include',
      });
      const data = await res.json();
      if (res.ok) {
        localStorage.removeItem('user_nickname');
        localStorage.removeItem('user_role');
        alert('회원 탈퇴가 완료되었습니다.');
        navigate('/login');
      } else {
        throw new Error(data.error || '회원 탈퇴 중 오류가 발생했습니다.');
      }
    } catch (e) {
      alert('오류: ' + e.message);
    } finally {
      setIsWithdrawing(false);
    }
  };

  return (
    <div className="bg-white rounded-[32px] p-8 border border-gray-200 shadow-sm flex flex-col gap-8">
      <div className="flex flex-col sm:flex-row items-center sm:items-start justify-between gap-6 pb-8 border-b border-gray-100">
        <div className="flex items-center gap-6">
          <div className="w-24 h-24 rounded-full bg-gray-100 border-2 border-gray-50 flex items-center justify-center shadow-inner overflow-hidden flex-shrink-0 text-gray-400">
            <User size={40} />
          </div>

          <div className="flex flex-col gap-2 text-center sm:text-left">
            <h4 className="text-2xl font-extrabold text-gray-900">{info?.nickname ?? '불러오는 중...'}</h4>
            {userRole === 'SELLER' ? (
              <span className="bg-emerald-100 text-emerald-700 text-[11px] font-black uppercase tracking-wider px-2.5 py-1 rounded-md w-max mx-auto sm:mx-0 flex items-center gap-1">
                <Store size={14} /> SELLER (판매자)
              </span>
            ) : (
              <span className="bg-blue-100 text-blue-700 text-[11px] font-black uppercase tracking-wider px-2.5 py-1 rounded-md w-max mx-auto sm:mx-0 flex items-center gap-1">
                <ShieldCheck size={14} /> BUYER (구매자)
              </span>
            )}
          </div>
        </div>

        <button
          onClick={() => navigate('../settings')} // 상대 경로로 세팅 페이지 이동
          className="px-5 py-2.5 bg-white border border-gray-200 text-gray-700 text-sm font-bold rounded-xl hover:bg-gray-50 hover:text-gray-900 transition flex items-center gap-2 shadow-sm"
        >
          <Edit3 size={16} /> 프로필 수정
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-y-10 gap-x-8 px-2">
        <div className="flex flex-col gap-1.5"><span className="text-xs font-bold text-gray-400 uppercase tracking-wider">닉네임</span><p className="text-base font-semibold text-gray-900">{info?.nickname ?? '-'}</p></div>
        <div className="flex flex-col gap-1.5"><span className="text-xs font-bold text-gray-400 uppercase tracking-wider">로그인 이메일 계정</span><p className="text-base font-semibold text-gray-900">{info?.email ?? '-'}</p></div>
        <div className="flex flex-col gap-1.5"><span className="text-xs font-bold text-gray-400 uppercase tracking-wider">가입 일시</span><p className="text-base font-semibold text-gray-900">{info?.createdAt ?? '-'}</p></div>
        <div className="flex flex-col gap-1.5"><span className="text-xs font-bold text-gray-400 uppercase tracking-wider">비밀번호</span>
          <div className="flex items-center gap-3">
            <p className="text-lg tracking-[0.2em] font-black text-gray-700 mt-1">
              {showPassword ? (info?.password ?? '-') : '********'}
            </p>
            <button
              type="button"
              onClick={() => setShowPassword((prev) => !prev)}
              className="text-gray-400 hover:text-gray-600 transition"
              aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 표시'}
            >
              {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
            <button onClick={() => navigate('../settings')} className="text-[11px] font-bold text-blue-600 bg-blue-50 px-2 py-1 rounded hover:bg-blue-100 transition">변경</button>
          </div>
        </div>
      </div>

      {/* CA 인증서 시리얼 번호 + 남은 유효시간 + 재발급 */}
      <div className="flex flex-col gap-2 px-2 pt-2 border-t border-gray-100">
        <span className="text-xs font-bold text-gray-400 uppercase tracking-wider flex items-center gap-1.5 mt-6">
          <KeyRound size={14} /> CA 인증서 시리얼 번호
        </span>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <p className="text-base font-mono font-semibold text-gray-900 break-all">
              {info?.certificateSerialNumber
                ? (showSerial ? info.certificateSerialNumber : maskSerial(info.certificateSerialNumber))
                : '발급된 인증서가 없습니다.'}
            </p>
            {info?.certificateSerialNumber && (
              <button
                type="button"
                onClick={() => setShowSerial((prev) => !prev)}
                className="text-gray-400 hover:text-gray-600 transition"
                aria-label={showSerial ? '시리얼 번호 숨기기' : '시리얼 번호 표시'}
              >
                {showSerial ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            )}
            {remainingSeconds !== null && remainingSeconds !== undefined && (
              <span className="flex items-center gap-1 text-xs font-bold text-amber-700 bg-amber-50 px-2.5 py-1 rounded-md">
                <Clock size={12} /> {formatRemaining(remainingSeconds)} 남음
              </span>
            )}
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowCertModal(true)}
              disabled={!info?.certificateSerialNumber}
              className="px-4 py-2 bg-white text-gray-700 border border-gray-200 text-xs font-bold rounded-xl hover:bg-gray-50 transition flex items-center gap-1.5 disabled:opacity-50"
            >
              <Info size={14} /> 상세보기
            </button>
            <button
              onClick={handleReissue}
              disabled={isReissuing}
              className="px-4 py-2 bg-purple-50 text-purple-700 border border-purple-200 text-xs font-bold rounded-xl hover:bg-purple-100 transition flex items-center gap-1.5 disabled:opacity-50"
            >
              <RefreshCw size={14} className={isReissuing ? 'animate-spin' : ''} /> {isReissuing ? '재발급 중...' : '인증서 재발급'}
            </button>
          </div>
        </div>
      </div>

      {showCertModal && (
        <CertificateDetailModal
          info={info}
          remainingSeconds={remainingSeconds}
          showSerial={showSerial}
          onToggleSerial={() => setShowSerial((prev) => !prev)}
          onClose={() => setShowCertModal(false)}
        />
      )}

      {/* 회원 탈퇴 */}
      <div className="flex justify-end px-2 pt-4 border-t border-gray-100">
        <button
          onClick={handleWithdraw}
          disabled={isWithdrawing}
          className="px-5 py-2.5 bg-white border border-red-200 text-red-600 text-sm font-bold rounded-xl hover:bg-red-50 transition disabled:opacity-50"
        >
          {isWithdrawing ? '처리 중...' : '회원 탈퇴'}
        </button>
      </div>
    </div>
  );
};

export default MyPageOverview;
