// API-AUTH-001 ~ 004
import { apiGet, apiPost, apiPut } from '@/api/client'
import type { LoginResponse, UserInfo } from '@/types/domain'

export const authApi = {
  login: (username: string, password: string) =>
    apiPost<LoginResponse>('/auth/login', { username, password }),
  logout: () => apiPost<null>('/auth/logout'),
  me: () => apiGet<UserInfo>('/auth/me'),
  changePassword: (currentPassword: string, newPassword: string) =>
    apiPut<null>('/auth/password', { currentPassword, newPassword }),
}
