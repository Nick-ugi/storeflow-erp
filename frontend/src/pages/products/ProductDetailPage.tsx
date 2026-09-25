import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Card, Descriptions, Skeleton, Space } from 'antd'
import { useNavigate, useParams } from 'react-router'
import { productApi } from '@/api/productApi'
import { CodeTag } from '@/components/common/CodeTag'
import { PageHeader } from '@/components/common/PageHeader'
import { QueryError } from '@/components/common/QueryError'
import { useApiError } from '@/hooks/useApiError'
import { useLoginUser } from '@/hooks/useLoginUser'
import { ACTIVE_STATUS } from '@/utils/codes'
import { formatDateTime, formatMoney } from '@/utils/format'

/** 상품 상세 · 사용 중지 (SCR-PROD-002) */
export function ProductDetailPage() {
  const productId = Number(useParams().productId)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { modal, message } = App.useApp()
  const showError = useApiError()
  const { isAdmin } = useLoginUser()

  const {
    data: product,
    error,
    isLoading,
  } = useQuery({
    queryKey: ['product', productId],
    queryFn: () => productApi.get(productId),
  })

  const statusMutation = useMutation({
    mutationFn: (active: boolean) => productApi.setActive(productId, active),
    onSuccess: (_, active) => {
      queryClient.invalidateQueries()
      message.success(
        active ? '상품을 다시 사용합니다.' : '상품을 사용 중지했습니다.',
      )
    },
    onError: (err) => showError(err),
  })

  if (error) return <QueryError error={error} />
  if (isLoading || !product) return <Skeleton active />

  const active = product.status === 'ACTIVE'
  const toggleStatus = () =>
    modal.confirm({
      title: active ? '상품 사용 중지' : '상품 다시 사용',
      content: active
        ? '상품을 사용 중지하시겠습니까? 새 판매 · 발주에서 선택할 수 없게 됩니다. 남은 재고와 기존 이력은 유지됩니다.'
        : '상품을 다시 사용하시겠습니까?',
      okText: active ? '사용 중지' : '다시 사용',
      okButtonProps: active ? { danger: true } : undefined,
      cancelText: '닫기',
      onOk: () => statusMutation.mutateAsync(!active),
    })

  return (
    <>
      <PageHeader
        title="상품 상세"
        extra={
          isAdmin && (
            <Space>
              <Button danger={active} onClick={toggleStatus}>
                {active ? '사용 중지' : '다시 사용'}
              </Button>
              <Button
                type="primary"
                onClick={() => navigate(`/products/${product.id}/edit`)}
              >
                수정
              </Button>
            </Space>
          )
        }
      />
      <Card>
        <Descriptions column={{ xs: 1, md: 2 }} bordered>
          <Descriptions.Item label="상품코드">
            {product.productCode}
          </Descriptions.Item>
          <Descriptions.Item label="상태">
            <CodeTag value={product.status} codes={ACTIVE_STATUS} />
          </Descriptions.Item>
          <Descriptions.Item label="상품명">
            {product.productName}
          </Descriptions.Item>
          <Descriptions.Item label="카테고리">
            {product.categoryName}
          </Descriptions.Item>
          <Descriptions.Item label="매입가">
            {formatMoney(product.purchasePrice)}
          </Descriptions.Item>
          <Descriptions.Item label="판매가">
            {formatMoney(product.salePrice)}
          </Descriptions.Item>
          <Descriptions.Item label="등록 일시">
            {formatDateTime(product.createdAt)}
          </Descriptions.Item>
          <Descriptions.Item label="수정 일시">
            {formatDateTime(product.updatedAt)}
          </Descriptions.Item>
        </Descriptions>
      </Card>
    </>
  )
}
