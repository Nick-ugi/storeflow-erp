import { PlusOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Table } from 'antd'
import { useState } from 'react'
import { categoryApi } from '@/api/productApi'
import { PageHeader } from '@/components/common/PageHeader'
import { useApiError } from '@/hooks/useApiError'
import { useLoginUser } from '@/hooks/useLoginUser'
import type { Category, CategoryRequest } from '@/types/domain'

const LENGTH_MESSAGE = '카테고리명은 50자, 설명은 200자 이하로 입력하세요.'

/** 카테고리 목록 (SCR-CAT-001) + 등록 · 수정 팝업 (SCR-CAT-002, ADMIN). 양이 적어 페이지 없이 전체 표시 */
export function CategoryPage() {
  const { isAdmin } = useLoginUser()
  const { data, isFetching } = useQuery({
    queryKey: ['categories'],
    queryFn: categoryApi.list,
  })
  const [editing, setEditing] = useState<Category | 'new' | null>(null)

  return (
    <>
      <PageHeader
        title="카테고리"
        extra={
          isAdmin && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setEditing('new')}
            >
              등록
            </Button>
          )
        }
      />
      <Table<Category>
        rowKey="id"
        loading={isFetching}
        dataSource={data}
        pagination={false}
        onRow={(category) =>
          isAdmin
            ? {
                onClick: () => setEditing(category),
                style: { cursor: 'pointer' },
              }
            : {}
        }
        columns={[
          { title: '카테고리명', dataIndex: 'categoryName', width: 240 },
          {
            title: '설명',
            dataIndex: 'description',
            render: (value: string | null) => value ?? '-',
          },
        ]}
      />
      {editing && (
        <CategoryModal
          category={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
        />
      )}
    </>
  )
}

function CategoryModal({
  category,
  onClose,
}: {
  category: Category | null
  onClose: () => void
}) {
  const [form] = Form.useForm<CategoryRequest>()
  const { message } = App.useApp()
  const showError = useApiError()
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (values: CategoryRequest) =>
      category
        ? categoryApi.update(category.id, values)
        : categoryApi.create(values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      message.success(
        category ? '카테고리가 수정되었습니다.' : '카테고리가 등록되었습니다.',
      )
      onClose()
    },
    onError: (error) => showError(error, form),
  })

  return (
    <Modal
      title={category ? '카테고리 수정' : '카테고리 등록'}
      open
      okText="저장"
      confirmLoading={mutation.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={category ?? undefined}
        onFinish={(values) => mutation.mutate(values)}
      >
        <Form.Item
          name="categoryName"
          label="카테고리명"
          rules={[
            {
              required: true,
              whitespace: true,
              message: '카테고리명을 입력하세요.',
            },
            { max: 50, message: LENGTH_MESSAGE },
          ]}
        >
          <Input maxLength={50} />
        </Form.Item>
        <Form.Item
          name="description"
          label="설명"
          rules={[{ max: 200, message: LENGTH_MESSAGE }]}
        >
          <Input.TextArea rows={3} maxLength={200} showCount />
        </Form.Item>
      </Form>
    </Modal>
  )
}
