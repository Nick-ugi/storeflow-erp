import { useQuery } from '@tanstack/react-query'
import { Input, Modal, Space, Table, Tag, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { productApi } from '@/api/productApi'
import { CategorySelect } from '@/components/common/OptionSelects'
import { tablePagination } from '@/components/table/pagination'
import type { Product } from '@/types/domain'
import { formatMoney, formatNumber } from '@/utils/format'

interface ProductSelectModalProps {
  open: boolean
  /** 판매: 판매가 표시, 재고 0이면 선택 불가 / 발주: 매입가 표시 */
  mode: 'SALE' | 'PURCHASE'
  /** 현재 재고 표시 기준 매장 (ADMIN이 선택한 매장. MANAGER · USER는 서버가 소속 매장으로 처리) */
  storeId: number | null
  onCancel: () => void
  onSelect: (products: Product[]) => void
}

/**
 * 상품 선택 팝업 (SCR-PROD-004) — 판매 등록 · 발주 등록/수정 공통
 * 사용 중인 상품만 표시하고, 여러 개를 골라 한 번에 추가한다.
 */
export function ProductSelectModal({
  open,
  mode,
  storeId,
  onCancel,
  onSelect,
}: ProductSelectModalProps) {
  const [keyword, setKeyword] = useState<string>()
  const [categoryId, setCategoryId] = useState<number>()
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<Product[]>([])

  const close = (products?: Product[]) => {
    setSelected([])
    if (products) {
      onSelect(products)
    } else {
      onCancel()
    }
  }

  const { data, isFetching } = useQuery({
    queryKey: ['products', 'select', { keyword, categoryId, page, storeId }],
    queryFn: () =>
      productApi.search({
        keyword,
        categoryId,
        status: 'ACTIVE',
        storeId,
        page,
        size: 10,
      }),
    enabled: open,
  })

  const columns: TableColumnsType<Product> = [
    { title: '상품코드', dataIndex: 'productCode', width: 110 },
    { title: '상품명', dataIndex: 'productName' },
    { title: '카테고리', dataIndex: 'categoryName', width: 100 },
    mode === 'SALE'
      ? {
          title: '판매가',
          dataIndex: 'salePrice',
          align: 'right',
          render: formatMoney,
        }
      : {
          title: '매입가',
          dataIndex: 'purchasePrice',
          align: 'right',
          render: formatMoney,
        },
    {
      title: '현재 재고',
      dataIndex: 'stockQuantity',
      align: 'right',
      render: (quantity: number) =>
        mode === 'SALE' && quantity === 0 ? (
          <Tag color="red">재고 없음</Tag>
        ) : (
          formatNumber(quantity)
        ),
    },
  ]

  return (
    <Modal
      title="상품 선택"
      open={open}
      width={760}
      okText={`선택 완료 (${selected.length})`}
      okButtonProps={{ disabled: selected.length === 0 }}
      onOk={() => close(selected)}
      onCancel={() => close()}
      destroyOnHidden
    >
      <Space wrap style={{ marginBottom: 12 }}>
        <Input.Search
          placeholder="상품명 · 상품코드"
          allowClear
          onSearch={(value) => {
            setKeyword(value || undefined)
            setPage(0)
          }}
          style={{ width: 240 }}
        />
        <CategorySelect
          allowClear
          value={categoryId}
          onChange={(value) => {
            setCategoryId(value)
            setPage(0)
          }}
        />
      </Space>
      <Table<Product>
        rowKey="id"
        size="small"
        loading={isFetching}
        columns={columns}
        dataSource={data?.content}
        pagination={tablePagination(data, setPage)}
        rowSelection={{
          preserveSelectedRowKeys: true,
          selectedRowKeys: selected.map((product) => product.id),
          onChange: (_keys, rows) => setSelected(rows),
          getCheckboxProps: (product) => ({
            disabled: mode === 'SALE' && product.stockQuantity === 0,
          }),
        }}
      />
    </Modal>
  )
}
