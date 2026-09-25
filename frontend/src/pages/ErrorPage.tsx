import { Button, Result } from 'antd'
import { useNavigate } from 'react-router'

/** 오류 안내 (SCR-COM-002) */
export function ErrorPage({ status }: { status: 403 | 404 }) {
  const navigate = useNavigate()
  return (
    <Result
      status={status === 403 ? '403' : '404'}
      title={status}
      subTitle={
        status === 403 ? '접근 권한이 없습니다.' : '페이지를 찾을 수 없습니다.'
      }
      extra={
        <Button type="primary" onClick={() => navigate('/dashboard')}>
          Dashboard로 이동
        </Button>
      }
    />
  )
}
