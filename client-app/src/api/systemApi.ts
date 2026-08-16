import { request } from './http'

export async function getGatewayHealth(): Promise<unknown> {
  const response = await request<unknown>('/api/health')
  return response.data
}

