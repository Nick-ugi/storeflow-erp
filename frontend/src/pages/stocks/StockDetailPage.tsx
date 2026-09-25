import { useQuery } from '@tanstack/react-query'
import { Button, Card, Descriptions, Skeleton, Space, Tag } from 'antd'
import dayjs from 'dayjs'
import { useState } from 'react'
import { Link, Navigate, useParams, useSearchParams } from 'react-router'
import { stockApi } from '@/api/stockApi'
import { CodeTag } from '@/components/common/CodeTag'
import { PageHeader } from '@/components/common/PageHeader'
import { QueryError } from '@/components/common/QueryError'
import { useLoginUser } from '@/hooks/useLoginUser'
import { StockHistoryTable } from '@/pages/stocks/StockHistoryTable'
import { SafetyStockModal, StockAdjustModal } from '@/pages/stocks/StockModals'
import { ACTIVE_STATUS } from '@/utils/codes'
import { formatNumber } from '@/utils/format'

/** 재고 상세 (SCR-STOCK-002) — 최근 변동 이력 10건, 재고 조정 · 안전재고 설정 */
export function StockDetailPage() {
  const productId = Number(useParams().productId)
  const [params] = useSearchParams()
  const { isAdmin, hasRole } = useLoginUser()
  const storeId = isAdmin ? Number(params.get('storeId')) || null : null
  const [modal, setModal] = useState<'adjust' | 'safety' | null>(null)

  const {
    data: stock,
    error,
    isLoading,
  } = useQuery({
    queryKey: ['stock', productId, storeId],
    queryFn: () => stockApi.get(productId, storeId),
    enabled: !isAdmin || storeId != null,
  })

  // ADMIN이 매장 지정 없이 들어오면 현재 재고 화면으로 보낸다. (명세 3.2)
  if (isAdmin && storeId == null) return <Navigate to="/stocks" replace />
  if (error) return <QueryError error={error} />
  if (isLoading || !stock) return <Skeleton active />

  const canEdit = hasRole('ADMIN', 'MANAGER')
  // 전체 이력은 조회 가능한 최대 기간(1년)으로 연다.
  const historyStart = dayjs().subtract(1, 'year').format('YYYY-MM-DD')
  const historyLink = `/stocks/histories?productId=${productId}${storeId ? `&storeId=${storeId}` : ''}&startDate=${historyStart}`

  return (
    <>
      <PageHeader
        title="재고 상세"
        extra={
          canEdit && (
            <Space>
              <Button onClick={() => setModal('safety')}>안전재고 설정</Button>
              <Button type="primary" onClick={() => setModal('adjust')}>
                재고 조정
              </Button>
            </Space>
          )
        }
      />
      <Card style={{ marginBottom: 16 }}>
        <Descriptions column={{ xs: 1, md: 3 }}>
          <Descriptions.Item label="매장">{stock.storeName}</Descriptions.Item>
          <Descriptions.Item label="상품코드">
            {stock.productCode}
          </Descriptions.Item>
          <Descriptions.Item label="상품명">
            {stock.productName}
          </Descriptions.Item>
          <Descriptions.Item label="카테고리">
            {stock.categoryName}
          </Descriptions.Item>
          <Descriptions.Item label="상품 상태">
            <CodeTag value={stock.productStatus} codes={ACTIVE_STATUS} />
          </Descriptions.Item>
          <Descriptions.Item label="부족 여부">
            {stock.shortage ? <Tag color="red">부족</Tag> : '정상'}
          </Descriptions.Item>
          <Descriptions.Item label="현재 수량">
            <strong>{formatNumber(stock.quantity)}개</strong>
          </Descriptions.Item>
          <Descriptions.Item label="안전재고">
            {formatNumber(stock.safetyStock)}개
          </Descriptions.Item>
        </Descriptions>
      </Card>
      <Card
        title="최근 변동 이력 (10건)"
        extra={<Link to={historyLink}>전체 이력 보기</Link>}
      >
        <StockHistoryTable
          histories={stock.recentHistories}
          pagination={false}
          showProduct={false}
        />
      </Card>

      <StockAdjustModal
        open={modal === 'adjust'}
        stock={stock}
        storeId={storeId}
        onClose={() => setModal(null)}
      />
      <SafetyStockModal
        open={modal === 'safety'}
        stock={stock}
        storeId={storeId}
        onClose={() => setModal(null)}
      />
    </>
  )
}
