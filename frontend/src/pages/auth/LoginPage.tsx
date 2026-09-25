import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { useMutation } from '@tanstack/react-query'
import { Alert, Button, Card, Flex, Form, Input, Typography } from 'antd'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import { ApiError } from '@/api/client'
import { authApi } from '@/api/authApi'
import { useAuthStore } from '@/stores/authStore'

interface LoginForm {
  username: string
  password: string
}

/** 로그인 (SCR-AUTH-001) */
export function LoginPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const token = useAuthStore((state) => state.token)
  const login = useAuthStore((state) => state.login)
  const redirect = params.get('redirect') || '/dashboard'

  const mutation = useMutation({
    mutationFn: ({ username, password }: LoginForm) =>
      authApi.login(username, password),
    onSuccess: (result) => {
      login(result.accessToken, result.user)
      navigate(redirect, { replace: true })
    },
  })

  if (token) {
    return <Navigate to={redirect} replace />
  }

  const errorMessage =
    mutation.error instanceof ApiError ? mutation.error.message : undefined

  return (
    <Flex
      justify="center"
      align="center"
      style={{ minHeight: '100vh', background: '#f5f6f8' }}
    >
      <Card style={{ width: 380 }}>
        <Flex vertical align="center" style={{ marginBottom: 24 }}>
          <Typography.Title level={3} style={{ margin: 0 }}>
            StoreFlow ERP
          </Typography.Title>
          <Typography.Text type="secondary">
            판매 · 재고 · 발주 통합 관리 시스템
          </Typography.Text>
        </Flex>
        {params.get('expired') && !errorMessage && (
          <Alert
            type="info"
            showIcon
            title="로그인이 만료되었습니다. 다시 로그인하세요."
            style={{ marginBottom: 16 }}
          />
        )}
        {errorMessage && (
          <Alert
            type="error"
            showIcon
            title={errorMessage}
            style={{ marginBottom: 16 }}
          />
        )}
        <Form<LoginForm>
          layout="vertical"
          onFinish={(values) => mutation.mutate(values)}
          requiredMark={false}
        >
          <Form.Item
            name="username"
            label="아이디"
            rules={[
              { required: true, message: '아이디와 비밀번호를 입력하세요.' },
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              autoComplete="username"
              autoFocus
            />
          </Form.Item>
          <Form.Item
            name="password"
            label="비밀번호"
            rules={[
              { required: true, message: '아이디와 비밀번호를 입력하세요.' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              autoComplete="current-password"
            />
          </Form.Item>
          <Button
            type="primary"
            htmlType="submit"
            block
            size="large"
            loading={mutation.isPending}
          >
            로그인
          </Button>
        </Form>
      </Card>
    </Flex>
  )
}
