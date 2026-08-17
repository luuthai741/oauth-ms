import { useEffect, useRef, useState } from 'react'
import { exchangeExternalCallback } from '../api/authApi'
import { HttpError } from '../api/http'
import { useAuth } from '../auth/AuthContext'

type CallbackPhase = 'processing' | 'success' | 'error'

const CALLBACK_PATH_PATTERN = /^\/oauth\/callback(?:\/([^/?#]+))?\/?$/

export function isOAuthCallbackPath(pathname: string): boolean {
  return CALLBACK_PATH_PATTERN.test(pathname)
}

function getProviderFromPath(pathname: string): string | null {
  const match = pathname.match(CALLBACK_PATH_PATTERN)
  return match?.[1] ?? null
}

export default function OAuthCallbackPage() {
  const auth = useAuth()
  const [phase, setPhase] = useState<CallbackPhase>('processing')
  const [message, setMessage] = useState<string>('Processing OAuth callback...')
  const didStartRef = useRef(false)

  useEffect(() => {
    if (didStartRef.current) {
      return
    }
    didStartRef.current = true

    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    const state = params.get('state')
    const provider = getProviderFromPath(window.location.pathname) ?? params.get('provider')

    if (!provider || !code || !state) {
      setPhase('error')
      setMessage('Missing provider/code/state in OAuth callback URL.')
      return
    }

    void (async () => {
      try {
        const response = await exchangeExternalCallback(provider, code, state)
        auth.setAuthFromResponse(response)
        setPhase('success')
        setMessage(`Logged in as ${response.username}. Redirecting...`)

        window.setTimeout(() => {
          window.location.replace('/')
        }, 700)
      } catch (error) {
        setPhase('error')
        if (error instanceof HttpError) {
          setMessage(`External callback failed (${error.status}): ${error.message}`)
          return
        }
        if (error instanceof Error) {
          setMessage(`External callback failed: ${error.message}`)
          return
        }
        setMessage('External callback failed with an unknown error.')
      }
    })()
  }, [auth])

  return (
    <div className="callback-shell">
      <section className="callback-card">
        <h2>OAuth Callback</h2>
        <p>{message}</p>
        {phase === 'processing' && <p>Please wait...</p>}
        {phase === 'error' && (
          <button type="button" onClick={() => window.location.replace('/')}>
            Back to Login
          </button>
        )}
      </section>
    </div>
  )
}

