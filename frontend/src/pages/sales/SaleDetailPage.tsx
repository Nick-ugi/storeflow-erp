import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Card,
  Descriptions,
  Flex,
  Skeleton,
  Table,
  Typography,
} from 'antd'
import { useParams } from 'react-router'
import { salesApi } from '@/api/salesApi'
import { CodeTag } from '@/components/common/CodeTag'
import { PageHeader } from '@/components/common/PageHeader'
import { QueryError } from '@/components/common/QueryError'
import { useApiError } from '@/hooks/useApiError'
import { useLoginUser } from '@/hooks/useLoginUser'
import { SALE_STATUS } from '@/utils/codes'
import { formatDateTime, formatMoney, formatNumber } from '@/utils/format'

/** 판매 상세 · 판매 취소 (SCR-SALE-003) — 판매 · 재고 명세 2.3 */
export function SaleDetailPage() {
  const saleId = Number(useParams().saleId)
  const queryClient = useQueryClient()
  const { modal, message } = App.useApp()
  const showError = useApiError()
  const { hasRole } = useLoginUser()

  const {
    data: sale,
    error,
    isLoading,
  } = useQuery({
    queryKey: ['sale', saleId],
    queryFn: () => salesApi.get(saleId),
  })

  const cancelMutation = useMutation({
    mutationFn: () => salesApi.cancel(saleId),
    onSuccess: () => {
      queryClient.invalidateQueries()
      message.success('판매가 취소되었습니다. 재고가 복원되었습니다.')
    },
    onError: (err) => showError(err),
  })

  if (error) return <QueryError error={error} />
  if (isLoading || !sale) return <Skeleton active />

  const canCancel = hasRole('ADMIN', 'MANAGER') && sale.status === 'COMPLETED'

  return (
    <>
      <PageHeader
        title="판매 상세"
        extra={
          canCancel && (
            <Button
              danger
              loading={cancelMutation.isPending}
              onClick={() =>
                modal.confirm({
                  title: '판매 취소',
                  content: `판매번호 ${sale.saleNumber}를 취소하시겠습니까? 판매 수량만큼 재고가 복원됩니다.`,
                  okText: '판매 취소',
                  okButtonProps: { danger: true },
                  cancelText: '닫기',
                  onOk: () => cancelMutation.mutateAsync(),
                })
              }
            >
              판매 취소
            </Button>
          )
        }
      />
      <Card style={{ marginBottom: 16 }}>
        <Descriptions column={{ xs: 1, md: 3 }}>
          <Descriptions.Item label="판매번호">
            {sale.saleNumber}
          </Descriptions.Item>
          <Descriptions.Item label="매장">{sale.storeName}</Descriptions.Item>
          <Descriptions.Item label="상태">
            <CodeTag value={sale.status} codes={SALE_STATUS} />
          </Descriptions.Item>
          <Descriptions.Item label="판매 일시">
            {formatDateTime(sale.soldAt)}
          </Descriptions.Item>
          <Descriptions.Item label="처리자">
            {sale.createdByName}
          </Descriptions.Item>
          {sale.status === 'CANCELLED' && (
            <Descriptions.Item label="취소">
              {formatDateTime(sale.cancelledAt)} · {sale.cancelledByName}
            </Descriptions.Item>
          )}
        </Descriptions>
      </Card>
      <Card title="판매 상품">
        <Table
          rowKey="productId"
          pagination={false}
          dataSource={sale.items}
          columns={[
            { title: '상품코드', dataIndex: 'productCode' },
            { title: '상품명', dataIndex: 'productName' },
            {
              title: '단가',
              dataIndex: 'unitPrice',
              align: 'right',
              render: formatMoney,
            },
            {
              title: '수량',
              dataIndex: 'quantity',
              align: 'right',
              render: formatNumber,
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
              {formatMoney(sale.totalAmount)}
            </Typography.Text>
          </Typography.Text>
        </Flex>
      </Card>
    </>
  )
}
