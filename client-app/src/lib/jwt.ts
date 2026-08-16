export type JwtPayload = {
  sub?: string
  email?: string
  user_id?: string
  roles?: string[]
  exp?: number
  iat?: number
}

function decodeBase64Url(input: string): string {
  const base64 = input.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
  return atob(padded)
}

export function parseJwt(token: string): JwtPayload | null {
  try {
    const parts = token.split('.')
    if (parts.length < 2) {
      return null
    }

    const payloadJson = decodeBase64Url(parts[1])
    return JSON.parse(payloadJson) as JwtPayload
  } catch {
    return null
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = parseJwt(token)
  if (!payload?.exp) {
    return true
  }
  const nowSeconds = Math.floor(Date.now() / 1000)
  return payload.exp <= nowSeconds
}

export function hasRole(token: string | null, role: string): boolean {
  if (!token) {
    return false
  }
  const payload = parseJwt(token)
  const roles = payload?.roles ?? []
  return roles.includes(role)
}

