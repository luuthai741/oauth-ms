import type { ApiResponse } from '../types'

const BASE_URL = import.meta.env.VITE_GATEWAY_URL ?? 'http://localhost:8080'

export class HttpError extends Error {
  status: number
  body?: unknown

  constructor(payload: { message: string; status: number; body?: unknown }) {
    super(payload.message)
    this.status = payload.status
    this.body = payload.body
  }
}

export async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string,
): Promise<ApiResponse<T>> {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  })

  const text = await response.text()
  const maybeJson = parseBody(text)

  if (!response.ok) {
    throw new HttpError({
      message: resolveErrorMessage(maybeJson, response.status),
      status: response.status,
      body: maybeJson,
    })
  }

  return {
    status: response.status,
    data: maybeJson as T,
  }
}

function parseBody(body: string): unknown {
  if (!body) {
    return null
  }

  try {
    return JSON.parse(body)
  } catch {
    return body
  }
}

function resolveErrorMessage(body: unknown, status: number): string {
  if (typeof body === 'string' && body.trim().length > 0) {
    return body
  }

  if (body && typeof body === 'object' && 'message' in body) {
    const message = (body as { message?: unknown }).message
    if (typeof message === 'string' && message.trim().length > 0) {
      return message
    }
  }

  return `Request failed with status ${status}`
}


