import { requestJson } from './http'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  username: string
  sessionId: string
}

export interface UserInfoResponse {
  username: string
  authenticated: boolean
}

export function login(payload: LoginRequest) {
  return requestJson<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function fetchCurrentUser() {
  return requestJson<UserInfoResponse>('/api/auth/me')
}

export async function logout() {
  await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include',
  })
}
