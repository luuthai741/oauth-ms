export type AuthResponse = {
  accessToken: string
  tokenType: string
  expiresIn: number
  username: string
  email: string
}

export type LoginRequest = {
  username: string
  password: string
}

export type RegisterRequest = {
  username: string
  email: string
  password: string
  firstName: string
  lastName: string
}

export type ExternalLoginRequest = {
  provider: string
  accessToken: string
}

export type ExternalLoginUrlResponse = {
  provider: string
  authorizationUrl: string
  state: string
}

export type IntrospectResponse = {
  active: boolean
  sub?: string
  user_id?: string
  roles?: string[]
  issuer?: string
  expires_at?: string
}

export type Order = {
  id: string
  userId: string
  username: string
  description: string
  amount: number
  status: string
  createdAt: number
  updatedAt: number
}

export type CreateOrderRequest = {
  description: string
  amount: number
}

export type UpdateOrderRequest = {
  description: string
  amount: number
  status: string
}

export type ApiResponse<T> = {
  status: number
  data: T
}

export type HttpErrorPayload = {
  message: string
  status: number
  body?: unknown
}



