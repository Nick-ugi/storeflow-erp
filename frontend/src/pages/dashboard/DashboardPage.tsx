import { ReloadOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Col,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
} from 'antd'
import { Link, useNavigate } from 'react-router'
import { dashboardApi } from '@/api/dashboardApi'
import { CodeTag } from '@/components/common/CodeTag'
import { StoreSelect } from '@/components/common/OptionSelects'
import { PageHeader } from '@/components/common/PageHeader'
import { useLoginUser } from '@/hooks/useLoginUser'
import { useSearchState } from '@/hooks/useSearchState'
import { SalesTrendChart } from '@/pages/dashboard/SalesTrendChart'
import type { Dashboard } from '@/types/domain'
import { PURCHASE_ORDER_STATUS, SALE_STATUS } from '@/utils/codes'
import { formatDateTime, formatMoney, formatNumber } from '@/utils/format'

type ShortageRow = Dashboard['shortageStocks'][number]

/** Dashboard (SCR-DASH-001) */
export function DashboardPage() {
  const navigate = useNavigate()
  const { isAdmin } = useLoginUser()
  const search = useSearchState()
  const storeId = isAdmin ? search.getNumber('storeId') : undefined

  const { data, error, isFetching, refetch } = useQuery({
    queryKey: ['dashboard', storeId],
    queryFn: () => dashboardApi.get(storeId),
  })

  const shortageLink = `/stocks?shortageOnly=true${storeId ? `&storeId=${storeId}` : ''}`

  return (
    <>
      <PageHeader
        title="Dashboard"
        extra={
          <Space>
            {isAdmin && (
              <StoreSelect
                allowClear
                placeholder="전체 매장"
                value={storeId}
                onChange={(value) => search.update({ storeId: value })}
              />
            )}
            <Button
              icon={<ReloadOutlined />}
              loading={isFetching}
              onClick={() => refetch()}
            >
              새로고침
            </Button>
          </Space>
        }
      />
      {error && (
        <Alert
          type="error"
          showIcon
          title={error.message}
          style={{ marginBottom: 16 }}
        />
      )}

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} xl={6}>
          <Card>
            <Statistic
              title="오늘 매출"
              value={formatMoney(data?.todaySales.amount)}
              loading={!data}
            />
            <Typography.Text type="secondary">
              판매 {formatNumber(data?.todaySales.count)}건
            </Typography.Text>
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card>
            <Statistic
              title="이번 달 매출"
              value={formatMoney(data?.monthSales.amount)}
              loading={!data}
            />
            <Typography.Text type="secondary">
              판매 {formatNumber(data?.monthSales.count)}건
            </Typography.Text>
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card>
            <Statistic
              title="재고 보유 상품"
              value={data?.stockSummary.inStockProductCount}
              suffix="개"
              loading={!data}
            />
            <Typography.Text type="secondary">
              현재 수량 1개 이상
            </Typography.Text>
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card hoverable onClick={() => navigate(shortageLink)}>
            <Statistic
              title="부족 재고"
              value={data?.stockSummary.shortageCount}
              suffix="건"
              loading={!data}
            />
            <Typography.Text type="secondary">
              안전재고 미만 · 눌러서 보기
            </Typography.Text>
          </Card>
        </Col>

        <Col span={24}>
          <Card title="최근 7일 매출" size="small">
            {data && <SalesTrendChart data={data.salesTrend} />}
          </Card>
        </Col>

        <Col xs={24} xl={data?.recentPurchaseOrders ? 8 : 12}>
          <Card
            title="재고 부족 상품"
            size="small"
            extra={<Link to={shortageLink}>전체 보기</Link>}
          >
            <Table<ShortageRow>
              size="small"
              pagination={false}
              rowKey={(row) => `${row.storeId}-${row.productId}`}
              dataSource={data?.shortageStocks}
              locale={{ emptyText: '부족한 재고가 없습니다.' }}
              columns={[
                ...(isAdmin && !storeId
                  ? [{ title: '매장', dataIndex: 'storeName' }]
                  : []),
                { title: '상품', dataIndex: 'productName' },
                {
                  title: '현재 / 안전재고',
                  align: 'right',
                  render: (_, row) =>
                    `${formatNumber(row.quantity)} / ${formatNumber(row.safetyStock)}`,
                },
                {
                  title: '부족',
                  dataIndex: 'shortageQuantity',
                  align: 'right',
                  render: (value: number) => (
                    <Tag color="red">{formatNumber(value)}</Tag>
                  ),
                },
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} xl={data?.recentPurchaseOrders ? 8 : 12}>
          <Card
            title="최근 판매"
            size="small"
            extra={<Link to="/sales">판매 내역</Link>}
          >
            <Table
              size="small"
              pagination={false}
              rowKey="id"
              dataSource={data?.recentSales}
              locale={{ emptyText: '판매 내역이 없습니다.' }}
              onRow={(row) => ({
                onClick: () => navigate(`/sales/${row.id}`),
                style: { cursor: 'pointer' },
              })}
              columns={[
                {
                  title: '판매 일시',
                  dataIndex: 'soldAt',
                  render: formatDateTime,
                },
                {
                  title: '금액',
                  dataIndex: 'totalAmount',
                  align: 'right',
                  render: formatMoney,
                },
                {
                  title: '상태',
                  dataIndex: 'status',
                  render: (value) => (
                    <CodeTag value={value} codes={SALE_STATUS} />
                  ),
                },
              ]}
            />
          </Card>
        </Col>
        {data?.recentPurchaseOrders && (
          <Col xs={24} xl={8}>
            <Card
              title="최근 발주"
              size="small"
              extra={<Link to="/purchase-orders">발주 목록</Link>}
            >
              <Table
                size="small"
                pagination={false}
                rowKey="id"
                dataSource={data.recentPurchaseOrders}
                locale={{ emptyText: '발주 내역이 없습니다.' }}
                onRow={(row) => ({
                  onClick: () => navigate(`/purchase-orders/${row.id}`),
                  style: { cursor: 'pointer' },
                })}
                columns={[
                  { title: '공급처', dataIndex: 'supplierName' },
                  {
                    title: '발주 일시',
                    dataIndex: 'orderedAt',
                    render: formatDateTime,
                  },
                  {
                    title: '상태',
                    dataIndex: 'status',
                    render: (value) => (
                      <CodeTag value={value} codes={PURCHASE_ORDER_STATUS} />
                    ),
                  },
                ]}
              />
            </Card>
          </Col>
        )}
      </Row>
    </>
  )
}
