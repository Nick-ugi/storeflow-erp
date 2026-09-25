import { useQuery } from '@tanstack/react-query'
import { Alert, DatePicker, Form, Input, Select } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { stockApi } from '@/api/stockApi'
import { StoreSelect } from '@/components/common/OptionSelects'
import { PageHeader } from '@/components/common/PageHeader'
import { SearchForm } from '@/components/common/SearchForm'
import { tablePagination } from '@/components/table/pagination'
import { useLoginUser } from '@/hooks/useLoginUser'
import { useSearchState } from '@/hooks/useSearchState'
import { StockHistoryTable } from '@/pages/stocks/StockHistoryTable'
import type { StockHistoryType } from '@/types/code'
import { STOCK_HISTORY_TYPE, toOptions } from '@/utils/codes'

interface HistorySearch {
  period: [Dayjs, Dayjs]
  keyword?: string
  type?: StockHistoryType
  storeId?: number
}

/** 재고 이력 (SCR-STOCK-005) — 기간 기본값 최근 7일. 재고 이력은 수정 · 삭제할 수 없다. */
export function StockHistoryPage() {
  const { isAdmin } = useLoginUser()
  const search = useSearchState()

  const startDate =
    search.get('startDate') ?? dayjs().subtract(6, 'day').format('YYYY-MM-DD')
  const endDate = search.get('endDate') ?? dayjs().format('YYYY-MM-DD')
  const productId = search.getNumber('productId')
  const params = {
    startDate,
    endDate,
    keyword: search.get('keyword'),
    productId,
    type: search.get('type'),
    storeId: isAdmin ? search.getNumber('storeId') : undefined,
    page: search.page,
  }
  const { data, isFetching, error } = useQuery({
    queryKey: ['stockHistories', params],
    queryFn: () => stockApi.histories(params),
  })

  return (
    <>
      <PageHeader title="재고 이력" />
      {productId && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          title="재고 상세에서 선택한 상품의 이력만 보고 있습니다."
          action={
            <a onClick={() => search.update({ productId: undefined })}>
              전체 상품 보기
            </a>
          }
        />
      )}
      <SearchForm<HistorySearch>
        key={search.params.toString()}
        initialValues={{
          period: [dayjs(startDate), dayjs(endDate)],
          keyword: params.keyword,
          type: params.type as StockHistoryType | undefined,
          storeId: params.storeId,
        }}
        onSearch={(values) =>
          search.update({
            startDate: values.period?.[0]?.format('YYYY-MM-DD'),
            endDate: values.period?.[1]?.format('YYYY-MM-DD'),
            keyword: values.keyword?.trim(),
            type: values.type,
            storeId: values.storeId,
          })
        }
        onReset={search.reset}
      >
        <Form.Item name="period" label="기간">
          <DatePicker.RangePicker allowClear={false} />
        </Form.Item>
        <Form.Item name="keyword" label="상품">
          <Input
            allowClear
            placeholder="상품명 · 상품코드"
            style={{ width: 180 }}
          />
        </Form.Item>
        <Form.Item name="type" label="변동 유형">
          <Select
            allowClear
            placeholder="전체"
            options={toOptions(STOCK_HISTORY_TYPE)}
            style={{ width: 120 }}
          />
        </Form.Item>
        {isAdmin && (
          <Form.Item name="storeId" label="매장">
            <StoreSelect allowClear placeholder="전체" />
          </Form.Item>
        )}
      </SearchForm>
      <StockHistoryTable
        histories={data?.content}
        loading={isFetching}
        pagination={tablePagination(data, search.setPage)}
        emptyText={error?.message}
      />
    </>
  )
}
