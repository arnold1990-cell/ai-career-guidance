const ACCESS_TOKEN_KEY = 'edurite_access_token';
const REFRESH_TOKEN_KEY = 'edurite_refresh_token';
const USER_KEY = 'edurite_user';

const storages = [localStorage, sessionStorage];

const getFromStorage = (key: string) => storages.map((storage) => storage.getItem(key)).find((value) => value !== null) ?? null;

export const authStore = {
  getAccessToken: () => getFromStorage(ACCESS_TOKEN_KEY),
  getRefreshToken: () => getFromStorage(REFRESH_TOKEN_KEY),
  getUser: () => {
    const user = getFromStorage(USER_KEY);
    if (!user) return null;

    try {
      return JSON.parse(user);
    } catch {
      storages.forEach((storage) => storage.removeItem(USER_KEY));
      return null;
    }
  },
  setTokens: (accessToken: string, refreshToken?: string, rememberMe = true) => {
    const primaryStorage = rememberMe ? localStorage : sessionStorage;
    const secondaryStorage = rememberMe ? sessionStorage : localStorage;
    primaryStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    secondaryStorage.removeItem(ACCESS_TOKEN_KEY);
    if (refreshToken) {
      primaryStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
      secondaryStorage.removeItem(REFRESH_TOKEN_KEY);
    } else {
      storages.forEach((storage) => storage.removeItem(REFRESH_TOKEN_KEY));
    }
  },
  setUser: (user: unknown, rememberMe = true) => {
    const primaryStorage = rememberMe ? localStorage : sessionStorage;
    const secondaryStorage = rememberMe ? sessionStorage : localStorage;
    primaryStorage.setItem(USER_KEY, JSON.stringify(user));
    secondaryStorage.removeItem(USER_KEY);
  },
  clear: () => {
    storages.forEach((storage) => {
      storage.removeItem(ACCESS_TOKEN_KEY);
      storage.removeItem(REFRESH_TOKEN_KEY);
      storage.removeItem(USER_KEY);
    });
  },
};
