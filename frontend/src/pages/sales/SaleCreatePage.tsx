import {
  DeleteOutlined,
  PlusOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Card,
  Empty,
  Flex,
  InputNumber,
  Space,
  Table,
  Tooltip,
  Typography,
  type TableColumnsType,
} from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { salesApi } from '@/api/salesApi'
import { StoreSelect } from '@/components/common/OptionSelects'
import { PageHeader } from '@/components/common/PageHeader'
import { ProductSelectModal } from '@/components/common/ProductSelectModal'
import { useApiError } from '@/hooks/useApiError'
import { useLoginUser } from '@/hooks/useLoginUser'
import type { Product } from '@/types/domain'
import { formatMoney, formatNumber } from '@/utils/format'

interface CartItem {
  productId: number
  productCode: string
  productName: string
  unitPrice: number
  stockQuantity: number
  quantity: number
}

const MAX_ITEMS = 50

/** 판매 등록 (SCR-SALE-001) — 판매 · 재고 명세 2.1 */
export function SaleCreatePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { modal, message } = App.useApp()
  const showError = useApiError()
  const { user, isAdmin } = useLoginUser()
  const [storeId, setStoreId] = useState<number | null>(
    isAdmin ? null : (user?.storeId ?? null),
  )
  const [items, setItems] = useState<CartItem[]>([])
  const [selecting, setSelecting] = useState(false)

  const totalAmount = items.reduce(
    (sum, item) => sum + item.unitPrice * item.quantity,
    0,
  )
  const overStock = items.some((item) => item.quantity > item.stockQuantity)

  const mutation = useMutation({
    mutationFn: () =>
      salesApi.create({
        storeId: isAdmin ? storeId : null,
        items: items.map((item) => ({
          productId: item.productId,
          quantity: item.quantity,
        })),
      }),
    onSuccess: (result) => {
      queryClient.invalidateQueries()
      message.success(`판매가 완료되었습니다. (${result.saleNumber})`)
      navigate(`/sales/${result.id}`)
    },
    onError: (error) => showError(error),
  })

  /** 이미 담긴 상품을 다시 고르면 새 줄을 만들지 않고 수량을 1 늘린다. */
  const addProducts = (products: Product[]) => {
    setItems((current) => {
      let next = current
      for (const product of products) {
        if (next.some((item) => item.productId === product.id)) {
          next = next.map((item) =>
            item.productId === product.id
              ? { ...item, quantity: Math.min(item.quantity + 1, 9_999) }
              : item,
          )
        } else if (next.length < MAX_ITEMS) {
          next = [
            ...next,
            {
              productId: product.id,
              productCode: product.productCode,
              productName: product.productName,
              unitPrice: product.salePrice,
              stockQuantity: product.stockQuantity,
              quantity: 1,
            },
          ]
        }
      }
      return next
    })
    setSelecting(false)
  }

  const changeQuantity = (productId: number, quantity: number | null) =>
    setItems((current) =>
      current.map((item) =>
        item.productId === productId
          ? { ...item, quantity: quantity ?? 1 }
          : item,
      ),
    )

  const confirmSale = () =>
    modal.confirm({
      title: '판매 완료',
      content: `합계 ${formatMoney(totalAmount)}을 판매 완료하시겠습니까?`,
      okText: '판매 완료',
      onOk: () => mutation.mutateAsync(),
    })

  const columns: TableColumnsType<CartItem> = [
    { title: '상품코드', dataIndex: 'productCode', width: 110 },
    { title: '상품명', dataIndex: 'productName' },
    {
      title: '단가',
      dataIndex: 'unitPrice',
      align: 'right',
      render: formatMoney,
    },
    {
      title: '현재 재고',
      dataIndex: 'stockQuantity',
      align: 'right',
      render: formatNumber,
    },
    {
      title: '수량',
      width: 150,
      render: (_, item) => (
        <Space>
          <InputNumber
            min={1}
            max={9_999}
            precision={0}
            value={item.quantity}
            status={item.quantity > item.stockQuantity ? 'error' : undefined}
            onChange={(value) => changeQuantity(item.productId, value)}
          />
          {item.quantity > item.stockQuantity && (
            <Tooltip title="현재 재고보다 많습니다.">
              <WarningOutlined style={{ color: '#cf1322' }} />
            </Tooltip>
          )}
        </Space>
      ),
    },
    {
      title: '금액',
      align: 'right',
      render: (_, item) => formatMoney(item.unitPrice * item.quantity),
    },
    {
      title: '',
      width: 56,
      render: (_, item) => (
        <Button
          type="text"
          danger
          icon={<DeleteOutlined />}
          aria-label="삭제"
          onClick={() =>
            setItems((current) =>
              current.filter((row) => row.productId !== item.productId),
            )
          }
        />
      ),
    },
  ]

  return (
    <>
      <PageHeader title="판매 등록" />
      <Card>
        <Flex
          justify="space-between"
          align="center"
          wrap
          gap={12}
          style={{ marginBottom: 16 }}
        >
          <Space>
            <Typography.Text strong>매장</Typography.Text>
            {isAdmin ? (
              <StoreSelect
                activeOnly
                value={storeId ?? undefined}
                onChange={(value) => {
                  setStoreId(value)
                  setItems([])
                }}
              />
            ) : (
              <Typography.Text>{user?.storeName}</Typography.Text>
            )}
          </Space>
          <Button
            icon={<PlusOutlined />}
            disabled={!storeId}
            onClick={() => setSelecting(true)}
          >
            상품 선택
          </Button>
        </Flex>

        <Table<CartItem>
          rowKey="productId"
          columns={columns}
          dataSource={items}
          pagination={false}
          locale={{
            emptyText: (
              <Empty description="상품 선택 버튼으로 판매할 상품을 추가하세요." />
            ),
          }}
        />

        <Flex
          justify="flex-end"
          align="center"
          gap={24}
          style={{ marginTop: 16 }}
        >
          <Typography.Text>
            합계{' '}
            <Typography.Text strong style={{ fontSize: 20 }}>
              {formatMoney(totalAmount)}
            </Typography.Text>
          </Typography.Text>
          <Button
            type="primary"
            size="large"
            disabled={items.length === 0 || overStock}
            loading={mutation.isPending}
            onClick={confirmSale}
          >
            판매 완료
          </Button>
        </Flex>
      </Card>

      <ProductSelectModal
        open={selecting}
        mode="SALE"
        storeId={storeId}
        onCancel={() => setSelecting(false)}
        onSelect={addProducts}
      />
    </>
  )
}
