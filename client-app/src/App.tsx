import { useMemo, useState, type FormEvent } from 'react'
import './App.css'
import {
  getExternalLoginUrl,
  getJwks,
  getOpenIdConfiguration,
  introspect,
  login,
  register,
} from './api/authApi'
import { createOrder, deleteOrder, getOrderById, getOrders, updateOrder } from './api/ordersApi'
import { getGatewayHealth } from './api/systemApi'
import { HttpError } from './api/http'
import { useAuth } from './auth/AuthContext'
import OAuthCallbackPage, { isOAuthCallbackPath } from './pages/OAuthCallbackPage'
import type { Order } from './types'

type TabId =
  | 'auth-login'
  | 'auth-register'
  | 'auth-external'
  | 'auth-introspect'
  | 'meta'
  | 'orders'
  | 'admin'

function App() {
  const auth = useAuth()
  const [activeTab, setActiveTab] = useState<TabId>('auth-login')
  const [message, setMessage] = useState<string>('')
  const [payloadView, setPayloadView] = useState<string>('')
  const [orders, setOrders] = useState<Order[]>([])
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [externalProvider, setExternalProvider] = useState<string>('google')

  const visibleTabs = useMemo(() => {
    const tabs: { id: TabId; label: string }[] = [
      { id: 'auth-login', label: 'Login' },
      { id: 'auth-register', label: 'Register' },
      { id: 'auth-external', label: 'External Login' },
      { id: 'auth-introspect', label: 'Introspect' },
      { id: 'meta', label: 'Gateway/JWKS' },
      { id: 'orders', label: 'Orders(USER)' },
    ]

    if (auth.isAdmin) {
      tabs.push({ id: 'admin', label: 'Admin Orders' })
    }

    return tabs
  }, [auth.isAdmin])

  const showError = (error: unknown) => {
    if (error instanceof HttpError) {
      setMessage(`Error ${error.status}: ${error.message}`)
      return
    }
    if (error instanceof Error) {
      setMessage(error.message)
      return
    }
    setMessage('Unknown error')
  }

  const requireToken = (): string | null => {
    if (!auth.token) {
      setMessage('Please login first.')
      return null
    }
    return auth.token
  }

  const onLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setMessage('')
    const form = new FormData(event.currentTarget)

    try {
      const response = await login({
        username: String(form.get('username') ?? ''),
        password: String(form.get('password') ?? ''),
      })
      auth.setAuthFromResponse(response)
      setMessage(`Login success as ${response.username}`)
      setActiveTab('orders')
    } catch (error) {
      showError(error)
    }
  }

  const onRegister = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setMessage('')
    const form = new FormData(event.currentTarget)

    try {
      const response = await register({
        username: String(form.get('username') ?? ''),
        email: String(form.get('email') ?? ''),
        password: String(form.get('password') ?? ''),
        firstName: String(form.get('firstName') ?? ''),
        lastName: String(form.get('lastName') ?? ''),
      })
      auth.setAuthFromResponse(response)
      setMessage(`Register success as ${response.username}`)
      setActiveTab('orders')
    } catch (error) {
      showError(error)
    }
  }

  const onStartExternalLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setMessage('')
    const form = new FormData(event.currentTarget)

    try {
      const provider = String(form.get('provider') ?? '')
      const response = await getExternalLoginUrl(provider)
      setExternalProvider(response.provider)
      setPayloadView(JSON.stringify(response, null, 2))
      setMessage(`Redirecting to ${response.provider} consent screen...`)
      window.location.href = response.authorizationUrl
    } catch (error) {
      showError(error)
    }
  }

  const onIntrospect = async () => {
    setMessage('')
    const token = requireToken()
    if (!token) return

    try {
      const response = await introspect(token)
      setPayloadView(JSON.stringify(response, null, 2))
      setMessage(response.active ? 'Token is active' : 'Token is inactive')
    } catch (error) {
      showError(error)
    }
  }

  const onFetchGatewayHealth = async () => {
    setMessage('')
    try {
      const data = await getGatewayHealth()
      setPayloadView(JSON.stringify(data, null, 2))
      setMessage('Gateway health fetched')
    } catch (error) {
      showError(error)
    }
  }

  const onFetchJwks = async () => {
    setMessage('')
    try {
      const data = await getJwks()
      setPayloadView(JSON.stringify(data, null, 2))
      setMessage('JWKS fetched')
    } catch (error) {
      showError(error)
    }
  }

  const onFetchOpenId = async () => {
    setMessage('')
    try {
      const data = await getOpenIdConfiguration()
      setPayloadView(JSON.stringify(data, null, 2))
      setMessage('OpenID config fetched')
    } catch (error) {
      showError(error)
    }
  }

  const onCreateOrder = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setMessage('')
    const token = requireToken()
    if (!token) return

    const form = new FormData(event.currentTarget)
    try {
      const created = await createOrder(
        {
          description: String(form.get('description') ?? ''),
          amount: Number(form.get('amount') ?? 0),
        },
        token,
      )
      setSelectedOrder(created)
      setMessage(`Order created: ${created.id}`)
    } catch (error) {
      showError(error)
    }
  }

  const onGetOrders = async () => {
    setMessage('')
    const token = requireToken()
    if (!token) return

    try {
      const list = await getOrders(token)
      setOrders(list)
      setPayloadView(JSON.stringify(list, null, 2))
      setMessage(`Fetched ${list.length} orders`)
    } catch (error) {
      showError(error)
    }
  }

  const onGetOrderById = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setMessage('')
    const token = requireToken()
    if (!token) return

    const form = new FormData(event.currentTarget)
    const orderId = String(form.get('orderId') ?? '')

    try {
      const order = await getOrderById(orderId, token)
      setSelectedOrder(order)
      setPayloadView(JSON.stringify(order, null, 2))
      setMessage(`Order loaded: ${order.id}`)
    } catch (error) {
      showError(error)
    }
  }

  const onUpdateOrder = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setMessage('')
    const token = requireToken()
    if (!token) return

    const form = new FormData(event.currentTarget)
    const orderId = String(form.get('orderId') ?? '')

    try {
      const order = await updateOrder(
        orderId,
        {
          description: String(form.get('description') ?? ''),
          amount: Number(form.get('amount') ?? 0),
          status: String(form.get('status') ?? ''),
        },
        token,
      )
      setSelectedOrder(order)
      setMessage(`Order updated: ${order.id}`)
    } catch (error) {
      showError(error)
    }
  }

  const onDeleteOrder = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setMessage('')
    const token = requireToken()
    if (!token) return

    const form = new FormData(event.currentTarget)
    const orderId = String(form.get('orderId') ?? '')

    try {
      const result = await deleteOrder(orderId, token)
      setMessage(result)
      setSelectedOrder(null)
    } catch (error) {
      showError(error)
    }
  }

  if (isOAuthCallbackPath(window.location.pathname)) {
    return <OAuthCallbackPage />
  }

  return (
    <div className="app-shell">
      <header className="top-bar">
        <div>
          <h1>Client App - Gateway API Console</h1>
          <p>Base URL: {import.meta.env.VITE_GATEWAY_URL ?? 'http://localhost:8080'}</p>
        </div>
        <div className="session-card">
          <p>
            User: <strong>{auth.username ?? 'Anonymous'}</strong>
          </p>
          <p>
            Roles: <strong>{auth.roles.join(', ') || 'none'}</strong>
          </p>
          <button type="button" onClick={auth.logout}>
            Logout
          </button>
        </div>
      </header>

      <nav className="tabs">
        {visibleTabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            className={tab.id === activeTab ? 'active' : ''}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </nav>

      <main className="page">
        {activeTab === 'auth-login' && (
          <section>
            <h2>POST /auth/login</h2>
            <form onSubmit={onLogin}>
              <input name="username" placeholder="username" defaultValue="user" required />
              <input name="password" type="password" placeholder="password" defaultValue="password123" required />
              <button type="submit">Login</button>
            </form>
          </section>
        )}

        {activeTab === 'auth-register' && (
          <section>
            <h2>POST /auth/register</h2>
            <form onSubmit={onRegister}>
              <input name="username" placeholder="username" required />
              <input name="email" type="email" placeholder="email" required />
              <input name="password" type="password" placeholder="password" required />
              <input name="firstName" placeholder="first name" required />
              <input name="lastName" placeholder="last name" required />
              <button type="submit">Register</button>
            </form>
          </section>
        )}

        {activeTab === 'auth-external' && (
          <section>
            <h2>External OAuth Login</h2>
            <p>Get provider login URL and redirect user to consent screen.</p>
            <form onSubmit={onStartExternalLogin}>
              <select
                name="provider"
                value={externalProvider}
                onChange={(event) => setExternalProvider(event.target.value)}
              >
                <option value="google">google</option>
              </select>
              <button type="submit">GET /auth/external/{'{provider}'}/login-url</button>
            </form>
            <p>
              Callback is handled automatically at
              {' '}
              <code>/oauth/callback/{'{provider}'}</code>
              {' '}
              and signs in user immediately.
            </p>
          </section>
        )}

        {activeTab === 'auth-introspect' && (
          <section>
            <h2>POST /auth/introspect</h2>
            <button type="button" onClick={onIntrospect}>
              Introspect Current Token
            </button>
          </section>
        )}

        {activeTab === 'meta' && (
          <section>
            <h2>Gateway & Metadata</h2>
            <div className="button-row">
              <button type="button" onClick={onFetchGatewayHealth}>
                GET /api/health
              </button>
              <button type="button" onClick={onFetchJwks}>
                GET /.well-known/jwks.json
              </button>
              <button type="button" onClick={onFetchOpenId}>
                GET /.well-known/openid-configuration
              </button>
            </div>
          </section>
        )}

        {activeTab === 'orders' && (
          <section>
            <h2>User Order APIs</h2>
            <form onSubmit={onCreateOrder}>
              <input name="description" placeholder="description" required />
              <input name="amount" type="number" min="1" placeholder="amount" required />
              <button type="submit">POST /orders</button>
            </form>

            <form onSubmit={onGetOrderById}>
              <input name="orderId" placeholder="order id" defaultValue={selectedOrder?.id ?? ''} required />
              <button type="submit">GET /orders/{'{id}'}</button>
            </form>

            <button type="button" onClick={onGetOrders}>
              GET /orders
            </button>
          </section>
        )}

        {activeTab === 'admin' && (
          <section>
            <h2>Admin Order APIs</h2>
            <form onSubmit={onUpdateOrder}>
              <input name="orderId" placeholder="order id" defaultValue={selectedOrder?.id ?? ''} required />
              <input name="description" placeholder="description" required />
              <input name="amount" type="number" min="1" placeholder="amount" required />
              <input name="status" placeholder="status e.g. COMPLETED" required />
              <button type="submit">PUT /orders/{'{id}'}</button>
            </form>

            <form onSubmit={onDeleteOrder}>
              <input name="orderId" placeholder="order id" defaultValue={selectedOrder?.id ?? ''} required />
              <button type="submit">DELETE /orders/{'{id}'}</button>
            </form>
          </section>
        )}
      </main>

      {message && <p className="message">{message}</p>}

      <section className="payload">
        <h3>Response Viewer</h3>
        <pre>{payloadView || 'No payload yet'}</pre>
      </section>

      {orders.length > 0 && (
        <section className="orders-table">
          <h3>Orders Snapshot</h3>
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>User</th>
                <th>Description</th>
                <th>Amount</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id} onClick={() => setSelectedOrder(order)}>
                  <td>{order.id}</td>
                  <td>{order.username}</td>
                  <td>{order.description}</td>
                  <td>{order.amount}</td>
                  <td>{order.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </div>
  )
}

export default App
