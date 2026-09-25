import { PlusOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Skeleton, Table } from 'antd'
import { useState } from 'react'
import { userApi } from '@/api/systemApi'
import { CodeTag } from '@/components/common/CodeTag'
import { StoreSelect } from '@/components/common/OptionSelects'
import { PageHeader } from '@/components/common/PageHeader'
import { SearchForm } from '@/components/common/SearchForm'
import { tablePagination } from '@/components/table/pagination'
import { useApiError } from '@/hooks/useApiError'
import { useLoginUser } from '@/hooks/useLoginUser'
import { useSearchState } from '@/hooks/useSearchState'
import type { Role } from '@/types/code'
import type { User, UserDetail, UserRequest } from '@/types/domain'
import { ROLE_LABELS, USER_STATUS, toOptions } from '@/utils/codes'
import { formatDate } from '@/utils/format'

const PASSWORD_RULE = {
  pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/,
  message: '비밀번호는 영문과 숫자를 포함해 8~20자로 입력하세요.',
}

interface UserSearch {
  keyword?: string
  role?: Role
  storeId?: number
  status?: string
}

/** 사용자 목록 (SCR-USER-001) + 등록 · 수정 · 비활성화 (SCR-USER-002) + 비밀번호 초기화 (SCR-USER-003) — ADMIN 전용 */
export function UserPage() {
  const search = useSearchState()
  const [editing, setEditing] = useState<number | 'new' | null>(null)

  const params = {
    keyword: search.get('keyword'),
    role: search.get('role'),
    storeId: search.getNumber('storeId'),
    status: search.get('status'),
    page: search.page,
  }
  const { data, isFetching, error } = useQuery({
    queryKey: ['users', params],
    queryFn: () => userApi.search(params),
  })

  return (
    <>
      <PageHeader
        title="사용자 관리"
        extra={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setEditing('new')}
          >
            등록
          </Button>
        }
      />
      <SearchForm<UserSearch>
        key={search.params.toString()}
        initialValues={{ ...params, role: params.role as Role | undefined }}
        onSearch={(values) =>
          search.update({
            keyword: values.keyword?.trim(),
            role: values.role,
            storeId: values.storeId,
            status: values.status,
          })
        }
        onReset={search.reset}
      >
        <Form.Item name="keyword" label="사용자">
          <Input
            allowClear
            placeholder="이름 · 아이디"
            style={{ width: 160 }}
          />
        </Form.Item>
        <Form.Item name="role" label="역할">
          <Select
            allowClear
            placeholder="전체"
            options={toOptions(ROLE_LABELS)}
            style={{ width: 130 }}
          />
        </Form.Item>
        <Form.Item name="storeId" label="소속 매장">
          <StoreSelect allowClear placeholder="전체" />
        </Form.Item>
        <Form.Item name="status" label="상태">
          <Select
            allowClear
            placeholder="전체"
            options={toOptions(USER_STATUS)}
            style={{ width: 100 }}
          />
        </Form.Item>
      </SearchForm>
      <Table<User>
        rowKey="id"
        loading={isFetching}
        dataSource={data?.content}
        pagination={tablePagination(data, search.setPage)}
        locale={{
          emptyText: error ? error.message : '조회된 사용자가 없습니다.',
        }}
        onRow={(user) => ({
          onClick: () => setEditing(user.id),
          style: { cursor: 'pointer' },
        })}
        columns={[
          { title: '아이디', dataIndex: 'username' },
          { title: '이름', dataIndex: 'name' },
          {
            title: '역할',
            dataIndex: 'role',
            render: (role: Role) => ROLE_LABELS[role],
          },
          {
            title: '소속 매장',
            dataIndex: 'storeName',
            render: (value: string | null) => value ?? '본사',
          },
          {
            title: '상태',
            dataIndex: 'status',
            render: (value) => <CodeTag value={value} codes={USER_STATUS} />,
          },
          { title: '등록일', dataIndex: 'createdAt', render: formatDate },
        ]}
      />
      {editing === 'new' && <UserModal onClose={() => setEditing(null)} />}
      {typeof editing === 'number' && (
        <UserEditModal userId={editing} onClose={() => setEditing(null)} />
      )}
    </>
  )
}

function UserEditModal({
  userId,
  onClose,
}: {
  userId: number
  onClose: () => void
}) {
  const { data } = useQuery({
    queryKey: ['user', userId],
    queryFn: () => userApi.get(userId),
  })
  if (!data) {
    return (
      <Modal open title="사용자 수정" footer={null} onCancel={onClose}>
        <Skeleton active />
      </Modal>
    )
  }
  return <UserModal user={data} onClose={onClose} />
}

function UserModal({
  user,
  onClose,
}: {
  user?: UserDetail
  onClose: () => void
}) {
  const [form] = Form.useForm<UserRequest>()
  const { modal, message } = App.useApp()
  const showError = useApiError()
  const queryClient = useQueryClient()
  const { user: me } = useLoginUser()
  const [resetting, setResetting] = useState(false)
  const role = Form.useWatch('role', form)
  const isSelf = user != null && user.id === me?.id

  const saveMutation = useMutation({
    mutationFn: (values: UserRequest) => {
      const request = {
        ...values,
        storeId: values.role === 'ADMIN' ? null : (values.storeId ?? null),
      }
      return user ? userApi.update(user.id, request) : userApi.create(request)
    },
    onSuccess: () => {
      queryClient.invalidateQueries()
      message.success(
        user ? '사용자가 수정되었습니다.' : '사용자가 등록되었습니다.',
      )
      onClose()
    },
    onError: (error) => showError(error, form),
  })
  const statusMutation = useMutation({
    mutationFn: (active: boolean) => userApi.setActive(user!.id, active),
    onSuccess: (_, active) => {
      queryClient.invalidateQueries()
      message.success(
        active ? '사용자를 다시 활성화했습니다.' : '사용자를 비활성화했습니다.',
      )
      onClose()
    },
    onError: (error) => showError(error),
  })

  const active = user?.status === 'ACTIVE'

  return (
    <Modal
      title={user ? '사용자 수정' : '사용자 등록'}
      open
      okText="저장"
      confirmLoading={saveMutation.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      footer={(_, { OkBtn, CancelBtn }) => (
        <>
          {user && (
            <>
              <Button onClick={() => setResetting(true)}>
                비밀번호 초기화
              </Button>
              {!isSelf && (
                <Button
                  danger={active}
                  onClick={() =>
                    modal.confirm({
                      title: active ? '사용자 비활성화' : '사용자 다시 활성화',
                      content: active
                        ? '사용자를 비활성화하시겠습니까? 이 사용자는 더 이상 로그인할 수 없습니다.'
                        : '사용자를 다시 활성화하시겠습니까?',
                      onOk: () => statusMutation.mutateAsync(!active),
                    })
                  }
                >
                  {active ? '비활성화' : '다시 활성화'}
                </Button>
              )}
            </>
          )}
          <CancelBtn />
          <OkBtn />
        </>
      )}
    >
      <Form<UserRequest>
        form={form}
        layout="vertical"
        initialValues={user ?? { role: 'USER' }}
        onFinish={(values) => saveMutation.mutate(values)}
      >
        <Form.Item
          name="username"
          label="아이디"
          extra={user ? '아이디는 수정할 수 없습니다.' : undefined}
          rules={[
            { required: true, message: '아이디를 입력하세요.' },
            {
              pattern: /^[a-z0-9]{4,20}$/,
              message: '아이디는 영문 소문자와 숫자로 4~20자로 입력하세요.',
            },
          ]}
        >
          <Input disabled={user != null} maxLength={20} autoComplete="off" />
        </Form.Item>
        {!user && (
          <Form.Item
            name="password"
            label="초기 비밀번호"
            rules={[
              { required: true, message: '초기 비밀번호를 입력하세요.' },
              PASSWORD_RULE,
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
        )}
        <Form.Item
          name="name"
          label="이름"
          rules={[
            { required: true, whitespace: true, message: '이름을 입력하세요.' },
            { max: 50, message: '이름은 50자 이하로 입력하세요.' },
          ]}
        >
          <Input maxLength={50} />
        </Form.Item>
        <Form.Item
          name="role"
          label="역할"
          extra={isSelf ? '본인 계정의 역할은 변경할 수 없습니다.' : undefined}
          rules={[{ required: true, message: '역할을 선택하세요.' }]}
        >
          <Select
            disabled={isSelf}
            options={toOptions(ROLE_LABELS)}
            onChange={(value: Role) =>
              value === 'ADMIN' && form.setFieldValue('storeId', null)
            }
          />
        </Form.Item>
        <Form.Item
          name="storeId"
          label="소속 매장"
          extra={
            role === 'ADMIN'
              ? 'ADMIN은 소속 매장이 없습니다. (본사)'
              : undefined
          }
          rules={[
            {
              required: role !== 'ADMIN',
              message: 'MANAGER와 USER는 소속 매장을 선택해야 합니다.',
            },
          ]}
        >
          <StoreSelect activeOnly disabled={role === 'ADMIN'} allowClear />
        </Form.Item>
      </Form>
      {user && (
        <PasswordResetModal
          open={resetting}
          userId={user.id}
          onClose={() => setResetting(false)}
        />
      )}
    </Modal>
  )
}

function PasswordResetModal({
  open,
  userId,
  onClose,
}: {
  open: boolean
  userId: number
  onClose: () => void
}) {
  const [form] = Form.useForm<{
    newPassword: string
    confirmPassword: string
  }>()
  const { message } = App.useApp()
  const showError = useApiError()
  const mutation = useMutation({
    mutationFn: (newPassword: string) =>
      userApi.resetPassword(userId, newPassword),
    onSuccess: () => {
      message.success('비밀번호가 초기화되었습니다.')
      onClose()
    },
    onError: (error) => showError(error, form),
  })

  return (
    <Modal
      title="비밀번호 초기화"
      open={open}
      okText="초기화"
      confirmLoading={mutation.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        preserve={false}
        onFinish={(values) => mutation.mutate(values.newPassword)}
      >
        <Form.Item
          name="newPassword"
          label="새 초기 비밀번호"
          rules={[
            { required: true, message: '새 초기 비밀번호를 입력하세요.' },
            PASSWORD_RULE,
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label="새 초기 비밀번호 확인"
          dependencies={['newPassword']}
          rules={[
            { required: true, message: '한 번 더 입력하세요.' },
            ({ getFieldValue }) => ({
              validator: (_, value) =>
                !value || value === getFieldValue('newPassword')
                  ? Promise.resolve()
                  : Promise.reject(
                      new Error('새 초기 비밀번호가 일치하지 않습니다.'),
                    ),
            }),
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
