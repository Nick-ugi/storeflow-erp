import { PlusOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Form, Input, Select, Table } from 'antd'
import { useNavigate } from 'react-router'
import { productApi } from '@/api/productApi'
import { CodeTag } from '@/components/common/CodeTag'
import { CategorySelect, StoreSelect } from '@/components/common/OptionSelects'
import { PageHeader } from '@/components/common/PageHeader'
import { SearchForm } from '@/components/common/SearchForm'
import { tablePagination } from '@/components/table/pagination'
import { useLoginUser } from '@/hooks/useLoginUser'
import { useSearchState } from '@/hooks/useSearchState'
import type { Product } from '@/types/domain'
import { ACTIVE_STATUS, toOptions } from '@/utils/codes'
import { formatMoney, formatNumber } from '@/utils/format'

interface ProductSearch {
  keyword?: string
  categoryId?: number
  status?: string
  storeId?: number
}

/** 상품 목록 (SCR-PROD-001) — 상태 기본값 '사용', 재고는 소속 매장(ADMIN: 선택 매장 또는 전 매장 합계) */
export function ProductListPage() {
  const navigate = useNavigate()
  const { isAdmin } = useLoginUser()
  const search = useSearchState()

  const status = search.get('status') ?? 'ACTIVE'
  const params = {
    keyword: search.get('keyword'),
    categoryId: search.getNumber('categoryId'),
    status: status === 'ALL' ? undefined : status,
    storeId: isAdmin ? search.getNumber('storeId') : undefined,
    page: search.page,
  }
  const { data, isFetching, error } = useQuery({
    queryKey: ['products', params],
    queryFn: () => productApi.search(params),
  })

  return (
    <>
      <PageHeader
        title="상품"
        extra={
          isAdmin && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => navigate('/products/new')}
            >
              상품 등록
            </Button>
          )
        }
      />
      <SearchForm<ProductSearch>
        key={search.params.toString()}
        initialValues={{ ...params, status }}
        onSearch={(values) =>
          search.update({
            keyword: values.keyword?.trim(),
            categoryId: values.categoryId,
            status: values.status === 'ACTIVE' ? undefined : values.status,
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
        <Form.Item name="status" label="상태">
          <Select
            options={[
              { value: 'ALL', label: '전체' },
              ...toOptions(ACTIVE_STATUS),
            ]}
            style={{ width: 110 }}
          />
        </Form.Item>
        {isAdmin && (
          <Form.Item name="storeId" label="재고 기준 매장">
            <StoreSelect allowClear placeholder="전 매장 합계" />
          </Form.Item>
        )}
      </SearchForm>

      <Table<Product>
        rowKey="id"
        loading={isFetching}
        dataSource={data?.content}
        pagination={tablePagination(data, search.setPage)}
        locale={{
          emptyText: error ? error.message : '조회된 상품이 없습니다.',
        }}
        onRow={(product) => ({
          onClick: () => navigate(`/products/${product.id}`),
          style: { cursor: 'pointer' },
        })}
        columns={[
          { title: '상품코드', dataIndex: 'productCode' },
          { title: '상품명', dataIndex: 'productName' },
          { title: '카테고리', dataIndex: 'categoryName' },
          {
            title: '판매가',
            dataIndex: 'salePrice',
            align: 'right',
            render: formatMoney,
          },
          {
            title: '재고',
            dataIndex: 'stockQuantity',
            align: 'right',
            render: formatNumber,
          },
          {
            title: '상태',
            dataIndex: 'status',
            render: (value) => <CodeTag value={value} codes={ACTIVE_STATUS} />,
          },
        ]}
      />
    </>
  )
}
