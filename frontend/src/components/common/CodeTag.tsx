import { Tag } from 'antd'

interface CodeTagProps<K extends string> {
  value: K | null | undefined
  codes: Record<K, { label: string; color: string }>
}

/** 상태 · 유형 코드를 색이 있는 태그로 표시한다. */
export function CodeTag<K extends string>({ value, codes }: CodeTagProps<K>) {
  if (!value) return null
  const info = codes[value]
  return <Tag color={info.color}>{info.label}</Tag>
}
