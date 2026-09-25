import { App, type FormInstance } from 'antd'
import { useCallback } from 'react'
import { ApiError } from '@/api/client'

/** "items[0].quantity" → ['items', 0, 'quantity'] */
function toNamePath(field: string): (string | number)[] {
  return field
    .split(/[.[\]]/)
    .filter(Boolean)
    .map((part) => (/^\d+$/.test(part) ? Number(part) : part))
}

/**
 * API 오류를 화면에 보여준다. 메시지는 서버가 준 한글 메시지를 그대로 쓰고,
 * 폼이 주어지면 fieldErrors를 해당 입력 항목 아래에 표시한다. (API 공통 규칙 6장)
 */
export function useApiError() {
  const { message } = App.useApp()
  return useCallback(
    (error: unknown, form?: FormInstance) => {
      if (!(error instanceof ApiError)) {
        message.error('알 수 없는 오류가 발생했습니다.')
        return
      }
      if (form && error.fieldErrors.length > 0) {
        const fieldNames = new Set(Object.keys(form.getFieldsValue(true)))
        const fields = error.fieldErrors
          .map((fieldError) => ({
            name: toNamePath(fieldError.field),
            errors: [fieldError.message],
          }))
          .filter((field) => fieldNames.has(String(field.name[0])))
        form.setFields(fields)
      }
      message.error(error.message)
    },
    [message],
  )
}
