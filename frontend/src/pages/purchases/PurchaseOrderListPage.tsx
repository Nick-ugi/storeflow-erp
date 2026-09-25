import { PlusOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, DatePicker, Form, Input, Select, Table } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useNavigate } from 'react-router'
import { purchaseApi } from '@/api/purchaseApi'
import { CodeTag } from '@/components/common/CodeTag'
import { StoreSelect, SupplierSelect } from '@/components/common/OptionSelects'
import { PageHeader } from '@/components/common/PageHeader'
import { SearchForm } from '@/components/common/SearchForm'
import { tablePagination } from '@/components/table/pagination'
import { useLoginUser } from '@/hooks/useLoginUser'
import { useSearchState } from '@/hooks/useSearchState'
import type { PurchaseOrderStatus } from '@/types/code'
import type { PurchaseOrder } from '@/types/domain'
import { PURCHASE_ORDER_STATUS, toOptions } from '@/utils/codes'
import { formatDateTime, formatMoney } from '@/utils/format'

interface OrderSearch {
  period: [Dayjs, Dayjs]
  status?: PurchaseOrderStatus
  supplierId?: number
  orderNumber?: string
  storeId?: number
}

/** 발주 목록 (SCR-PO-001) — 기간 기본값 최근 30일 */
export function PurchaseOrderListPage() {
  const navigate = useNavigate()
  const { isAdmin } = useLoginUser()
  const search = useSearchState()

  const startDate =
    search.get('startDate') ?? dayjs().subtract(29, 'day').format('YYYY-MM-DD')
  const endDate = search.get('endDate') ?? dayjs().format('YYYY-MM-DD')
  const params = {
    startDate,
    endDate,
    status: search.get('status'),
    supplierId: search.getNumber('supplierId'),
    orderNumber: search.get('orderNumber'),
    storeId: isAdmin ? search.getNumber('storeId') : undefined,
    page: search.page,
  }
  const { data, isFetching, error } = useQuery({
    queryKey: ['purchaseOrders', params],
    queryFn: () => purchaseApi.search(params),
  })

  return (
    <>
      <PageHeader
        title="발주 목록"
        extra={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => navigate('/purchase-orders/new')}
          >
            발주 등록
          </Button>
        }
      />
      <SearchForm<OrderSearch>
        key={search.params.toString()}
        initialValues={{
          period: [dayjs(startDate), dayjs(endDate)],
          status: params.status as PurchaseOrderStatus | undefined,
          supplierId: params.supplierId,
          orderNumber: params.orderNumber,
          storeId: params.storeId,
        }}
        onSearch={(values) =>
          search.update({
            startDate: values.period?.[0]?.format('YYYY-MM-DD'),
            endDate: values.period?.[1]?.format('YYYY-MM-DD'),
            status: values.status,
            supplierId: values.supplierId,
            orderNumber: values.orderNumber?.trim(),
            storeId: values.storeId,
          })
        }
        onReset={search.reset}
      >
        <Form.Item name="period" label="발주일">
          <DatePicker.RangePicker allowClear={false} />
        </Form.Item>
        <Form.Item name="status" label="상태">
          <Select
            allowClear
            placeholder="전체"
            options={toOptions(PURCHASE_ORDER_STATUS)}
            style={{ width: 110 }}
          />
        </Form.Item>
        <Form.Item name="supplierId" label="공급처">
          <SupplierSelect allowClear placeholder="전체" />
        </Form.Item>
        <Form.Item
          name="orderNumber"
          label="발주번호"
          tooltip="입력하면 발주일 조건은 적용하지 않습니다."
        >
          <Input allowClear style={{ width: 200 }} />
        </Form.Item>
        {isAdmin && (
          <Form.Item name="storeId" label="매장">
            <StoreSelect allowClear placeholder="전체" />
          </Form.Item>
        )}
      </SearchForm>

      <Table<PurchaseOrder>
        rowKey="id"
        loading={isFetching}
        dataSource={data?.content}
        pagination={tablePagination(data, search.setPage)}
        locale={{ emptyText: error ? error.message : '발주 내역이 없습니다.' }}
        onRow={(order) => ({
          onClick: () => navigate(`/purchase-orders/${order.id}`),
          style: { cursor: 'pointer' },
        })}
        columns={[
          { title: '발주번호', dataIndex: 'orderNumber' },
          { title: '발주일', dataIndex: 'orderedAt', render: formatDateTime },
          { title: '매장', dataIndex: 'storeName' },
          { title: '공급처', dataIndex: 'supplierName' },
          { title: '품목 수', dataIndex: 'itemCount', align: 'right' },
          {
            title: '합계 금액',
            dataIndex: 'totalAmount',
            align: 'right',
            render: formatMoney,
          },
          {
            title: '상태',
            dataIndex: 'status',
            render: (value) => (
              <CodeTag value={value} codes={PURCHASE_ORDER_STATUS} />
            ),
          },
        ]}
      />
    </>
  )
}
