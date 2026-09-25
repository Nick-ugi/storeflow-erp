import { Segmented, Table } from 'antd'
import dayjs from 'dayjs'
import { useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { formatMoney } from '@/utils/format'

// 차트 색 · 선 — dataviz 기준 팔레트 (단일 계열, 흰 카드 배경 대비 검증 통과)
const BAR_COLOR = '#2a78d6'
const GRID_COLOR = '#e1e0d9'
const AXIS_COLOR = '#c3c2b7'
const MUTED_INK = '#898781'
const SECONDARY_INK = '#52514e'

const compact = new Intl.NumberFormat('ko-KR', {
  notation: 'compact',
  maximumFractionDigits: 1,
})

/** 축 최댓값을 1 · 2 · 2.5 · 5 × 10ⁿ 으로 올려 눈금이 깔끔한 숫자가 되게 한다. (87,000 → 100,000) */
function niceCeil(dataMax: number): number {
  if (dataMax <= 0) return 100_000
  const magnitude = 10 ** Math.floor(Math.log10(dataMax))
  const fraction = dataMax / magnitude
  const step =
    [1, 2, 2.5, 5, 10].find((candidate) => fraction <= candidate) ?? 10
  return step * magnitude
}

interface SalesTrendChartProps {
  data: { date: string; amount: number }[]
}

/**
 * 최근 7일 일별 매출 (REQ-DASH-003). 단일 계열 세로 막대 — 제목이 계열을 설명하므로 범례는 두지 않고,
 * 값은 오늘 막대에만 직접 표시하며 나머지는 툴팁 · 표 보기로 확인한다.
 */
export function SalesTrendChart({ data }: SalesTrendChartProps) {
  const [view, setView] = useState<'chart' | 'table'>('chart')
  const lastIndex = data.length - 1
  const rows = data.map((day, index) => ({
    ...day,
    label: dayjs(day.date).format('MM-DD (dd)'),
    // 직접 표시할 값은 오늘 막대 하나뿐 — 나머지 행은 값이 없어 라벨이 그려지지 않는다.
    todayLabel: index === lastIndex ? formatMoney(day.amount) : undefined,
  }))

  return (
    <>
      <Segmented
        size="small"
        value={view}
        onChange={(value) => setView(value as 'chart' | 'table')}
        options={[
          { label: '차트', value: 'chart' },
          { label: '표', value: 'table' },
        ]}
        style={{ marginBottom: 12 }}
      />
      {view === 'chart' ? (
        <ResponsiveContainer width="100%" height={260}>
          <BarChart
            data={rows}
            margin={{ top: 24, right: 8, left: 8, bottom: 0 }}
          >
            <CartesianGrid vertical={false} stroke={GRID_COLOR} />
            <XAxis
              dataKey="label"
              tickLine={false}
              axisLine={{ stroke: AXIS_COLOR }}
              tick={{ fill: MUTED_INK, fontSize: 12 }}
            />
            <YAxis
              tickLine={false}
              axisLine={false}
              tick={{ fill: MUTED_INK, fontSize: 12 }}
              tickFormatter={(value: number) => compact.format(value)}
              domain={[0, niceCeil]}
              allowDecimals={false}
              width={56}
            />
            <Tooltip
              cursor={{ fill: 'rgba(0, 0, 0, 0.04)' }}
              formatter={(value) => [formatMoney(Number(value)), '매출']}
              labelStyle={{ color: SECONDARY_INK }}
            />
            <Bar
              dataKey="amount"
              fill={BAR_COLOR}
              maxBarSize={24}
              radius={[4, 4, 0, 0]}
              isAnimationActive={false}
            >
              <LabelList
                dataKey="todayLabel"
                position="top"
                fill={SECONDARY_INK}
                fontSize={12}
              />
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      ) : (
        <Table
          size="small"
          rowKey="date"
          pagination={false}
          dataSource={rows}
          columns={[
            { title: '날짜', dataIndex: 'label' },
            {
              title: '매출',
              dataIndex: 'amount',
              align: 'right',
              render: formatMoney,
            },
          ]}
        />
      )}
    </>
  )
}
