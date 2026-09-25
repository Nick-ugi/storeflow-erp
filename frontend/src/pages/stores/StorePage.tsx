import { PlusOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Skeleton, Table } from 'antd'
import { useState } from 'react'
import { storeApi } from '@/api/systemApi'
import { CodeTag } from '@/components/common/CodeTag'
import { PageHeader } from '@/components/common/PageHeader'
import { SearchForm } from '@/components/common/SearchForm'
import { tablePagination } from '@/components/table/pagination'
import { useApiError } from '@/hooks/useApiError'
import { useSearchState } from '@/hooks/useSearchState'
import type { Store, StoreDetail, StoreRequest } from '@/types/domain'
import { ACTIVE_STATUS, toOptions } from '@/utils/codes'

/** 매장 목록 (SCR-STORE-001) + 등록 · 수정 · 사용 중지 팝업 (SCR-STORE-002) — ADMIN 전용 */
export function StorePage() {
  const search = useSearchState()
  const [editing, setEditing] = useState<number | 'new' | null>(null)

  const params = {
    keyword: search.get('keyword'),
    status: search.get('status'),
    page: search.page,
  }
  const { data, isFetching, error } = useQuery({
    queryKey: ['stores', params],
    queryFn: () => storeApi.search(params),
  })

  return (
    <>
      <PageHeader
        title="매장 관리"
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
      <SearchForm<{ keyword?: string; status?: string }>
        key={search.params.toString()}
        initialValues={params}
        onSearch={(values) =>
          search.update({
            keyword: values.keyword?.trim(),
            status: values.status,
          })
        }
        onReset={search.reset}
      >
        <Form.Item name="keyword" label="매장">
          <Input
            allowClear
            placeholder="매장명 · 매장코드"
            style={{ width: 180 }}
          />
        </Form.Item>
        <Form.Item name="status" label="사용 여부">
          <Select
            allowClear
            placeholder="전체"
            options={toOptions(ACTIVE_STATUS)}
            style={{ width: 110 }}
          />
        </Form.Item>
      </SearchForm>
      <Table<Store>
        rowKey="id"
        loading={isFetching}
        dataSource={data?.content}
        pagination={tablePagination(data, search.setPage)}
        locale={{
          emptyText: error ? error.message : '조회된 매장이 없습니다.',
        }}
        onRow={(store) => ({
          onClick: () => setEditing(store.id),
          style: { cursor: 'pointer' },
        })}
        columns={[
          { title: '매장코드', dataIndex: 'storeCode' },
          { title: '매장명', dataIndex: 'storeName' },
          {
            title: '주소',
            dataIndex: 'address',
            render: (value: string | null) => value ?? '-',
          },
          {
            title: '전화번호',
            dataIndex: 'phone',
            render: (value: string | null) => value ?? '-',
          },
          {
            title: '사용 여부',
            dataIndex: 'status',
            render: (value) => <CodeTag value={value} codes={ACTIVE_STATUS} />,
          },
        ]}
      />
      {editing === 'new' && <StoreModal onClose={() => setEditing(null)} />}
      {typeof editing === 'number' && (
        <StoreEditModal storeId={editing} onClose={() => setEditing(null)} />
      )}
    </>
  )
}

function StoreEditModal({
  storeId,
  onClose,
}: {
  storeId: number
  onClose: () => void
}) {
  const { data } = useQuery({
    queryKey: ['store', storeId],
    queryFn: () => storeApi.get(storeId),
  })
  if (!data) {
    return (
      <Modal open title="매장 수정" footer={null} onCancel={onClose}>
        <Skeleton active />
      </Modal>
    )
  }
  return <StoreModal store={data} onClose={onClose} />
}

function StoreModal({
  store,
  onClose,
}: {
  store?: StoreDetail
  onClose: () => void
}) {
  const [form] = Form.useForm<StoreRequest>()
  const { modal, message } = App.useApp()
  const showError = useApiError()
  const queryClient = useQueryClient()

  const saveMutation = useMutation({
    mutationFn: (values: StoreRequest) =>
      store ? storeApi.update(store.id, values) : storeApi.create(values),
    onSuccess: () => {
      queryClient.invalidateQueries()
      message.success(
        store
          ? '매장이 수정되었습니다.'
          : '매장이 등록되었습니다. 모든 상품의 재고가 생성되었습니다.',
      )
      onClose()
    },
    onError: (error) => showError(error, form),
  })
  const statusMutation = useMutation({
    mutationFn: (active: boolean) => storeApi.setActive(store!.id, active),
    onSuccess: (_, active) => {
      queryClient.invalidateQueries()
      message.success(
        active ? '매장을 다시 사용합니다.' : '매장을 사용 중지했습니다.',
      )
      onClose()
    },
    onError: (error) => showError(error),
  })

  const active = store?.status === 'ACTIVE'

  return (
    <Modal
      title={store ? '매장 수정' : '매장 등록'}
      open
      okText="저장"
      confirmLoading={saveMutation.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      footer={(_, { OkBtn, CancelBtn }) => (
        <>
          {store && (
            <Button
              danger={active}
              onClick={() =>
                modal.confirm({
                  title: active ? '매장 사용 중지' : '매장 다시 사용',
                  content: active
                    ? '매장을 사용 중지하시겠습니까? 이 매장에서 새 판매 · 발주를 등록할 수 없게 됩니다.'
                    : '매장을 다시 사용하시겠습니까?',
                  onOk: () => statusMutation.mutateAsync(!active),
                })
              }
            >
              {active ? '사용 중지' : '다시 사용'}
            </Button>
          )}
          <CancelBtn />
          <OkBtn />
        </>
      )}
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={store}
        onFinish={(values) => saveMutation.mutate(values)}
      >
        <Form.Item
          name="storeCode"
          label="매장코드"
          extra={
            store
              ? '매장코드는 수정할 수 없습니다.'
              : '영문 대문자 · 숫자 2~10자 (판매번호 · 발주번호에 사용)'
          }
          normalize={(value: string) => value?.toUpperCase()}
          rules={[
            { required: true, message: '매장코드를 입력하세요.' },
            {
              pattern: /^[A-Z0-9]{2,10}$/,
              message: '매장코드는 영문 대문자와 숫자로 2~10자로 입력하세요.',
            },
          ]}
        >
          <Input disabled={store != null} maxLength={10} />
        </Form.Item>
        <Form.Item
          name="storeName"
          label="매장명"
          rules={[
            {
              required: true,
              whitespace: true,
              message: '매장명을 입력하세요.',
            },
            { max: 50, message: '매장명은 50자 이하로 입력하세요.' },
          ]}
        >
          <Input maxLength={50} />
        </Form.Item>
        <Form.Item
          name="address"
          label="주소"
          rules={[{ max: 200, message: '주소는 200자 이하로 입력하세요.' }]}
        >
          <Input maxLength={200} />
        </Form.Item>
        <Form.Item
          name="phone"
          label="전화번호"
          rules={[
            {
              pattern: /^[0-9-]{0,20}$/,
              message:
                '전화번호는 숫자와 하이픈(-)으로 20자 이하로 입력하세요.',
            },
          ]}
        >
          <Input maxLength={20} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
