import { createContext, useContext, useMemo, useState } from 'react';
import { authStore } from '@/features/auth/authStore';
import { authService } from '@/services/authService';
import type { Role, User } from '@/types';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  login: (payload: { email: string; password: string }) => Promise<void>;
  logout: () => Promise<void>;
  registerStudent: (payload: Record<string, unknown>) => Promise<void>;
  registerCompany: (payload: Record<string, unknown>) => Promise<void>;
  hasRole: (role: Role) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(authStore.getUser());

  const setSession = (payload: { accessToken: string; refreshToken?: string; user: User }) => {
    authStore.setTokens(payload.accessToken, payload.refreshToken);
    authStore.setUser(payload.user);
    setUser(payload.user);
  };

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      isAuthenticated: Boolean(authStore.getAccessToken() && user),
      login: async (payload) => setSession(await authService.login(payload)),
      registerStudent: async (payload) => setSession(await authService.registerStudent(payload)),
      registerCompany: async (payload) => setSession(await authService.registerCompany(payload)),
      logout: async () => {
        await authService.logout();
        authStore.clear();
        setUser(null);
      },
      hasRole: (role) => user?.role === role,
    }),
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
};
