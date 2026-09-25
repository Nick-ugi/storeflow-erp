import { useMutation } from '@tanstack/react-query'
import { App, Form, Input, Modal } from 'antd'
import { authApi } from '@/api/authApi'
import { useApiError } from '@/hooks/useApiError'

interface PasswordForm {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

const PASSWORD_RULE = {
  pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/,
  message: '비밀번호는 영문과 숫자를 포함해 8~20자로 입력하세요.',
}

/** 비밀번호 변경 (SCR-AUTH-002) */
export function PasswordChangeModal({
  open,
  onClose,
}: {
  open: boolean
  onClose: () => void
}) {
  const [form] = Form.useForm<PasswordForm>()
  const { message } = App.useApp()
  const showError = useApiError()
  const mutation = useMutation({
    mutationFn: (values: PasswordForm) =>
      authApi.changePassword(values.currentPassword, values.newPassword),
    onSuccess: () => {
      message.success('비밀번호가 변경되었습니다.')
      onClose()
    },
    onError: (error) => showError(error, form),
  })

  return (
    <Modal
      title="비밀번호 변경"
      open={open}
      okText="변경"
      confirmLoading={mutation.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={(values) => mutation.mutate(values)}
        preserve={false}
      >
        <Form.Item
          name="currentPassword"
          label="현재 비밀번호"
          rules={[{ required: true, message: '현재 비밀번호를 입력하세요.' }]}
        >
          <Input.Password autoComplete="current-password" />
        </Form.Item>
        <Form.Item
          name="newPassword"
          label="새 비밀번호"
          rules={[
            { required: true, message: '새 비밀번호를 입력하세요.' },
            PASSWORD_RULE,
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label="새 비밀번호 확인"
          dependencies={['newPassword']}
          rules={[
            { required: true, message: '새 비밀번호를 한 번 더 입력하세요.' },
            ({ getFieldValue }) => ({
              validator: (_, value) =>
                !value || value === getFieldValue('newPassword')
                  ? Promise.resolve()
                  : Promise.reject(
                      new Error('새 비밀번호가 일치하지 않습니다.'),
                    ),
            }),
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
