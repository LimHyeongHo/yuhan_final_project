import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';

const API_BASE = `http://${window.location.hostname}:8080`;
const SessionContext = createContext(null);

export const SessionProvider = ({ children }) => {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);

  const refreshSession = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE}/api/member/session`, {
        credentials: 'include',
      });
      if (!response.ok) {
        setSession(null);
        return null;
      }

      const data = await response.json();
      const authenticatedSession = data.authenticated ? data : null;
      setSession(authenticatedSession);
      return authenticatedSession;
    } catch {
      setSession(null);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  const clearSession = useCallback(() => {
    setSession(null);
    setLoading(false);
  }, []);

  useEffect(() => {
    refreshSession();
  }, [refreshSession]);

  useEffect(() => {
    window.addEventListener('user-profile-updated', refreshSession);
    return () => window.removeEventListener('user-profile-updated', refreshSession);
  }, [refreshSession]);

  return (
    <SessionContext.Provider value={{ session, loading, refreshSession, clearSession }}>
      {children}
    </SessionContext.Provider>
  );
};

export const useSession = () => {
  const context = useContext(SessionContext);
  if (!context) {
    throw new Error('useSession must be used within SessionProvider');
  }
  return context;
};
