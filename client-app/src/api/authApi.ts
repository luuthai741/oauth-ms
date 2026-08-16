import { request } from './http'
import type {
  AuthResponse,
  ExternalLoginUrlResponse,
  IntrospectResponse,
  LoginRequest,
  RegisterRequest,
} from '../types'

export async function login(payload: LoginRequest): Promise<AuthResponse> {
  const response = await request<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

export async function register(payload: RegisterRequest): Promise<AuthResponse> {
  const response = await request<AuthResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

export async function getExternalLoginUrl(provider: string): Promise<ExternalLoginUrlResponse> {
  const encodedProvider = encodeURIComponent(provider)
  const response = await request<ExternalLoginUrlResponse>(`/auth/external/${encodedProvider}/login-url`)
  return response.data
}

export async function exchangeExternalCallback(
  provider: string,
  code: string,
  state: string,
): Promise<AuthResponse> {
  const encodedProvider = encodeURIComponent(provider)
  const query = new URLSearchParams({ code, state }).toString()
  const response = await request<AuthResponse>(`/auth/external/${encodedProvider}/callback?${query}`)
  return response.data
}

export async function introspect(token: string): Promise<IntrospectResponse> {
  const response = await request<IntrospectResponse>('/auth/introspect', {
    method: 'POST',
    body: JSON.stringify({ token }),
  })
  return response.data
}

export async function getJwks(): Promise<unknown> {
  const response = await request<unknown>('/.well-known/jwks.json')
  return response.data
}

export async function getOpenIdConfiguration(): Promise<unknown> {
  const response = await request<unknown>('/.well-known/openid-configuration')
  return response.data
}

