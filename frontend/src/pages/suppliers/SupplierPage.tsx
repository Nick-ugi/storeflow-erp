import { PlusOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Skeleton, Table } from 'antd'
import { useState } from 'react'
import { supplierApi } from '@/api/productApi'
import { CodeTag } from '@/components/common/CodeTag'
import { PageHeader } from '@/components/common/PageHeader'
import { SearchForm } from '@/components/common/SearchForm'
import { tablePagination } from '@/components/table/pagination'
import { useApiError } from '@/hooks/useApiError'
import { useLoginUser } from '@/hooks/useLoginUser'
import { useSearchState } from '@/hooks/useSearchState'
import type { Supplier, SupplierDetail, SupplierRequest } from '@/types/domain'
import { ACTIVE_STATUS, toOptions } from '@/utils/codes'
import { formatBusinessNumber } from '@/utils/format'

/** 공급처 목록 (SCR-SUPP-001) + 등록 · 수정 · 사용 중지 팝업 (SCR-SUPP-002, ADMIN) */
export function SupplierPage() {
  const { isAdmin } = useLoginUser()
  const search = useSearchState()
  const [editing, setEditing] = useState<number | 'new' | null>(null)

  const params = {
    keyword: search.get('keyword'),
    status: search.get('status'),
    page: search.page,
  }
  const { data, isFetching, error } = useQuery({
    queryKey: ['suppliers', params],
    queryFn: () => supplierApi.search(params),
  })

  return (
    <>
      <PageHeader
        title="공급처"
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
        <Form.Item name="keyword" label="공급처명">
          <Input allowClear style={{ width: 180 }} />
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
      <Table<Supplier>
        rowKey="id"
        loading={isFetching}
        dataSource={data?.content}
        pagination={tablePagination(data, search.setPage)}
        locale={{
          emptyText: error ? error.message : '조회된 공급처가 없습니다.',
        }}
        onRow={(supplier) =>
          isAdmin
            ? {
                onClick: () => setEditing(supplier.id),
                style: { cursor: 'pointer' },
              }
            : {}
        }
        columns={[
          { title: '공급처명', dataIndex: 'supplierName' },
          {
            title: '사업자등록번호',
            dataIndex: 'businessNumber',
            render: formatBusinessNumber,
          },
          {
            title: '담당자명',
            dataIndex: 'contactName',
            render: (value: string | null) => value ?? '-',
          },
          {
            title: '연락처',
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
      {editing === 'new' && <SupplierModal onClose={() => setEditing(null)} />}
      {typeof editing === 'number' && (
        <SupplierEditModal
          supplierId={editing}
          onClose={() => setEditing(null)}
        />
      )}
    </>
  )
}

function SupplierEditModal({
  supplierId,
  onClose,
}: {
  supplierId: number
  onClose: () => void
}) {
  const { data } = useQuery({
    queryKey: ['supplier', supplierId],
    queryFn: () => supplierApi.get(supplierId),
  })
  if (!data) {
    return (
      <Modal open title="공급처 수정" footer={null} onCancel={onClose}>
        <Skeleton active />
      </Modal>
    )
  }
  return <SupplierModal supplier={data} onClose={onClose} />
}

function SupplierModal({
  supplier,
  onClose,
}: {
  supplier?: SupplierDetail
  onClose: () => void
}) {
  const [form] = Form.useForm<SupplierRequest>()
  const { modal, message } = App.useApp()
  const showError = useApiError()
  const queryClient = useQueryClient()

  const saveMutation = useMutation({
    mutationFn: (values: SupplierRequest) =>
      supplier
        ? supplierApi.update(supplier.id, values)
        : supplierApi.create(values),
    onSuccess: () => {
      queryClient.invalidateQueries()
      message.success(
        supplier ? '공급처가 수정되었습니다.' : '공급처가 등록되었습니다.',
      )
      onClose()
    },
    onError: (error) => showError(error, form),
  })
  const statusMutation = useMutation({
    mutationFn: (active: boolean) =>
      supplierApi.setActive(supplier!.id, active),
    onSuccess: (_, active) => {
      queryClient.invalidateQueries()
      message.success(
        active ? '공급처를 다시 사용합니다.' : '공급처를 사용 중지했습니다.',
      )
      onClose()
    },
    onError: (error) => showError(error),
  })

  const active = supplier?.status === 'ACTIVE'
  const footerExtra = supplier && (
    <Button
      danger={active}
      onClick={() =>
        modal.confirm({
          title: active ? '공급처 사용 중지' : '공급처 다시 사용',
          content: active
            ? '공급처를 사용 중지하시겠습니까? 새 발주에서 선택할 수 없게 되며, 진행 중인 발주는 그대로 처리할 수 있습니다.'
            : '공급처를 다시 사용하시겠습니까?',
          onOk: () => statusMutation.mutateAsync(!active),
        })
      }
    >
      {active ? '사용 중지' : '다시 사용'}
    </Button>
  )

  return (
    <Modal
      title={supplier ? '공급처 수정' : '공급처 등록'}
      open
      okText="저장"
      confirmLoading={saveMutation.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      footer={(_, { OkBtn, CancelBtn }) => (
        <>
          {footerExtra}
          <CancelBtn />
          <OkBtn />
        </>
      )}
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={
          supplier && {
            ...supplier,
            businessNumber: formatBusinessNumber(supplier.businessNumber),
          }
        }
        onFinish={(values) => saveMutation.mutate(values)}
      >
        <Form.Item
          name="supplierName"
          label="공급처명"
          rules={[
            {
              required: true,
              whitespace: true,
              message: '공급처명을 입력하세요.',
            },
            { max: 100, message: '공급처명은 100자 이하로 입력하세요.' },
          ]}
        >
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item
          name="businessNumber"
          label="사업자등록번호"
          rules={[
            { required: true, message: '사업자등록번호를 입력하세요.' },
            {
              validator: (_, value: string) =>
                !value || /^\d{10}$/.test(value.replace(/-/g, ''))
                  ? Promise.resolve()
                  : Promise.reject(
                      new Error('사업자등록번호는 숫자 10자리로 입력하세요.'),
                    ),
            },
          ]}
        >
          <Input placeholder="000-00-00000" maxLength={12} />
        </Form.Item>
        <Form.Item
          name="contactName"
          label="담당자명"
          rules={[{ max: 50, message: '담당자명은 50자 이하로 입력하세요.' }]}
        >
          <Input maxLength={50} />
        </Form.Item>
        <Form.Item
          name="phone"
          label="연락처"
          rules={[
            {
              pattern: /^[0-9-]{0,20}$/,
              message: '연락처는 숫자와 하이픈(-)으로 20자 이하로 입력하세요.',
            },
          ]}
        >
          <Input maxLength={20} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
