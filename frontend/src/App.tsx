import { useEffect, useState } from 'react'

// 개발 환경 확인용 임시 화면. STEP 10(React UI)에서 로그인/라우터 구성으로 교체한다.
type BackendStatus = 'CHECKING' | 'UP' | 'DOWN'

function App() {
  const [status, setStatus] = useState<BackendStatus>('CHECKING')

  useEffect(() => {
    fetch('/actuator/health')
      .then((res) => res.json())
      .then((body: { status: string }) =>
        setStatus(body.status === 'UP' ? 'UP' : 'DOWN'),
      )
      .catch(() => setStatus('DOWN'))
  }, [])

  return (
    <main className="setup">
      <h1>StoreFlow ERP</h1>
      <p>판매·재고·발주 통합 관리 시스템</p>
      <p>
        Backend + DB: <strong className={`status ${status}`}>{status}</strong>
      </p>
    </main>
  )
}

export default App
