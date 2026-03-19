import { createContext, useEffect, useMemo, useState } from 'react';
import { flushSync } from 'react-dom';
import { authStore } from '@/features/auth/authStore';
import { getNormalizedUserRoles, resolvePrimaryRole } from '@/features/auth/roleUtils';
import { authService } from '@/services/authService';
import type { CompanyRegisterPayload, Role, StudentRegisterPayload, User } from '@/types';

export interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isHydrated: boolean;
  login: (payload: { email: string; password: string }, options?: { rememberMe?: boolean }) => Promise<User>;
  logout: () => Promise<void>;
  registerStudent: (payload: StudentRegisterPayload) => Promise<User>;
  registerCompany: (payload: CompanyRegisterPayload) => Promise<User>;
  hasRole: (role: Role) => boolean;
  getPrimaryRole: () => Role | null;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

const normalizeStoredUser = (user: User | null): User | null => {
  if (!user) return null;
  const roles = getNormalizedUserRoles(user);
  if (!roles.length) return null;
  const primaryRole = resolvePrimaryRole({ ...user, roles });
  return { ...user, roles, primaryRole: primaryRole ? `ROLE_${primaryRole}` : user.primaryRole };
};

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isHydrated, setIsHydrated] = useState(false);

  useEffect(() => {
    const token = authStore.getAccessToken();
    const storedUser = normalizeStoredUser(authStore.getUser());
    if (token && storedUser) {
      setUser(storedUser);
    } else if (!token) {
      authStore.clear();
    }
    setIsHydrated(true);
  }, []);

  const setSession = (payload: { accessToken: string; refreshToken?: string; user: User }, options?: { rememberMe?: boolean }) => {
    const rememberMe = options?.rememberMe ?? true;
    const normalizedUser = normalizeStoredUser(payload.user);
    if (!normalizedUser) {
      throw new Error('Authenticated session did not include a supported role.');
    }
    authStore.setTokens(payload.accessToken, payload.refreshToken, rememberMe);
    authStore.setUser(normalizedUser, rememberMe);
    flushSync(() => {
      setUser(normalizedUser);
    });
    if (import.meta.env.DEV) {
      console.info('[auth] auth context session committed', {
        email: normalizedUser.email,
        roles: normalizedUser.roles,
        primaryRole: normalizedUser.primaryRole,
      });
    }
    return normalizedUser;
  };

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      isHydrated,
      isAuthenticated: Boolean(authStore.getAccessToken() && user),
      login: async (payload, options) => setSession(await authService.login(payload), options),
      registerStudent: async (payload) => setSession(await authService.registerStudent(payload)),
      registerCompany: async (payload) => setSession(await authService.registerCompany(payload)),
      logout: async () => {
        try {
          await authService.logout();
        } finally {
          authStore.clear();
          setUser(null);
        }
      },
      hasRole: (role) => getNormalizedUserRoles(user).includes(`ROLE_${role}`),
      getPrimaryRole: () => resolvePrimaryRole(user),
    }),
    [isHydrated, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
