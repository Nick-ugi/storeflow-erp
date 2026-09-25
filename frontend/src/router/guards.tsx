import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useLoginUser } from '@/hooks/useLoginUser'
import { useAuthStore } from '@/stores/authStore'
import type { Role } from '@/types/code'

/** 로그인하지 않았으면 로그인 화면으로 보내고, 로그인 후 원래 화면으로 돌아오게 한다. (화면 공통 규칙 1) */
export function RequireAuth({ children }: { children: ReactNode }) {
  const token = useAuthStore((state) => state.token)
  const loggedOutByUser = useAuthStore((state) => state.loggedOutByUser)
  const location = useLocation()
  if (!token) {
    // 직접 로그아웃한 경우에는 이전 화면을 기억하지 않는다. (다른 계정이 로그인하면 권한이 다를 수 있음)
    if (loggedOutByUser) return <Navigate to="/login" replace />
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  return children
}

/** 권한이 없는 화면에 URL로 직접 들어오면 권한 없음 화면을 보여준다. (화면 공통 규칙 3) */
export function RequireRole({
  roles,
  children,
}: {
  roles: Role[]
  children: ReactNode
}) {
  const { hasRole } = useLoginUser()
  return hasRole(...roles) ? children : <Navigate to="/403" replace />
}
