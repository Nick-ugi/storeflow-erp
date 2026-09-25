import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Skeleton,
  Space,
} from 'antd'
import { useNavigate, useParams } from 'react-router'
import { productApi } from '@/api/productApi'
import { CategorySelect } from '@/components/common/OptionSelects'
import { PageHeader } from '@/components/common/PageHeader'
import { QueryError } from '@/components/common/QueryError'
import { useApiError } from '@/hooks/useApiError'
import type { ProductDetail, ProductRequest } from '@/types/domain'

const PRICE_RULE = {
  type: 'number' as const,
  min: 0,
  max: 99_999_999,
  message: '가격은 0 이상 99,999,999 이하로 입력하세요.',
}
const moneyFormatter = (value: number | string | undefined) =>
  `${value ?? ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
const moneyParser = (value: string | undefined) =>
  Number(value?.replace(/,/g, '') ?? 0)

/** 상품 등록 · 수정 (SCR-PROD-003, ADMIN) — 상품코드는 등록 시에만 입력한다. */
export function ProductFormPage() {
  const idParam = useParams().productId
  const productId = idParam ? Number(idParam) : null
  const { data, error, isLoading } = useQuery({
    queryKey: ['product', productId],
    queryFn: () => productApi.get(productId!),
    enabled: productId != null,
  })
  if (productId != null) {
    if (error) return <QueryError error={error} />
    if (isLoading || !data) return <Skeleton active />
  }
  return <ProductForm key={productId ?? 'new'} product={data} />
}

function ProductForm({ product }: { product?: ProductDetail }) {
  const [form] = Form.useForm<ProductRequest>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { modal, message } = App.useApp()
  const showError = useApiError()
  const editing = product != null

  const mutation = useMutation({
    mutationFn: async (values: ProductRequest) => {
      if (editing) {
        await productApi.update(product.id, values)
        return product.id
      }
      return (await productApi.create(values)).id
    },
    onSuccess: (id) => {
      queryClient.invalidateQueries()
      message.success(
        editing
          ? '상품이 수정되었습니다.'
          : '상품이 등록되었습니다. 모든 매장에 재고가 생성되었습니다.',
      )
      navigate(`/products/${id}`)
    },
    onError: (error) => showError(error, form),
  })

  /** 판매가가 매입가보다 낮으면 저장 전에 확인한다. (막지는 않음) */
  const submit = (values: ProductRequest) => {
    if (values.salePrice < values.purchasePrice) {
      modal.confirm({
        title: '가격 확인',
        content: '판매가가 매입가보다 낮습니다. 저장하시겠습니까?',
        okText: '저장',
        cancelText: '다시 입력',
        onOk: () => mutation.mutateAsync(values),
      })
    } else {
      mutation.mutate(values)
    }
  }

  return (
    <>
      <PageHeader title={editing ? '상품 수정' : '상품 등록'} />
      <Card style={{ maxWidth: 640 }}>
        <Form<ProductRequest>
          form={form}
          layout="vertical"
          initialValues={product}
          onFinish={submit}
        >
          <Form.Item
            name="productCode"
            label="상품코드"
            extra={
              editing
                ? '상품코드는 수정할 수 없습니다.'
                : '영문 대문자, 숫자, 하이픈(-) 20자 이하 (예: P-0001)'
            }
            normalize={(value: string) => value?.toUpperCase()}
            rules={[
              { required: true, message: '상품코드를 입력하세요.' },
              {
                pattern: /^[A-Z0-9-]{1,20}$/,
                message:
                  '상품코드는 영문 대문자, 숫자, 하이픈(-)으로 20자 이하로 입력하세요.',
              },
            ]}
          >
            <Input disabled={editing} maxLength={20} />
          </Form.Item>
          <Form.Item
            name="productName"
            label="상품명"
            rules={[
              {
                required: true,
                whitespace: true,
                message: '상품명을 입력하세요.',
              },
              { max: 100, message: '상품명은 100자 이하로 입력하세요.' },
            ]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item
            name="categoryId"
            label="카테고리"
            rules={[{ required: true, message: '카테고리를 선택하세요.' }]}
          >
            <CategorySelect />
          </Form.Item>
          <Space size="large">
            <Form.Item
              name="purchasePrice"
              label="매입가"
              rules={[
                { required: true, message: '매입가를 입력하세요.' },
                PRICE_RULE,
              ]}
            >
              <InputNumber<number>
                min={0}
                precision={0}
                suffix="원"
                formatter={moneyFormatter}
                parser={moneyParser}
                style={{ width: 200 }}
              />
            </Form.Item>
            <Form.Item
              name="salePrice"
              label="판매가"
              rules={[
                { required: true, message: '판매가를 입력하세요.' },
                PRICE_RULE,
              ]}
            >
              <InputNumber<number>
                min={0}
                precision={0}
                suffix="원"
                formatter={moneyFormatter}
                parser={moneyParser}
                style={{ width: 200 }}
              />
            </Form.Item>
          </Space>
          <Space>
            <Button onClick={() => navigate(-1)}>취소</Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={mutation.isPending}
            >
              저장
            </Button>
          </Space>
        </Form>
      </Card>
    </>
  )
}
