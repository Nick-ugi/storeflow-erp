import { Flex, Typography } from 'antd'
import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: ReactNode
  extra?: ReactNode
}

export function PageHeader({ title, extra }: PageHeaderProps) {
  return (
    <Flex
      justify="space-between"
      align="center"
      wrap
      gap={8}
      style={{ marginBottom: 16 }}
    >
      <Typography.Title level={4} style={{ margin: 0 }}>
        {title}
      </Typography.Title>
      {extra}
    </Flex>
  )
}
