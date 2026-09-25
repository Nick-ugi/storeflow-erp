// API-DASH-001
import { apiGet } from '@/api/client'
import type { Dashboard } from '@/types/domain'

export const dashboardApi = {
  get: (storeId?: number | null) =>
    apiGet<Dashboard>('/dashboard', { storeId }),
}
