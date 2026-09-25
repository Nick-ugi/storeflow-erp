import { PlusOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, DatePicker, Form, Input, Select, Table } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useNavigate } from 'react-router'
import { salesApi } from '@/api/salesApi'
import { CodeTag } from '@/components/common/CodeTag'
import { StoreSelect } from '@/components/common/OptionSelects'
import { PageHeader } from '@/components/common/PageHeader'
import { SearchForm } from '@/components/common/SearchForm'
import { tablePagination } from '@/components/table/pagination'
import { useLoginUser } from '@/hooks/useLoginUser'
import { useSearchState } from '@/hooks/useSearchState'
import type { SaleStatus } from '@/types/code'
import type { Sale } from '@/types/domain'
import { SALE_STATUS, toOptions } from '@/utils/codes'
import { formatDateTime, formatMoney } from '@/utils/format'

interface SaleSearch {
  period: [Dayjs, Dayjs]
  saleNumber?: string
  status?: SaleStatus
  storeId?: number
}

/** 판매 내역 (SCR-SALE-002) — 기간 기본값 최근 7일 */
export function SaleListPage() {
  const navigate = useNavigate()
  const { isAdmin } = useLoginUser()
  const search = useSearchState()

  const startDate =
    search.get('startDate') ?? dayjs().subtract(6, 'day').format('YYYY-MM-DD')
  const endDate = search.get('endDate') ?? dayjs().format('YYYY-MM-DD')
  const params = {
    startDate,
    endDate,
    saleNumber: search.get('saleNumber'),
    status: search.get('status'),
    storeId: isAdmin ? search.getNumber('storeId') : undefined,
    page: search.page,
  }
  const { data, isFetching, error } = useQuery({
    queryKey: ['sales', params],
    queryFn: () => salesApi.search(params),
  })

  return (
    <>
      <PageHeader
        title="판매 내역"
        extra={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => navigate('/sales/new')}
          >
            판매 등록
          </Button>
        }
      />
      <SearchForm<SaleSearch>
        key={search.params.toString()}
        initialValues={{
          period: [dayjs(startDate), dayjs(endDate)],
          saleNumber: params.saleNumber,
          status: params.status as SaleStatus | undefined,
          storeId: params.storeId,
        }}
        onSearch={(values) =>
          search.update({
            startDate: values.period?.[0]?.format('YYYY-MM-DD'),
            endDate: values.period?.[1]?.format('YYYY-MM-DD'),
            saleNumber: values.saleNumber?.trim(),
            status: values.status,
            storeId: values.storeId,
          })
        }
        onReset={search.reset}
      >
        <Form.Item name="period" label="판매일">
          <DatePicker.RangePicker allowClear={false} />
        </Form.Item>
        <Form.Item
          name="saleNumber"
          label="판매번호"
          tooltip="입력하면 판매일 조건은 적용하지 않습니다."
        >
          <Input allowClear style={{ width: 200 }} />
        </Form.Item>
        <Form.Item name="status" label="상태">
          <Select
            allowClear
            placeholder="전체"
            options={toOptions(SALE_STATUS)}
            style={{ width: 100 }}
          />
        </Form.Item>
        {isAdmin && (
          <Form.Item name="storeId" label="매장">
            <StoreSelect allowClear placeholder="전체" />
          </Form.Item>
        )}
      </SearchForm>

      <Table<Sale>
        rowKey="id"
        loading={isFetching}
        dataSource={data?.content}
        pagination={tablePagination(data, search.setPage)}
        locale={{ emptyText: error ? error.message : '판매 내역이 없습니다.' }}
        onRow={(sale) => ({
          onClick: () => navigate(`/sales/${sale.id}`),
          style: { cursor: 'pointer' },
        })}
        columns={[
          { title: '판매번호', dataIndex: 'saleNumber' },
          { title: '판매 일시', dataIndex: 'soldAt', render: formatDateTime },
          { title: '매장', dataIndex: 'storeName' },
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
            render: (value) => <CodeTag value={value} codes={SALE_STATUS} />,
          },
          { title: '처리자', dataIndex: 'createdByName' },
        ]}
      />
    </>
  )
}
