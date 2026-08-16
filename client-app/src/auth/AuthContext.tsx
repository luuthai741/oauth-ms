import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { hasRole, isTokenExpired, parseJwt } from '../lib/jwt'
import type { AuthResponse } from '../types'

type AuthContextValue = {
  token: string | null
  username: string | null
  email: string | null
  roles: string[]
  isAuthenticated: boolean
  isAdmin: boolean
  setAuthFromResponse: (payload: AuthResponse) => void
  logout: () => void
}

const STORAGE_KEY = 'gateway_auth'

const AuthContext = createContext<AuthContextValue | null>(null)

type AuthStorage = {
  token: string
  username: string
  email: string
}

function readInitialAuth(): AuthStorage | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as AuthStorage
    if (!parsed.token || isTokenExpired(parsed.token)) {
      localStorage.removeItem(STORAGE_KEY)
      return null
    }
    return parsed
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const initial = readInitialAuth()
  const [token, setToken] = useState<string | null>(initial?.token ?? null)
  const [username, setUsername] = useState<string | null>(initial?.username ?? null)
  const [email, setEmail] = useState<string | null>(initial?.email ?? null)

  const payload = token ? parseJwt(token) : null
  const roles = payload?.roles ?? []

  const value = useMemo<AuthContextValue>(() => {
    const setAuthFromResponse = (auth: AuthResponse) => {
      setToken(auth.accessToken)
      setUsername(auth.username)
      setEmail(auth.email)
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ token: auth.accessToken, username: auth.username, email: auth.email }),
      )
    }

    const logout = () => {
      setToken(null)
      setUsername(null)
      setEmail(null)
      localStorage.removeItem(STORAGE_KEY)
    }

    const authenticated = token !== null && !isTokenExpired(token)

    return {
      token,
      username,
      email,
      roles,
      isAuthenticated: authenticated,
      isAdmin: hasRole(token, 'ADMIN'),
      setAuthFromResponse,
      logout,
    }
  }, [email, roles, token, username])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}

