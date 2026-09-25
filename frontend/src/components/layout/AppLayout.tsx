import {
  AppstoreOutlined,
  DashboardOutlined,
  DownOutlined,
  InboxOutlined,
  KeyOutlined,
  LogoutOutlined,
  SettingOutlined,
  ShoppingCartOutlined,
  ShoppingOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button,
  Dropdown,
  Flex,
  Layout,
  Menu,
  Skeleton,
  Typography,
  type MenuProps,
} from 'antd'
import { Suspense, useEffect, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'
import { authApi } from '@/api/authApi'
import { PasswordChangeModal } from '@/components/layout/PasswordChangeModal'
import { useLoginUser } from '@/hooks/useLoginUser'
import { useAuthStore } from '@/stores/authStore'
import type { Role } from '@/types/code'
import { ROLE_LABELS } from '@/utils/codes'

interface MenuEntry {
  key: string
  label: string
  roles?: Role[]
}

interface MenuGroup {
  key: string
  label: string
  icon: React.ReactNode
  roles?: Role[]
  children?: MenuEntry[]
}

/** 메뉴 구조 — 화면 목록 2장. roles가 없으면 전 역할에 표시 */
const MENU: MenuGroup[] = [
  { key: '/dashboard', label: 'Dashboard', icon: <DashboardOutlined /> },
  {
    key: 'sales',
    label: '판매',
    icon: <ShoppingCartOutlined />,
    children: [
      { key: '/sales/new', label: '판매 등록' },
      { key: '/sales', label: '판매 내역' },
    ],
  },
  {
    key: 'stocks',
    label: '재고',
    icon: <InboxOutlined />,
    children: [
      { key: '/stocks', label: '현재 재고' },
      { key: '/stocks/histories', label: '재고 이력' },
    ],
  },
  {
    key: 'purchases',
    label: '발주',
    icon: <ShoppingOutlined />,
    roles: ['ADMIN', 'MANAGER'],
    children: [{ key: '/purchase-orders', label: '발주 목록' }],
  },
  {
    key: 'master',
    label: '기준정보',
    icon: <AppstoreOutlined />,
    children: [
      { key: '/products', label: '상품' },
      { key: '/categories', label: '카테고리' },
      { key: '/suppliers', label: '공급처' },
    ],
  },
  {
    key: 'system',
    label: '시스템 관리',
    icon: <SettingOutlined />,
    roles: ['ADMIN'],
    children: [
      { key: '/stores', label: '매장 관리' },
      { key: '/users', label: '사용자 관리' },
    ],
  },
]

const ALL_PATHS = MENU.flatMap((group) =>
  group.children ? group.children.map((c) => c.key) : [group.key],
)

/** 현재 경로와 가장 길게 일치하는 메뉴를 선택한다. (예: /sales/12 → 판매 내역) */
function selectedMenuKey(pathname: string) {
  if (pathname === '/sales/new') return '/sales/new'
  return ALL_PATHS.filter(
    (path) => pathname === path || pathname.startsWith(`${path}/`),
  ).sort((a, b) => b.length - a.length)[0]
}

/** 공통 레이아웃 (SCR-COM-001): 헤더(사용자 정보 · 비밀번호 변경 · 로그아웃) + 역할별 사이드 메뉴 */
export function AppLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const { user, hasRole } = useLoginUser()
  const setUser = useAuthStore((state) => state.setUser)
  const logout = useAuthStore((state) => state.logout)
  const [passwordOpen, setPasswordOpen] = useState(false)

  // 로그인 직후와 새로고침 시 내 정보를 서버에서 다시 읽는다. (역할 · 매장 변경 반영)
  const { data: me } = useQuery({ queryKey: ['me'], queryFn: authApi.me })
  useEffect(() => {
    if (me) setUser(me)
  }, [me, setUser])

  const menuItems: MenuProps['items'] = MENU.filter(
    (group) => !group.roles || hasRole(...group.roles),
  ).map((group) =>
    group.children
      ? {
          key: group.key,
          icon: group.icon,
          label: group.label,
          children: group.children
            .filter((child) => !child.roles || hasRole(...child.roles))
            .map((child) => ({ key: child.key, label: child.label })),
        }
      : { key: group.key, icon: group.icon, label: group.label },
  )

  /** 로그인 상태를 지우면 RequireAuth가 로그인 화면으로 보낸다. (직접 로그아웃이라 이전 화면은 기억하지 않음) */
  const handleLogout = async () => {
    await authApi.logout().catch(() => undefined)
    logout(true)
    queryClient.clear()
  }

  const userMenu: MenuProps['items'] = [
    {
      key: 'password',
      icon: <KeyOutlined />,
      label: '비밀번호 변경',
      onClick: () => setPasswordOpen(true),
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '로그아웃',
      danger: true,
      onClick: handleLogout,
    },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Sider
        width={220}
        breakpoint="lg"
        collapsedWidth={0}
        theme="light"
        style={{ borderRight: '1px solid #f0f0f0' }}
      >
        <Flex align="center" style={{ height: 56, padding: '0 20px' }}>
          <Typography.Text
            strong
            style={{ fontSize: 17, cursor: 'pointer' }}
            onClick={() => navigate('/dashboard')}
          >
            StoreFlow ERP
          </Typography.Text>
        </Flex>
        <Menu
          mode="inline"
          items={menuItems}
          selectedKeys={[selectedMenuKey(location.pathname) ?? '']}
          defaultOpenKeys={MENU.filter((group) => group.children).map(
            (group) => group.key,
          )}
          onClick={({ key }) => navigate(key)}
          style={{ borderInlineEnd: 'none' }}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header
          style={{
            background: '#fff',
            borderBottom: '1px solid #f0f0f0',
            padding: '0 24px',
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            height: 56,
          }}
        >
          {user && (
            <Dropdown menu={{ items: userMenu }} trigger={['click']}>
              <Button type="text" icon={<UserOutlined />}>
                {user.name} · {ROLE_LABELS[user.role]} ·{' '}
                {user.storeName ?? '본사'} <DownOutlined />
              </Button>
            </Dropdown>
          )}
        </Layout.Header>
        <Layout.Content style={{ padding: 24 }}>
          {/* 화면 코드를 내려받는 동안 표시 (router에서 화면별로 코드 분할) */}
          <Suspense fallback={<Skeleton active />}>
            <Outlet />
          </Suspense>
        </Layout.Content>
      </Layout>
      <PasswordChangeModal
        open={passwordOpen}
        onClose={() => setPasswordOpen(false)}
      />
    </Layout>
  )
}
