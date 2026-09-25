import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  App,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
} from 'antd'
import { stockApi } from '@/api/stockApi'
import { useApiError } from '@/hooks/useApiError'
import type { StockDetail } from '@/types/domain'
import { formatNumber } from '@/utils/format'

interface StockModalProps {
  open: boolean
  stock: StockDetail
  /** ADMIN이 보고 있는 매장. MANAGER는 null (서버가 소속 매장으로 처리) */
  storeId: number | null
  onClose: () => void
}

interface AdjustForm {
  direction: 'INCREASE' | 'DECREASE'
  amount: number
  reason: string
}

/** 재고 조정 (SCR-STOCK-003) — 조정 후 수량 미리보기, 사유 필수 */
export function StockAdjustModal({
  open,
  stock,
  storeId,
  onClose,
}: StockModalProps) {
  const [form] = Form.useForm<AdjustForm>()
  const { message } = App.useApp()
  const showError = useApiError()
  const queryClient = useQueryClient()
  const direction = Form.useWatch('direction', form)
  const amount = Form.useWatch('amount', form) ?? 0
  const preview = stock.quantity + (direction === 'DECREASE' ? -amount : amount)

  const mutation = useMutation({
    mutationFn: (values: AdjustForm) =>
      stockApi.adjust({
        storeId,
        productId: stock.productId,
        quantity:
          values.direction === 'DECREASE' ? -values.amount : values.amount,
        reason: values.reason,
      }),
    onSuccess: (result) => {
      queryClient.invalidateQueries()
      // 팝업을 연 뒤 다른 판매가 먼저 처리됐을 수 있으므로 서버가 준 실제 수량을 보여준다.
      message.success(
        `재고가 조정되었습니다. (${formatNumber(result.beforeQuantity)}개 → ${formatNumber(result.afterQuantity)}개)`,
      )
      onClose()
    },
    onError: (error) => showError(error, form),
  })

  return (
    <Modal
      title="재고 조정"
      open={open}
      okText="저장"
      okButtonProps={{ disabled: preview < 0 }}
      confirmLoading={mutation.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      destroyOnHidden
    >
      <Descriptions size="small" column={1} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="상품">{stock.productName}</Descriptions.Item>
        <Descriptions.Item label="현재 수량">
          {formatNumber(stock.quantity)}개
        </Descriptions.Item>
      </Descriptions>
      <Form<AdjustForm>
        form={form}
        layout="vertical"
        preserve={false}
        initialValues={{ direction: 'INCREASE', amount: 1 }}
        onFinish={(values) => mutation.mutate(values)}
      >
        <Form.Item name="direction" label="조정 구분">
          <Radio.Group
            optionType="button"
            options={[
              { value: 'INCREASE', label: '증가' },
              { value: 'DECREASE', label: '감소' },
            ]}
          />
        </Form.Item>
        <Form.Item
          name="amount"
          label="조정 수량"
          rules={[{ required: true, message: '조정 수량을 입력하세요.' }]}
        >
          <InputNumber
            min={1}
            max={9_999}
            precision={0}
            style={{ width: 160 }}
          />
        </Form.Item>
        {preview < 0 ? (
          <Alert
            type="error"
            showIcon
            title={`조정 후 재고는 0보다 작을 수 없습니다. (현재 재고 ${formatNumber(stock.quantity)}개)`}
          />
        ) : (
          <Alert
            type="info"
            showIcon
            title={`조정 후 수량: ${formatNumber(preview)}개`}
          />
        )}
        <Form.Item
          name="reason"
          label="조정 사유"
          style={{ marginTop: 16 }}
          rules={[
            {
              required: true,
              whitespace: true,
              message: '조정 사유를 입력하세요.',
            },
            { max: 200, message: '조정 사유는 200자 이하로 입력하세요.' },
          ]}
        >
          <Input.TextArea
            rows={3}
            showCount
            maxLength={200}
            placeholder="예: 파손 3개 폐기, 실사 결과 보정"
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

/** 안전재고 설정 (SCR-STOCK-004) — 0이면 부족 재고 판단을 하지 않는다. */
export function SafetyStockModal({
  open,
  stock,
  storeId,
  onClose,
}: StockModalProps) {
  const [form] = Form.useForm<{ safetyStock: number }>()
  const { message } = App.useApp()
  const showError = useApiError()
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: ({ safetyStock }: { safetyStock: number }) =>
      stockApi.updateSafetyStock(stock.productId, storeId, safetyStock),
    onSuccess: () => {
      queryClient.invalidateQueries()
      message.success('안전재고가 변경되었습니다.')
      onClose()
    },
    onError: (error) => showError(error, form),
  })

  return (
    <Modal
      title="안전재고 설정"
      open={open}
      okText="저장"
      confirmLoading={mutation.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      destroyOnHidden
    >
      <Descriptions size="small" column={1} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="상품">{stock.productName}</Descriptions.Item>
        <Descriptions.Item label="현재 안전재고">
          {formatNumber(stock.safetyStock)}개
        </Descriptions.Item>
      </Descriptions>
      <Form
        form={form}
        layout="vertical"
        preserve={false}
        initialValues={{ safetyStock: stock.safetyStock }}
        onFinish={(values) => mutation.mutate(values)}
      >
        <Form.Item
          name="safetyStock"
          label="새 안전재고"
          extra="0이면 부족 재고로 판단하지 않습니다."
          rules={[{ required: true, message: '안전재고를 입력하세요.' }]}
        >
          <InputNumber
            min={0}
            max={99_999}
            precision={0}
            style={{ width: 160 }}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}
