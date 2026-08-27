// [신규 파일] CA 인증서 유효시간(기본 10분) 카운트다운 + 만료 시 자동 로그아웃을 앱 전역에서 공유하기 위한 Context
import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

const API_BASE = 'http://localhost:8080';
// 서버(만료시각의 기준)와 얼마 주기로 재동기화할지: 드리프트 보정 + 서버측 강제 폐기 감지용
const SYNC_INTERVAL_MS = 15000;

const CertificateTimerContext = createContext(null);

export const CertificateTimerProvider = ({ children }) => {
  const navigate = useNavigate();
  // null = 인증서 타이머 세션이 없음(비로그인 또는 관리자 계정 등)
  const [remainingSeconds, setRemainingSeconds] = useState(null);
  const expiredHandledRef = useRef(false);

  // [신규] 00:00 도달 / 서버가 만료로 판단했을 때 공통 진입점: alert -> 인증서 폐기 -> 로그아웃 -> 메인으로 이동
  const forceExpireLogout = useCallback(async () => {
    if (expiredHandledRef.current) return;
    expiredHandledRef.current = true;

    try {
      await fetch(`${API_BASE}/api/member/certificate/revoke`, {
        method: 'POST',
        credentials: 'include',
      });
    } catch (e) {
      // 이미 서버에서 폐기됐거나 세션이 끊긴 경우도 있으므로 실패해도 로그아웃은 진행한다
    }

    localStorage.removeItem('user_nickname');
    localStorage.removeItem('user_role');
    setRemainingSeconds(null);
    alert('인증서 시간이 만료되었습니다.');
    navigate('/');
  }, [navigate]);

  // [신규] 서버 기준 남은 시간 재동기화. 새로고침 시에도 이 값을 기준으로 카운트다운을 이어간다.
  const syncStatus = useCallback(async () => {
    if (!localStorage.getItem('user_nickname')) {
      setRemainingSeconds(null);
      return;
    }
    try {
      const res = await fetch(`${API_BASE}/api/member/certificate/status`, {
        credentials: 'include',
      });

      if (res.status === 401) {
        const errorData = await res.json().catch(() => null);
        if (errorData?.code === 'AUTH_SESSION_EXPIRED') {
          // 인증서 세션 자체가 없는 계정(예: 관리자 로그인 화면의 테스트 계정)은 조용히 무시
          setRemainingSeconds(null);
          return;
        }
        // 그 외 401(CertificateExpirationFilter가 진짜 만료로 판단해 막은 경우)만 강제 로그아웃
        await forceExpireLogout();
        return;
      }
      if (!res.ok) {
        return;
      }

      const data = await res.json();
      if (data.expired) {
        await forceExpireLogout();
        return;
      }
      expiredHandledRef.current = false;
      setRemainingSeconds(data.remainingSeconds);
    } catch (e) {
      // 네트워크 오류는 다음 폴링에서 재시도
    }
  }, [forceExpireLogout]);

  // [신규] +5분/-5분 조정: 화면 표시뿐 아니라 서버 DB의 만료시각(expiresAt)을 직접 갱신한다.
  const extend = useCallback(async (deltaMinutes) => {
    try {
      const res = await fetch(`${API_BASE}/api/member/certificate/extend`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ deltaMinutes }),
      });

      if (res.status === 401) {
        await forceExpireLogout();
        return;
      }

      const data = await res.json();
      if (res.ok) {
        setRemainingSeconds(data.remainingSeconds);
      }
    } catch (e) {
      // 실패해도 다음 syncStatus 폴링에서 복구됨
    }
  }, [forceExpireLogout]);

  // [신규] 1초마다 로컬 카운트다운 표시만 갱신 (실제 만료 기준은 항상 서버 expiresAt)
  useEffect(() => {
    const tick = setInterval(() => {
      setRemainingSeconds((prev) => (prev === null || prev <= 0 ? prev : prev - 1));
    }, 1000);
    return () => clearInterval(tick);
  }, []);

  // [신규] 카운트다운이 0에 도달하는 순간 만료 처리 트리거
  useEffect(() => {
    if (remainingSeconds === 0) {
      forceExpireLogout();
    }
  }, [remainingSeconds, forceExpireLogout]);

  // [신규] 최초 마운트 시 + 주기적으로 서버와 재동기화 (새로고침해도 서버 값 기준으로 타이머가 계속 흐름)
  useEffect(() => {
    syncStatus();
    const poll = setInterval(syncStatus, SYNC_INTERVAL_MS);
    return () => clearInterval(poll);
  }, [syncStatus]);

  return (
    <CertificateTimerContext.Provider value={{ remainingSeconds, extend, syncStatus, forceExpireLogout }}>
      {children}
    </CertificateTimerContext.Provider>
  );
};

// [신규] 헤더 등에서 남은 시간/조정 함수를 꺼내 쓰기 위한 훅
export const useCertificateTimer = () => useContext(CertificateTimerContext);
