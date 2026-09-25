import { ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import { Button, Card, Form, Space } from 'antd'
import type { ReactNode } from 'react'

interface SearchFormProps<T extends object> {
  initialValues: T
  onSearch: (values: T) => void
  onReset: () => void
  children: ReactNode
}

/**
 * 목록 화면 검색 영역 (화면 공통 규칙 6).
 * 조건은 URL에 있으므로, URL이 바뀌면 key를 바꿔 다시 그려 입력값을 맞춘다.
 */
export function SearchForm<T extends object>({
  initialValues,
  onSearch,
  onReset,
  children,
}: SearchFormProps<T>) {
  const [form] = Form.useForm<T>()
  return (
    <Card size="small" style={{ marginBottom: 16 }}>
      <Form
        form={form}
        layout="inline"
        initialValues={initialValues}
        onFinish={onSearch}
        style={{ rowGap: 8 }}
      >
        {children}
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
              검색
            </Button>
            <Button icon={<ReloadOutlined />} onClick={onReset}>
              초기화
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  )
}
