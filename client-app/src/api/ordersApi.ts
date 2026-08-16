import { request } from './http'
import type { CreateOrderRequest, Order, UpdateOrderRequest } from '../types'

export async function createOrder(payload: CreateOrderRequest, token: string): Promise<Order> {
  const response = await request<Order>(
    '/orders',
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    token,
  )
  return response.data
}

export async function getOrders(token: string): Promise<Order[]> {
  const response = await request<Order[]>('/orders', {}, token)
  return response.data
}

export async function getOrderById(orderId: string, token: string): Promise<Order> {
  const response = await request<Order>(`/orders/${orderId}`, {}, token)
  return response.data
}

export async function updateOrder(orderId: string, payload: UpdateOrderRequest, token: string): Promise<Order> {
  const response = await request<Order>(
    `/orders/${orderId}`,
    {
      method: 'PUT',
      body: JSON.stringify(payload),
    },
    token,
  )
  return response.data
}

export async function deleteOrder(orderId: string, token: string): Promise<string> {
  const response = await request<string>(
    `/orders/${orderId}`,
    {
      method: 'DELETE',
    },
    token,
  )
  return typeof response.data === 'string' ? response.data : 'Deleted'
}

