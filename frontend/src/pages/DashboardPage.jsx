import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const ROLE_LABELS = {
  ADMIN_INSTITUCION: 'Administrador',
  DIRECTOR: 'Director',
  DOCENTE: 'Docente',
  ESTUDIANTE: 'Estudiante',
  TECNICO: 'Técnico',
  AUDITOR: 'Auditor',
}

export default function DashboardPage() {
  const { auth, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const roleLabel = auth?.roles?.map((r) => ROLE_LABELS[r] ?? r).join(', ')

  return (
    <div className="dashboard-wrapper">
      <header className="topbar">
        <div className="topbar-brand">
          <span className="logo-icon">⚡</span>
          <span>EnergíaClara AI</span>
        </div>
        <div className="topbar-user">
          <span className="user-email">{auth?.email}</span>
          <span className="role-badge">{roleLabel}</span>
          <button onClick={handleLogout} className="btn-logout">Salir</button>
        </div>
      </header>

      <main className="dashboard-main">
        <div className="welcome-card">
          <h2>Panel de control</h2>
          <p>
            Institución: <code>{auth?.tenantId}</code>
          </p>
          <div className="modules-grid">
            <div className="module-card coming">
              <span className="module-icon">⚡</span>
              <h3>Consumo</h3>
              <p>Lecturas y medidores</p>
            </div>
            <div className="module-card coming">
              <span className="module-icon">🔍</span>
              <h3>Anomalías</h3>
              <p>Detección y alertas</p>
            </div>
            <div className="module-card coming">
              <span className="module-icon">🔧</span>
              <h3>Mantenimiento</h3>
              <p>Tickets y técnicos</p>
            </div>
          </div>
          <p className="coming-soon">Módulos en desarrollo — próxima entrega.</p>
        </div>
      </main>
    </div>
  )
}
