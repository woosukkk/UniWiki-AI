import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';
import {
  AUTH_CHANGED_EVENT,
  clearAuthentication,
  getStoredUser,
  saveAuthentication,
  updateStoredUser,
} from '../auth.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getStoredUser);

  useEffect(() => {
    const syncUser = () => setUser(getStoredUser());
    window.addEventListener('storage', syncUser);
    window.addEventListener(AUTH_CHANGED_EVENT, syncUser);
    return () => {
      window.removeEventListener('storage', syncUser);
      window.removeEventListener(AUTH_CHANGED_EVENT, syncUser);
    };
  }, []);

  useEffect(() => {
    const token = localStorage.getItem('uniwiki-token');
    if (!token || !user) return undefined;

    let active = true;
    api.getMe()
      .then((response) => {
        if (!active) return;
        const refreshedUser = updateStoredUser(response);
        if (refreshedUser) setUser(refreshedUser);
      })
      .catch(() => {
        // 401 responses are handled by the shared API interceptor. For temporary
        // network failures, keep the existing session instead of logging out.
      });

    return () => {
      active = false;
    };
  }, [user?.id]);

  const value = useMemo(() => ({
    user,
    isAuthenticated: Boolean(user),
    isAdmin: user?.role === 'ADMIN',
    login(response) {
      const authenticatedUser = saveAuthentication(response);
      setUser(authenticatedUser);
      return authenticatedUser;
    },
    logout() {
      clearAuthentication();
      setUser(null);
    },
  }), [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider.');
  return context;
}
