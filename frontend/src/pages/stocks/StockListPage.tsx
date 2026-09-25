import { HistoryOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Checkbox, Form, Input, Space, Table, Tag } from 'antd'
import { useNavigate } from 'react-router'
import { stockApi } from '@/api/stockApi'
import { CategorySelect, StoreSelect } from '@/components/common/OptionSelects'
import { PageHeader } from '@/components/common/PageHeader'
import { SearchForm } from '@/components/common/SearchForm'
import { tablePagination } from '@/components/table/pagination'
import { useLoginUser } from '@/hooks/useLoginUser'
import { useSearchState } from '@/hooks/useSearchState'
import type { Stock } from '@/types/domain'
import { formatNumber } from '@/utils/format'

interface StockSearch {
  keyword?: string
  categoryId?: number
  shortageOnly?: boolean
  storeId?: number
}

/** 현재 재고 (SCR-STOCK-001) — 부족 재고 = 사용 중인 상품의 현재 수량 < 안전재고 */
export function StockListPage() {
  const navigate = useNavigate()
  const { isAdmin } = useLoginUser()
  const search = useSearchState()

  const params = {
    keyword: search.get('keyword'),
    categoryId: search.getNumber('categoryId'),
    shortageOnly: search.get('shortageOnly') === 'true' || undefined,
    storeId: isAdmin ? search.getNumber('storeId') : undefined,
    page: search.page,
  }
  const { data, isFetching, error } = useQuery({
    queryKey: ['stocks', params],
    queryFn: () => stockApi.search(params),
  })

  return (
    <>
      <PageHeader
        title="현재 재고"
        extra={
          <Button
            icon={<HistoryOutlined />}
            onClick={() => navigate('/stocks/histories')}
          >
            재고 이력
          </Button>
        }
      />
      <SearchForm<StockSearch>
        key={search.params.toString()}
        initialValues={params}
        onSearch={(values) =>
          search.update({
            keyword: values.keyword?.trim(),
            categoryId: values.categoryId,
            shortageOnly: values.shortageOnly,
            storeId: values.storeId,
          })
        }
        onReset={search.reset}
      >
        <Form.Item name="keyword" label="상품">
          <Input
            allowClear
            placeholder="상품명 · 상품코드"
            style={{ width: 180 }}
          />
        </Form.Item>
        <Form.Item name="categoryId" label="카테고리">
          <CategorySelect allowClear placeholder="전체" />
        </Form.Item>
        {isAdmin && (
          <Form.Item name="storeId" label="매장">
            <StoreSelect allowClear placeholder="전체" />
          </Form.Item>
        )}
        <Form.Item name="shortageOnly" valuePropName="checked">
          <Checkbox>부족 재고만 보기</Checkbox>
        </Form.Item>
      </SearchForm>

      <Table<Stock>
        rowKey={(stock) => `${stock.storeId}-${stock.productId}`}
        loading={isFetching}
        dataSource={data?.content}
        pagination={tablePagination(data, search.setPage)}
        locale={{
          emptyText: error ? error.message : '조회된 재고가 없습니다.',
        }}
        onRow={(stock) => ({
          onClick: () =>
            navigate(
              `/stocks/${stock.productId}${isAdmin ? `?storeId=${stock.storeId}` : ''}`,
            ),
          style: { cursor: 'pointer' },
        })}
        columns={[
          { title: '매장', dataIndex: 'storeName' },
          { title: '상품코드', dataIndex: 'productCode' },
          {
            title: '상품명',
            dataIndex: 'productName',
            render: (name: string, stock) => (
              <Space>
                {name}
                {stock.productStatus === 'INACTIVE' && <Tag>사용 중지</Tag>}
              </Space>
            ),
          },
          { title: '카테고리', dataIndex: 'categoryName' },
          {
            title: '현재 수량',
            dataIndex: 'quantity',
            align: 'right',
            render: formatNumber,
          },
          {
            title: '안전재고',
            dataIndex: 'safetyStock',
            align: 'right',
            render: formatNumber,
          },
          {
            title: '부족 여부',
            dataIndex: 'shortage',
            align: 'center',
            render: (shortage: boolean) =>
              shortage ? <Tag color="red">부족</Tag> : null,
          },
        ]}
      />
    </>
  )
}
