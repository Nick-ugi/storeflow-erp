import { useAuthStore } from '@/stores/authStore'
import type { Role } from '@/types/code'

/**
 * 로그인 사용자와 역할 확인. 메뉴 · 버튼 표시에만 쓰며, 실제 권한 검사는 서버가 한다. (BR-003)
 */
export function useLoginUser() {
  const user = useAuthStore((state) => state.user)
  const role = user?.role
  return {
    user,
    isAdmin: role === 'ADMIN',
    hasRole: (...roles: Role[]) => role != null && roles.includes(role),
  }
}
