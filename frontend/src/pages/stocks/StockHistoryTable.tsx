import { Table, Typography, type TablePaginationConfig } from 'antd'
import { Link } from 'react-router'
import { CodeTag } from '@/components/common/CodeTag'
import { useLoginUser } from '@/hooks/useLoginUser'
import type { StockHistory } from '@/types/domain'
import { STOCK_HISTORY_TYPE } from '@/utils/codes'
import {
  formatDateTime,
  formatNumber,
  formatSignedQuantity,
} from '@/utils/format'

interface StockHistoryTableProps {
  histories: StockHistory[] | undefined
  loading?: boolean
  pagination: TablePaginationConfig | false
  showProduct?: boolean
  emptyText?: string
}

/** 재고 이력 표 — 재고 상세의 최근 이력과 재고 이력 화면이 함께 쓴다. */
export function StockHistoryTable({
  histories,
  loading,
  pagination,
  showProduct = true,
  emptyText,
}: StockHistoryTableProps) {
  const { hasRole } = useLoginUser()
  const canViewPurchaseOrder = hasRole('ADMIN', 'MANAGER')

  /** 관련 번호: 판매 → 판매 상세, 발주 → 발주 상세 (USER는 발주 권한이 없어 링크 없이 표시) */
  const renderReference = (history: StockHistory) => {
    if (!history.referenceNumber || !history.referenceId) return '-'
    if (history.referenceType === 'SALE') {
      return (
        <Link to={`/sales/${history.referenceId}`}>
          {history.referenceNumber}
        </Link>
      )
    }
    return canViewPurchaseOrder ? (
      <Link to={`/purchase-orders/${history.referenceId}`}>
        {history.referenceNumber}
      </Link>
    ) : (
      history.referenceNumber
    )
  }

  return (
    <Table<StockHistory>
      rowKey="id"
      size="small"
      loading={loading}
      dataSource={histories}
      pagination={pagination}
      locale={{ emptyText: emptyText ?? '변동 이력이 없습니다.' }}
      scroll={{ x: 'max-content' }}
      columns={[
        { title: '일시', dataIndex: 'createdAt', render: formatDateTime },
        ...(showProduct
          ? [
              { title: '매장', dataIndex: 'storeName' },
              { title: '상품코드', dataIndex: 'productCode' },
              { title: '상품명', dataIndex: 'productName' },
            ]
          : []),
        {
          title: '유형',
          dataIndex: 'type',
          render: (value) => (
            <CodeTag value={value} codes={STOCK_HISTORY_TYPE} />
          ),
        },
        {
          title: '변동 수량',
          dataIndex: 'quantity',
          align: 'right',
          render: (quantity: number) => (
            <Typography.Text type={quantity > 0 ? 'success' : 'danger'}>
              {formatSignedQuantity(quantity)}
            </Typography.Text>
          ),
        },
        {
          title: '변동 전 → 후',
          align: 'right',
          render: (_, history) =>
            `${formatNumber(history.beforeQuantity)} → ${formatNumber(history.afterQuantity)}`,
        },
        {
          title: '관련 번호',
          render: (_, history) => renderReference(history),
        },
        {
          title: '사유',
          dataIndex: 'reason',
          render: (reason: string | null) => reason ?? '-',
        },
        { title: '처리자', dataIndex: 'createdByName' },
      ]}
    />
  )
}
