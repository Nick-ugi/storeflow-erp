import { useQuery } from '@tanstack/react-query'
import { Select, type SelectProps } from 'antd'
import { categoryApi, supplierApi } from '@/api/productApi'
import { storeApi } from '@/api/systemApi'

interface OptionSelectProps extends Omit<SelectProps, 'options'> {
  /** 등록 화면: 사용 중인 항목만 / 검색 조건: 사용 중지 포함 */
  activeOnly?: boolean
}

/** 매장 선택 (ADMIN 전용 API-STORE-002) */
export function StoreSelect({ activeOnly, ...props }: OptionSelectProps) {
  const { data = [], isLoading } = useQuery({
    queryKey: ['storeOptions'],
    queryFn: storeApi.options,
  })
  const options = data
    .filter((store) => !activeOnly || store.status === 'ACTIVE')
    .map((store) => ({
      value: store.id,
      label:
        store.status === 'ACTIVE'
          ? store.storeName
          : `${store.storeName} (사용 중지)`,
    }))
  return (
    <Select
      placeholder="매장 선택"
      loading={isLoading}
      options={options}
      style={{ minWidth: 160 }}
      {...props}
    />
  )
}

/** 카테고리 선택 (API-CAT-001) */
export function CategorySelect(props: Omit<SelectProps, 'options'>) {
  const { data = [], isLoading } = useQuery({
    queryKey: ['categories'],
    queryFn: categoryApi.list,
  })
  const options = data.map((category) => ({
    value: category.id,
    label: category.categoryName,
  }))
  return (
    <Select
      placeholder="카테고리"
      loading={isLoading}
      options={options}
      style={{ minWidth: 140 }}
      {...props}
    />
  )
}

/** 공급처 선택 (ADMIN · MANAGER API-SUPP-002) */
export function SupplierSelect({ activeOnly, ...props }: OptionSelectProps) {
  const { data = [], isLoading } = useQuery({
    queryKey: ['supplierOptions'],
    queryFn: supplierApi.options,
  })
  const options = data
    .filter((supplier) => !activeOnly || supplier.status === 'ACTIVE')
    .map((supplier) => ({
      value: supplier.id,
      label:
        supplier.status === 'ACTIVE'
          ? supplier.supplierName
          : `${supplier.supplierName} (사용 중지)`,
    }))
  return (
    <Select
      placeholder="공급처 선택"
      loading={isLoading}
      options={options}
      showSearch={{ optionFilterProp: 'label' }}
      style={{ minWidth: 180 }}
      {...props}
    />
  )
}
