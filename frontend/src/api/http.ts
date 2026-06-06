export class ApiError extends Error {
  status: number
  body: unknown

  constructor(status: number, body: unknown, message?: string) {
    super(message ?? `请求失败（${status}）`)
    this.status = status
    this.body = body
  }
}

export async function requestJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    credentials: 'include',
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init.headers ?? {}),
    },
  })

  const text = await response.text()
  const body = text ? JSON.parse(text) : null

  if (!response.ok) {
    const message =
      typeof body === 'object' && body !== null && 'message' in body
        ? String((body as { message: string }).message)
        : undefined
    throw new ApiError(response.status, body, message)
  }
  return body as T
}
