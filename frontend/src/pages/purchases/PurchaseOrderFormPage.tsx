import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Card,
  Empty,
  Flex,
  InputNumber,
  Skeleton,
  Space,
  Table,
  Typography,
  type TableColumnsType,
} from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { purchaseApi } from '@/api/purchaseApi'
import { StoreSelect, SupplierSelect } from '@/components/common/OptionSelects'
import { PageHeader } from '@/components/common/PageHeader'
import { ProductSelectModal } from '@/components/common/ProductSelectModal'
import { QueryError } from '@/components/common/QueryError'
import { useApiError } from '@/hooks/useApiError'
import { useLoginUser } from '@/hooks/useLoginUser'
import type { Product, PurchaseOrderDetail } from '@/types/domain'
import { formatMoney } from '@/utils/format'

interface OrderLine {
  productId: number
  productCode: string
  productName: string
  quantity: number
  unitPrice: number
}

const MAX_ITEMS = 50

/** 발주 등록 · 수정 (SCR-PO-002) — 수정은 '작성' 상태에서만, 매장은 바꿀 수 없다. */
export function PurchaseOrderFormPage() {
  const idParam = useParams().purchaseOrderId
  const orderId = idParam ? Number(idParam) : null
  const {
    data: order,
    error,
    isLoading,
  } = useQuery({
    queryKey: ['purchaseOrder', orderId],
    queryFn: () => purchaseApi.get(orderId!),
    enabled: orderId != null,
  })

  if (orderId != null) {
    if (error) return <QueryError error={error} />
    if (isLoading || !order) return <Skeleton active />
  }
  return <PurchaseOrderForm key={orderId ?? 'new'} order={order} />
}

function PurchaseOrderForm({ order }: { order?: PurchaseOrderDetail }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message } = App.useApp()
  const showError = useApiError()
  const { user, isAdmin } = useLoginUser()
  const editing = order != null

  const [storeId, setStoreId] = useState<number | null>(
    order?.storeId ?? (isAdmin ? null : (user?.storeId ?? null)),
  )
  const [supplierId, setSupplierId] = useState<number | undefined>(
    order?.supplierId,
  )
  const [lines, setLines] = useState<OrderLine[]>(
    order?.items.map(
      ({ productId, productCode, productName, quantity, unitPrice }) => ({
        productId,
        productCode,
        productName,
        quantity,
        unitPrice,
      }),
    ) ?? [],
  )
  const [selecting, setSelecting] = useState(false)

  // '작성'이 아닌 발주는 수정할 수 없다. (명세 3장 화면 동작 4)
  useEffect(() => {
    if (order && order.status !== 'DRAFT') {
      message.warning('작성 상태의 발주만 수정할 수 있습니다.')
      navigate(`/purchase-orders/${order.id}`, { replace: true })
    }
  }, [order, message, navigate])

  const totalAmount = lines.reduce(
    (sum, line) => sum + line.unitPrice * line.quantity,
    0,
  )

  const mutation = useMutation({
    mutationFn: async () => {
      const request = {
        supplierId: supplierId!,
        items: lines.map(({ productId, quantity, unitPrice }) => ({
          productId,
          quantity,
          unitPrice,
        })),
      }
      if (editing) {
        await purchaseApi.update(order.id, request)
        return order.id
      }
      const created = await purchaseApi.create({
        ...request,
        storeId: isAdmin ? storeId : null,
      })
      return created.id
    },
    onSuccess: (id) => {
      queryClient.invalidateQueries()
      message.success(
        editing ? '발주가 수정되었습니다.' : '발주가 등록되었습니다.',
      )
      navigate(`/purchase-orders/${id}`)
    },
    onError: (error) => showError(error),
  })

  /** 이미 담긴 상품은 수량을 1 늘리고, 새 상품의 단가 기본값은 매입가다. (BR-053) */
  const addProducts = (products: Product[]) => {
    setLines((current) => {
      let next = current
      for (const product of products) {
        if (next.some((line) => line.productId === product.id)) {
          next = next.map((line) =>
            line.productId === product.id
              ? { ...line, quantity: Math.min(line.quantity + 1, 99_999) }
              : line,
          )
        } else if (next.length < MAX_ITEMS) {
          next = [
            ...next,
            {
              productId: product.id,
              productCode: product.productCode,
              productName: product.productName,
              quantity: 1,
              unitPrice: product.purchasePrice,
            },
          ]
        }
      }
      return next
    })
    setSelecting(false)
  }

  const updateLine = (productId: number, patch: Partial<OrderLine>) =>
    setLines((current) =>
      current.map((line) =>
        line.productId === productId ? { ...line, ...patch } : line,
      ),
    )

  const columns: TableColumnsType<OrderLine> = [
    { title: '상품코드', dataIndex: 'productCode', width: 110 },
    { title: '상품명', dataIndex: 'productName' },
    {
      title: '수량',
      width: 130,
      render: (_, line) => (
        <InputNumber
          min={1}
          max={99_999}
          precision={0}
          value={line.quantity}
          onChange={(value) =>
            updateLine(line.productId, { quantity: value ?? 1 })
          }
        />
      ),
    },
    {
      title: '단가',
      width: 170,
      render: (_, line) => (
        <InputNumber
          min={0}
          max={99_999_999}
          precision={0}
          value={line.unitPrice}
          suffix="원"
          formatter={(value) =>
            `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
          }
          parser={(value) => Number(value?.replace(/,/g, '') ?? 0)}
          onChange={(value) =>
            updateLine(line.productId, { unitPrice: value ?? 0 })
          }
          style={{ width: 150 }}
        />
      ),
    },
    {
      title: '금액',
      align: 'right',
      render: (_, line) => formatMoney(line.unitPrice * line.quantity),
    },
    {
      title: '',
      width: 56,
      render: (_, line) => (
        <Button
          type="text"
          danger
          icon={<DeleteOutlined />}
          aria-label="삭제"
          onClick={() =>
            setLines((current) =>
              current.filter((row) => row.productId !== line.productId),
            )
          }
        />
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title={editing ? `발주 수정 · ${order.orderNumber}` : '발주 등록'}
      />
      <Card>
        <Flex
          justify="space-between"
          align="center"
          wrap
          gap={12}
          style={{ marginBottom: 16 }}
        >
          <Space wrap size="large">
            <Space>
              <Typography.Text strong>매장</Typography.Text>
              {isAdmin && !editing ? (
                <StoreSelect
                  activeOnly
                  value={storeId ?? undefined}
                  onChange={(value) => setStoreId(value)}
                />
              ) : (
                <Typography.Text>
                  {order?.storeName ?? user?.storeName}
                </Typography.Text>
              )}
            </Space>
            <Space>
              <Typography.Text strong>공급처</Typography.Text>
              <SupplierSelect
                activeOnly
                value={supplierId}
                onChange={(value) => setSupplierId(value)}
              />
            </Space>
          </Space>
          <Button
            icon={<PlusOutlined />}
            disabled={!storeId}
            onClick={() => setSelecting(true)}
          >
            상품 선택
          </Button>
        </Flex>

        <Table<OrderLine>
          rowKey="productId"
          columns={columns}
          dataSource={lines}
          pagination={false}
          locale={{
            emptyText: (
              <Empty description="상품 선택 버튼으로 발주할 상품을 추가하세요." />
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
          <Button onClick={() => navigate(-1)}>취소</Button>
          <Button
            type="primary"
            size="large"
            disabled={!storeId || !supplierId || lines.length === 0}
            loading={mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            저장
          </Button>
        </Flex>
      </Card>

      <ProductSelectModal
        open={selecting}
        mode="PURCHASE"
        storeId={storeId}
        onCancel={() => setSelecting(false)}
        onSelect={addProducts}
      />
    </>
  )
}
