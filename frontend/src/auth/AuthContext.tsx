import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { authApi } from "../api";
import { TOKEN_KEY } from "../api/client";
import type { AuthResponse, LoginRequest, RegisterRequest, User } from "../api/types";

interface AuthContextValue {
  token: string | null;
  roles: string[];
  user: User | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isAgent: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  setUser: (user: User) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [roles, setRoles] = useState<string[]>(() => {
    try {
      return JSON.parse(localStorage.getItem("tunidesk_roles") ?? "[]") as string[];
    } catch {
      return [];
    }
  });
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    if (!token) return;
    authApi
      .me()
      .then(setUser)
      .catch(() => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem("tunidesk_roles");
        setToken(null);
      });
  }, [token]);

  const applyAuth = (auth: AuthResponse) => {
    localStorage.setItem(TOKEN_KEY, auth.accessToken);
    localStorage.setItem("tunidesk_roles", JSON.stringify(auth.roles));
    setToken(auth.accessToken);
    setRoles(auth.roles);
  };

  const login = async (data: LoginRequest) => {
    applyAuth(await authApi.login(data));
  };

  const register = async (data: RegisterRequest) => {
    applyAuth(await authApi.register(data));
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem("tunidesk_roles");
    setToken(null);
    setRoles([]);
    setUser(null);
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      roles,
      user,
      isAuthenticated: Boolean(token),
      isAdmin: roles.includes("ADMIN"),
      isAgent: roles.includes("ADMIN") || roles.includes("AGENT"),
      login,
      register,
      logout,
      setUser,
    }),
    [token, roles, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
