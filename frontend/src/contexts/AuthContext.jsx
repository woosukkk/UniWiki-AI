import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { AUTH_CHANGED_EVENT, clearAuthentication, getStoredUser, saveAuthentication } from '../auth.js';

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
