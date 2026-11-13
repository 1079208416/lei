interface ApiEnvelope<T> {
  code: string
  message: string
  data: T
  requestId: string | null
}

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown
}

const baseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

export class ApiError extends Error {
  status: number
  code: string

  constructor(message: string, status: number, code: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }
  const token = localStorage.getItem('shike_token')
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  let envelope: ApiEnvelope<T> | null = null
  try {
    envelope = (await response.json()) as ApiEnvelope<T>
  } catch {
    throw new ApiError('服务返回了无法识别的内容', response.status, 'INVALID_RESPONSE')
  }

  if (!response.ok || envelope.code !== 'SUCCESS') {
    if (response.status === 401) {
      localStorage.removeItem('shike_token')
      localStorage.removeItem('shike_account')
    }
    throw new ApiError(envelope.message || '请求失败', response.status, envelope.code)
  }
  return envelope.data
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PATCH', body }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}
