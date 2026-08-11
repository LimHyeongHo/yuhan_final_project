import React, { useState } from 'react';

/// [*] 회원가입 페이지와 연결
import { Link, useNavigate } from 'react-router-dom';

/// [*] 헤더 컴포넌트 연결
import Header from '../../components/layout/Header';

/// [신규] 로그인 성공 직후 인증서 타이머(10분)를 서버와 동기화하기 위한 훅
import { useCertificateTimer } from '../../contexts/CertificateTimerContext';

const backgroundImgUrl = "https://images.unsplash.com/photo-1589998059171-988d887df646?q=80&w=2600";

const DB_NAME = "PKI_KeyStore";
const STORE_NAME = "privateKeys";

const LoginPage = () => {
  const navigate = useNavigate();
  const [mode, setMode] = useState('user'); // 'user' | 'admin'
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isVerified, setIsVerified] = useState(false);
  const [regCi, setRegCi] = useState(null);
  const [privateKey, setPrivateKey] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [needsReissue, setNeedsReissue] = useState(false);
  // [신규] 테스트 로그인 탭에서 선택한 역할 힌트 ('seller' | 'buyer' | 'admin' | null)
  const [testRoleHint, setTestRoleHint] = useState(null);
  // [신규] 로그인 성공 후 인증서 타이머 상태를 갱신하기 위해 Context에서 syncStatus를 꺼내옴
  const { syncStatus } = useCertificateTimer();

  // [신규] 역할별 테스트 계정 안내 (자동입력 아님, 참고용 텍스트만 보여줌)
  const TEST_ROLE_HINTS = {
    seller: [
      { email: 'seller01@test.com', nickname: '판매자1', password: '1234' },
      { email: 'seller02@test.com', nickname: '판매자2', password: '1234' },
    ],
    buyer: [
      { email: 'buyer01@test.com', nickname: '구매자1', password: '1234' },
      { email: 'buyer02@test.com', nickname: '구매자2', password: '1234' },
    ],
    admin: [
      { email: 'admin@naver.com', nickname: '관리자', password: 'admin1234' },
    ],
  };

  const handleAdminLogin = async (e) => {
    e.preventDefault();
    if (!email || !password) return alert("이메일과 비밀번호를 입력해주세요.");
    setIsLoading(true);
    try {
      const res = await fetch('http://localhost:8080/api/pki/admin/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        // [수정] 세션 쿠키를 주고받기 위해 credentials 추가
        credentials: 'include',
        body: JSON.stringify({ email, password })
      });
      const result = await res.json();
      if (res.ok) {
        localStorage.setItem('user_nickname', result.nickname);
        localStorage.setItem('user_role', result.role);
        // [신규] 관리자는 인증서 타이머가 없지만, 혹시 있을 상태를 정리하기 위해 동기화 호출
        await syncStatus();
        alert(`${result.nickname}님 환영합니다!`);
        navigate('/');
      } else {
        throw new Error(result.message);
      }
    } catch (e) {
      alert("로그인 오류: " + e.message);
      window.location.reload();
    } finally {
      setIsLoading(false);
    }
  };

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
    const tx = db.transaction(STORE_NAME, "readwrite");
    tx.objectStore(STORE_NAME).put(key, userId.trim());
    return new Promise((resolve, reject) => {
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  };

  const loadPrivateKey = async (userId) => {
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, "readonly");
    const store = tx.objectStore(STORE_NAME);
    const request = store.get(userId.trim());
    return new Promise((resolve, reject) => {
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  };

  const generateKeyPair = async () => {
    return await window.crypto.subtle.generateKey(
      { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
      true, ["sign", "verify"]
    );
  };

  const exportPublicKey = async (key) => {
    const exported = await window.crypto.subtle.exportKey("spki", key);
    return btoa(String.fromCharCode(...new Uint8Array(exported)));
  };

  // 1. 본인인증 실행
  const handleVerify = async (e) => {
    e.preventDefault();
    if (!email) return alert("이메일을 입력해주세요.");

    setNeedsReissue(false);
    setIsLoading(true);
    try {
      const response = await window.PortOne.requestIdentityVerification({
        storeId: "store-fd77dc69-cea9-4fd9-83c9-9888650fe579",
        channelKey: "channel-key-8ffdf987-acaa-40a7-bac8-c8000aab12f2",
        identityVerificationId: `verif-${Date.now()}`
      });

      if (response.code !== undefined) throw new Error(response.message);

      const verifyRes = await fetch('http://localhost:8080/api/pki/verify-portone', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ identityVerificationId: response.identityVerificationId })
      });

      if (!verifyRes.ok) throw new Error("서버 검증 실패");

      const result = await verifyRes.json();
      if (!result.ci) throw new Error("CI 정보가 없습니다.");

      setRegCi(result.ci);
      setIsVerified(true);

      const key = await loadPrivateKey(email);
      if (key) setPrivateKey(key);

      alert(`✨ ${result.name}님 본인인증이 완료되었습니다!`);
    } catch (e) {
      alert("본인인증 중 오류 발생: " + e.message);
    } finally {
      setIsLoading(false);
    }
  };

  // 2. 안전 로그인 실행
  const handleLogin = async (e) => {
    e.preventDefault();
    if (!isVerified) return alert("본인인증을 먼저 진행해주세요.");
    if (!password) return alert("비밀번호를 입력해주세요.");

    setIsLoading(true);
    let reissueNeeded = false;
    try {
      let currentKey = privateKey;
      if (!currentKey) currentKey = await loadPrivateKey(email);
      if (!currentKey) {
        reissueNeeded = true;
        throw new Error("기기 인증 정보가 없습니다. '인증서 재발급'이 필요합니다.");
      }

      const deviceId = getDeviceId();
      const chalRes = await fetch(`http://localhost:8080/api/pki/login/challenge?deviceId=${deviceId}`);
      if (!chalRes.ok) {
        reissueNeeded = true;
        throw new Error("서버에서 기기 정보를 찾을 수 없습니다.");
      }
      const { challenge } = await chalRes.json();

      const sig = await window.crypto.subtle.sign("RSASSA-PKCS1-v1_5", currentKey, new TextEncoder().encode(challenge));
      const signature = btoa(String.fromCharCode(...new Uint8Array(sig)));

      const verRes = await fetch('http://localhost:8080/api/pki/login/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        // [수정] 로그인 성공 시 서버가 내려주는 세션 쿠키(JSESSIONID)를 저장/전송하기 위해 credentials 추가
        credentials: 'include',
        body: JSON.stringify({ deviceId, password, signature })
      });

      const result = await verRes.json();
      if (verRes.ok) {
        localStorage.setItem('user_nickname', result.nickname);
        localStorage.setItem('user_role', result.role || 'ROLE_BUYER');
        // [신규] 로그인 성공 직후 서버에서 시작된 인증서 10분 타이머를 프론트와 동기화
        await syncStatus();
        alert(`${result.nickname}님 환영합니다!`);
        navigate('/');
        return;
      } else {
        const message = result.message || "로그인에 실패했습니다.";
        // 기기 인증서가 없거나 폐기/서명 불일치인 경우도 재발급 대상.
        if (message.includes("기기 인증 정보가 없습니다") || message.includes("기기 인증 실패")) {
          reissueNeeded = true;
        }
        throw new Error(message);
      }
    } catch (e) {
      setNeedsReissue(reissueNeeded);
      alert("로그인 오류: " + e.message);
      // 재발급이 필요한 경우엔 새로고침하면 재발급 버튼이 다시 숨겨지므로, 이 경우만 새로고침을 건너뜀.
      if (!reissueNeeded) {
        window.location.reload();
      }
    } finally {
      setIsLoading(false);
    }
  };

  // 3. 기기 인증서 재발급
  const handleReissue = async () => {
    if (!isVerified || !regCi) return alert("본인인증을 먼저 완료해야 합니다.");
    if (!password) return alert("비밀번호를 입력해주세요.");
    if (!window.confirm("인증서를 재발급하시겠습니까?")) return;

    setIsLoading(true);
    try {
      const keyPair = await generateKeyPair();
      const pubKey = await exportPublicKey(keyPair.publicKey);

      const res = await fetch('http://localhost:8080/api/pki/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, nickname: "", role: "", ci: regCi, publicKey: pubKey, deviceId: getDeviceId() })
      });

      if (res.ok) {
        await savePrivateKey(email, keyPair.privateKey);
        setPrivateKey(keyPair.privateKey);
        setNeedsReissue(false);
        alert("인증서가 성공적으로 재발급되었습니다! 이제 로그인이 가능합니다.");
      } else {
        const data = await res.json();
        throw new Error(data.error || "재발급 중 오류 발생");
      }
    } catch (e) {
      alert("오류: " + e.message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-gray-50 text-gray-900">
      {/* [*] 헤더 내용 수정 */}
      <Header />

      {/* 2. 메인 콘텐츠 영역 (배경 + 로그인 카드) */}
      <main
        className="flex-grow flex justify-center items-center bg-cover bg-center p-4 relative"
        style={{ backgroundImage: `url(${backgroundImgUrl})` }}
      >
        {/* 디자인의 그라데이션 오버레이 효과 */}
        <div className="absolute inset-0 bg-radial-gradient from-gray-100/50 via-gray-300/80 to-gray-400/90 mix-blend-multiply"></div>

        {/* 3. 중앙 로그인 카드 (Login Modal) */}
        <div className="relative z-10 bg-white p-12 rounded-3xl shadow-2xl w-full max-w-md border border-gray-100/50 backdrop-blur-sm">
          {/* 카드 상단 */}
          <div className="text-center mb-8">
            <p className="text-xs font-semibold text-red-500 uppercase tracking-widest mb-1">
              SECURE LOGIN
            </p>
            <h1 className="text-4xl font-extrabold text-gray-950">
              로그인
            </h1>
          </div>

          {/* 모드 탭 */}
          <div className="flex rounded-xl overflow-hidden border border-gray-200 mb-6">
            <button
              type="button"
              onClick={() => { setMode('user'); setEmail(''); setPassword(''); setIsVerified(false); setTestRoleHint(null); setNeedsReissue(false); }}
              className={`flex-1 py-2.5 text-sm font-bold transition ${mode === 'user' ? 'bg-blue-600 text-white' : 'bg-gray-50 text-gray-500 hover:bg-gray-100'}`}
            >
              일반 로그인
            </button>
            <button
              type="button"
              onClick={() => { setMode('admin'); setEmail(''); setPassword(''); setIsVerified(false); setTestRoleHint(null); setNeedsReissue(false); }}
              className={`flex-1 py-2.5 text-sm font-bold transition ${mode === 'admin' ? 'bg-red-600 text-white' : 'bg-gray-50 text-gray-500 hover:bg-gray-100'}`}
            >
              관리자 로그인
            </button>
          </div>

          {/* [신규] 테스트 역할 선택 (자동입력 없음, 참고용 힌트만 표시) */}
          {mode === 'admin' && (
            <div className="mb-4">
              <div className="flex gap-2">
                {[
                  { key: 'seller', label: '판매자' },
                  { key: 'buyer', label: '구매자' },
                  { key: 'admin', label: '관리자' },
                ].map(({ key, label }) => (
                  <button
                    key={key}
                    type="button"
                    onClick={() => setTestRoleHint(key)}
                    className={`flex-1 py-2 text-xs font-bold rounded-lg border transition ${
                      testRoleHint === key
                        ? 'border-purple-500 bg-purple-50 text-purple-600'
                        : 'border-gray-200 bg-gray-50 text-gray-500 hover:border-gray-300'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
              {testRoleHint && (
                <div className="mt-2 p-3 rounded-lg bg-gray-50 border border-gray-200 text-xs text-gray-600 space-y-1">
                  {TEST_ROLE_HINTS[testRoleHint].map((acc) => (
                    <div key={acc.email}>
                      {acc.nickname}: <span className="font-mono">{acc.email}</span> / <span className="font-mono">{acc.password}</span>
                    </div>
                  ))}
                  <div className="text-gray-400">위 이메일/비밀번호를 아래 입력창에 직접 입력해주세요.</div>
                </div>
              )}
            </div>
          )}

          {/* 4. 로그인 폼 (Login Form) */}
          <form className="flex flex-col gap-6" onSubmit={mode === 'admin' ? handleAdminLogin : (e) => e.preventDefault()}>
            {/* 이메일 입력 */}
            <div className="flex flex-col gap-1.5">
              <label htmlFor="email" className="text-sm font-medium text-gray-800">
                아이디(이메일)
              </label>
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@example.com"
                className="w-full p-4 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition text-base"
                disabled={isVerified}
              />
            </div>

            {/* 비밀번호 입력 */}
            <div className="flex flex-col gap-1.5">
              <div className="flex justify-between items-baseline">
                <label htmlFor="password" className="text-sm font-medium text-gray-800">
                  비밀번호
                </label>
              </div>
              <input
                type="password"
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full p-4 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition text-base"
              />
            </div>

            {mode === 'admin' ? (
              <button
                type="submit"
                disabled={isLoading}
                className="w-full mt-2 p-4 bg-red-600 text-white rounded-xl font-bold text-lg shadow-lg shadow-red-200 hover:bg-red-700 transition duration-150 active:scale-[0.98] disabled:bg-gray-400"
              >
                {isLoading ? '로그인 중...' : '관리자 로그인'}
              </button>
            ) : !isVerified ? (
              <button
                type="button"
                onClick={handleVerify}
                disabled={isLoading}
                className="w-full mt-2 p-4 bg-blue-600 text-white rounded-xl font-bold text-lg shadow-lg shadow-blue-200 hover:bg-blue-700 transition duration-150 active:scale-[0.98] disabled:bg-gray-400"
              >
                {isLoading ? '처리 중...' : '본인인증 실행'}
              </button>
            ) : (
              <div className="flex flex-col gap-3">
                <button
                  type="button"
                  onClick={handleLogin}
                  disabled={isLoading}
                  className="w-full mt-2 p-4 bg-green-600 text-white rounded-xl font-bold text-lg shadow-lg shadow-green-200 hover:bg-green-700 transition duration-150 active:scale-[0.98]"
                >
                  {isLoading ? '로그인 중...' : '안전 로그인 실행'}
                </button>
                {needsReissue && (
                  <button
                    type="button"
                    onClick={handleReissue}
                    disabled={isLoading}
                    className="w-full p-3 bg-purple-600 text-white rounded-xl font-bold text-base shadow-lg shadow-purple-200 hover:bg-purple-700 transition duration-150"
                  >
                    기기 인증서 재발급
                  </button>
                )}
              </div>
            )}
          </form>

          {/* 5. 카드 하단 (Footer)
          [*] 회원가입 페이지와 연결 */}
          <div className="mt-10 text-center text-gray-700">
            계정이 없으신가요? <Link to="/signup" className="text-blue-600 font-medium hover:underline">회원가입</Link>
          </div>
        </div>
      </main>
    </div>
  );
};

export default LoginPage;
