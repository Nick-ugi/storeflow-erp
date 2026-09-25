import { useCallback } from 'react'
import { useSearchParams } from 'react-router'

type ParamValue = string | number | boolean | null | undefined

/**
 * 목록 화면의 검색 조건과 페이지를 URL 쿼리에 보관한다. (판매 · 재고 명세 1.6)
 * 뒤로 가기, Dashboard 링크, 새로고침에도 조건이 유지된다.
 */
export function useSearchState() {
  const [params, setParams] = useSearchParams()

  const get = useCallback(
    (key: string) => params.get(key) ?? undefined,
    [params],
  )
  const getNumber = useCallback(
    (key: string) => {
      const value = params.get(key)
      return value == null || value === '' ? undefined : Number(value)
    },
    [params],
  )
  const page = Number(params.get('page') ?? '0')

  /** 조건을 바꾸면 첫 페이지로 돌아간다. 빈 값 · false는 URL에서 뺀다. */
  const update = useCallback(
    (values: Record<string, ParamValue>, resetPage = true) => {
      setParams((prev) => {
        const next = new URLSearchParams(prev)
        Object.entries(values).forEach(([key, value]) => {
          if (
            value === undefined ||
            value === null ||
            value === '' ||
            value === false
          ) {
            next.delete(key)
          } else {
            next.set(key, String(value))
          }
        })
        if (resetPage) next.delete('page')
        return next
      })
    },
    [setParams],
  )

  const reset = useCallback(() => setParams(new URLSearchParams()), [setParams])
  const setPage = useCallback(
    (value: number) => update({ page: value || undefined }, false),
    [update],
  )

  return { params, get, getNumber, page, update, reset, setPage }
}
