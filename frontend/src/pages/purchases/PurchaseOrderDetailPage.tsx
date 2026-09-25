import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Card,
  Col,
  Descriptions,
  Flex,
  Row,
  Skeleton,
  Space,
  Table,
  Timeline,
  Typography,
} from 'antd'
import { useNavigate, useParams } from 'react-router'
import { purchaseApi, type PurchaseOrderAction } from '@/api/purchaseApi'
import { CodeTag } from '@/components/common/CodeTag'
import { PageHeader } from '@/components/common/PageHeader'
import { QueryError } from '@/components/common/QueryError'
import { useApiError } from '@/hooks/useApiError'
import type { PurchaseOrderDetail } from '@/types/domain'
import { PURCHASE_ORDER_STATUS } from '@/utils/codes'
import { formatDateTime, formatMoney, formatNumber } from '@/utils/format'

/** 상태 변경 확인 메시지 — 발주 명세 4장 */
const ACTIONS: Record<
  PurchaseOrderAction,
  {
    label: string
    confirm: (order: PurchaseOrderDetail) => string
    done: string
  }
> = {
  approve: {
    label: '승인',
    confirm: () =>
      '발주를 승인하시겠습니까? 승인 후에는 내용을 수정할 수 없습니다.',
    done: '발주가 승인되었습니다.',
  },
  receive: {
    label: '입고',
    confirm: () =>
      '발주 수량 전량을 입고 처리하시겠습니까? 매장 재고가 증가합니다.',
    done: '입고 처리되었습니다. 매장 재고가 증가했습니다.',
  },
  cancel: {
    label: '발주 취소',
    confirm: (order) => `발주 ${order.orderNumber}를 취소하시겠습니까?`,
    done: '발주가 취소되었습니다.',
  },
}

/** 발주 상세 · 승인 · 입고 · 취소 (SCR-PO-003) */
export function PurchaseOrderDetailPage() {
  const orderId = Number(useParams().purchaseOrderId)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { modal, message } = App.useApp()
  const showError = useApiError()

  const {
    data: order,
    error,
    isLoading,
  } = useQuery({
    queryKey: ['purchaseOrder', orderId],
    queryFn: () => purchaseApi.get(orderId),
  })

  const mutation = useMutation({
    mutationFn: (action: PurchaseOrderAction) =>
      purchaseApi.changeStatus(orderId, action),
    onSuccess: (_, action) => {
      queryClient.invalidateQueries()
      message.success(ACTIONS[action].done)
    },
    onError: (err) => showError(err),
  })

  if (error) return <QueryError error={error} />
  if (isLoading || !order) return <Skeleton active />

  const runAction = (action: PurchaseOrderAction) =>
    modal.confirm({
      title: ACTIONS[action].label,
      content: ACTIONS[action].confirm(order),
      okText: ACTIONS[action].label,
      okButtonProps: action === 'cancel' ? { danger: true } : undefined,
      cancelText: '닫기',
      onOk: () => mutation.mutateAsync(action),
    })

  // 상태별 버튼 — 작성: 수정 · 승인 · 취소 / 승인: 입고 · 취소
  const buttons = (
    <Space>
      {order.status === 'DRAFT' && (
        <>
          <Button onClick={() => navigate(`/purchase-orders/${order.id}/edit`)}>
            수정
          </Button>
          <Button danger onClick={() => runAction('cancel')}>
            발주 취소
          </Button>
          <Button type="primary" onClick={() => runAction('approve')}>
            승인
          </Button>
        </>
      )}
      {order.status === 'APPROVED' && (
        <>
          <Button danger onClick={() => runAction('cancel')}>
            발주 취소
          </Button>
          <Button type="primary" onClick={() => runAction('receive')}>
            입고
          </Button>
        </>
      )}
    </Space>
  )

  const history = [
    {
      label: '작성',
      at: order.orderedAt,
      by: order.createdByName,
      color: 'gray',
    },
    {
      label: '승인',
      at: order.approvedAt,
      by: order.approvedByName,
      color: 'blue',
    },
    {
      label: '입고',
      at: order.completedAt,
      by: order.completedByName,
      color: 'green',
    },
    {
      label: '취소',
      at: order.cancelledAt,
      by: order.cancelledByName,
      color: 'red',
    },
  ].filter((step) => step.at)

  return (
    <>
      <PageHeader title="발주 상세" extra={buttons} />
      <Row gutter={16}>
        <Col xs={24} lg={17}>
          <Card style={{ marginBottom: 16 }}>
            <Descriptions column={{ xs: 1, md: 2 }}>
              <Descriptions.Item label="발주번호">
                {order.orderNumber}
              </Descriptions.Item>
              <Descriptions.Item label="상태">
                <CodeTag value={order.status} codes={PURCHASE_ORDER_STATUS} />
              </Descriptions.Item>
              <Descriptions.Item label="매장">
                {order.storeName}
              </Descriptions.Item>
              <Descriptions.Item label="공급처">
                {order.supplierName}
              </Descriptions.Item>
            </Descriptions>
          </Card>
          <Card title="발주 상품" style={{ marginBottom: 16 }}>
            <Table
              rowKey="productId"
              pagination={false}
              dataSource={order.items}
              columns={[
                { title: '상품코드', dataIndex: 'productCode' },
                { title: '상품명', dataIndex: 'productName' },
                {
                  title: '수량',
                  dataIndex: 'quantity',
                  align: 'right',
                  render: formatNumber,
                },
                {
                  title: '단가',
                  dataIndex: 'unitPrice',
                  align: 'right',
                  render: formatMoney,
                },
                {
                  title: '금액',
                  dataIndex: 'totalPrice',
                  align: 'right',
                  render: formatMoney,
                },
              ]}
            />
            <Flex justify="flex-end" style={{ marginTop: 16 }}>
              <Typography.Text>
                합계{' '}
                <Typography.Text strong style={{ fontSize: 18 }}>
                  {formatMoney(order.totalAmount)}
                </Typography.Text>
              </Typography.Text>
            </Flex>
          </Card>
        </Col>
        <Col xs={24} lg={7}>
          <Card title="처리 이력">
            <Timeline
              items={history.map((step) => ({
                color: step.color,
                content: (
                  <>
                    <Typography.Text strong>{step.label}</Typography.Text>
                    <br />
                    <Typography.Text type="secondary">
                      {formatDateTime(step.at)} · {step.by}
                    </Typography.Text>
                  </>
                ),
              }))}
            />
            {order.updatedByName && (
              <Typography.Text type="secondary">
                마지막 수정: {formatDateTime(order.updatedAt)} ·{' '}
                {order.updatedByName}
              </Typography.Text>
            )}
          </Card>
        </Col>
      </Row>
    </>
  )
}
