import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserInfo } from '@/types/domain'

interface AuthState {
  token: string | null
  user: UserInfo | null
  /** 사용자가 직접 로그아웃했는지. 이때는 로그인 후 돌아갈 화면을 기억하지 않는다. */
  loggedOutByUser: boolean
  login: (token: string, user: UserInfo) => void
  setUser: (user: UserInfo) => void
  logout: (byUser?: boolean) => void
}

/**
 * 로그인 상태. 새로고침해도 유지되도록 localStorage에 보관한다.
 * 권한 판단은 서버가 요청마다 하므로 여기 있는 역할은 메뉴 · 버튼 표시용이다. (BR-003)
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      loggedOutByUser: false,
      login: (token, user) => set({ token, user, loggedOutByUser: false }),
      setUser: (user) => set({ user }),
      logout: (byUser = false) =>
        set({ token: null, user: null, loggedOutByUser: byUser }),
    }),
    {
      name: 'storeflow-auth',
      partialize: (state) => ({ token: state.token, user: state.user }),
    },
  ),
)
