import { useCallback, useEffect, useState } from 'react'
import api, { getApiError } from './api'
import './App.css'

const categories = [
  'MEDICAL_EMERGENCY',
  'FIRE_OR_SMOKE',
  'SUSPICIOUS_ACTIVITY',
  'PHYSICAL_ASSAULT_OR_HARASSMENT',
  'THEFT_OR_PROPERTY_DAMAGE',
  'INFRASTRUCTURE_HAZARD',
  'NATURAL_DISASTER',
  'OTHER_SAFETY_CONCERN',
]
const severities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
const statuses = ['ACKNOWLEDGED', 'RESPONDING', 'ON_SCENE', 'RESOLVED', 'CLOSED']
const tabs = ['Overview', 'Incidents', 'Alerts', 'Notifications', 'Profile']

function formatLabel(value = '') {
  return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
}

function AuthScreen({ onAuthenticated }) {
  const [registering, setRegistering] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    const values = Object.fromEntries(new FormData(event.currentTarget))
    try {
      const { data } = await api.post(registering ? '/auth/register' : '/auth/login', values)
      sessionStorage.setItem('campus-safety-token', data.token)
      onAuthenticated(data)
    } catch (requestError) {
      setError(getApiError(requestError))
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="auth-screen">
      <section className="auth-card">
        <p className="eyebrow accent">Campus safety command</p>
        <h1>Campus Shield</h1>
        <p className="auth-intro">Sign in to report an incident, coordinate a response, or review campus safety activity.</p>
        {error && <div className="error-banner" role="alert">{error}</div>}
        <form className="form-stack" onSubmit={submit}>
          {registering && <>
            <label>First name<input name="firstName" autoComplete="given-name" required /></label>
            <label>Last name<input name="lastName" autoComplete="family-name" required /></label>
          </>}
          <label>Email<input name="email" type="email" autoComplete="email" required /></label>
          <label>Password<input name="password" type="password" minLength="8" autoComplete={registering ? 'new-password' : 'current-password'} required /></label>
          {registering && <p className="form-hint">New accounts are created with student access. Campus administrators assign staff roles.</p>}
          <button className="primary-button" disabled={busy}>{busy ? 'Please wait…' : registering ? 'Create student account' : 'Sign in'}</button>
        </form>
        <button className="link-button auth-toggle" type="button" onClick={() => { setRegistering(!registering); setError('') }}>
          {registering ? 'Already have an account? Sign in' : 'Need an account? Register as a student'}
        </button>
      </section>
    </main>
  )
}

function App() {
  const [user, setUser] = useState(null)
  const [activeTab, setActiveTab] = useState('Overview')
  const [incidents, setIncidents] = useState([])
  const [alerts, setAlerts] = useState([])
  const [notifications, setNotifications] = useState([])
  const [contacts, setContacts] = useState([])
  const [adminDashboard, setAdminDashboard] = useState(null)
  const [analytics, setAnalytics] = useState(null)
  const [insights, setInsights] = useState(null)
  const [users, setUsers] = useState([])
  const [officers, setOfficers] = useState([])
  const [auditLogs, setAuditLogs] = useState([])
  const [selectedIncident, setSelectedIncident] = useState(null)
  const [incidentHistory, setIncidentHistory] = useState([])
  const [noteDraft, setNoteDraft] = useState('')
  const [assignmentSelection, setAssignmentSelection] = useState({})
  const [incidentFilters, setIncidentFilters] = useState({})
  const [recommendation, setRecommendation] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadWorkspace = useCallback(async (currentUser, filters = {}) => {
    setLoading(true)
    setError('')
    try {
      const requests = [
        api.get('/incidents', { params: { page: 0, size: 100, ...filters } }),
        api.get('/alerts'),
        api.get('/notifications'),
        api.get('/emergency-contacts'),
      ]
      const responses = await Promise.all(requests)
      setIncidents(responses[0].data.content || [])
      setAlerts(responses[1].data)
      setNotifications(responses[2].data)
      setContacts(responses[3].data)
      if (currentUser.role === 'SECURITY_OFFICER' || currentUser.role === 'ADMIN') {
        const { data } = await api.get('/officers')
        setOfficers(data)
      }
      if (currentUser.role === 'ADMIN') {
        const adminData = await Promise.all([
          api.get('/admin/dashboard'),
          api.get('/admin/analytics'),
          api.get('/ai/safety-insights'),
          api.get('/admin/users'),
          api.get('/admin/audit-logs'),
        ])
        setAdminDashboard(adminData[0].data)
        setAnalytics(adminData[1].data)
        setInsights(adminData[2].data)
        setUsers(adminData[3].data)
        setAuditLogs(adminData[4].data)
      }
    } catch (requestError) {
      setError(getApiError(requestError))
      if (requestError.response?.status === 401) signOut()
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!sessionStorage.getItem('campus-safety-token')) return
    api.get('/auth/me')
      .then(({ data }) => setUser(data))
      .catch((requestError) => {
        sessionStorage.removeItem('campus-safety-token')
        setError(getApiError(requestError))
      })
  }, [])

  useEffect(() => {
    if (user) queueMicrotask(() => loadWorkspace(user))
  }, [user, loadWorkspace])

  function signOut() {
    sessionStorage.removeItem('campus-safety-token')
    setUser(null)
    setIncidents([])
    setAlerts([])
    setNotifications([])
  }

  async function perform(action, successMessage) {
    setError('')
    setNotice('')
    try {
      await action()
      if (successMessage) setNotice(successMessage)
      if (user) await loadWorkspace(user, incidentFilters)
    } catch (requestError) {
      setError(getApiError(requestError))
    }
  }

  async function submitIncident(event) {
    event.preventDefault()
    const form = event.currentTarget
    const values = Object.fromEntries(new FormData(form))
    await perform(async () => {
      await api.post('/incidents', {
        ...values,
      })
      setRecommendation(null)
      form.reset()
    }, 'Incident submitted and saved.')
  }

  async function analyzeIncident(event) {
    event.preventDefault()
    const description = new FormData(event.currentTarget.form).get('description')
    if (!description?.trim()) {
      setError('Enter a description before requesting an AI recommendation.')
      return
    }
    await perform(async () => {
      const { data } = await api.post('/ai/classify-incident', { description })
      setRecommendation(data)
    })
  }

  async function updateIncidentStatus(incident, status) {
    await perform(() => api.patch(`/incidents/${incident.id}/status`, { status }), `Incident #${incident.id} updated.`)
  }

  async function assignIncident(incident, officerId) {
    await perform(() => api.patch(`/incidents/${incident.id}/assign`, { officerId: Number(officerId) }), `Incident #${incident.id} assignment updated.`)
  }

  async function viewHistory(incident) {
    setSelectedIncident(incident)
    setIncidentHistory([])
    try {
      const { data } = await api.get(`/incidents/${incident.id}/history`)
      setIncidentHistory(data)
    } catch (requestError) {
      setError(getApiError(requestError))
    }
  }

  async function submitNote(event) {
    event.preventDefault()
    if (!selectedIncident || !noteDraft.trim()) return
    await perform(async () => {
      await api.post(`/incidents/${selectedIncident.id}/notes`, { note: noteDraft.trim() })
      setNoteDraft('')
      const { data } = await api.get(`/incidents/${selectedIncident.id}/history`)
      setIncidentHistory(data)
    }, 'Response note added.')
  }

  async function markRead(notification) {
    await perform(() => api.patch(`/notifications/${notification.id}/read`), 'Notification marked as read.')
  }

  async function submitAlert(event) {
    event.preventDefault()
    const form = event.currentTarget
    const values = Object.fromEntries(new FormData(form))
    values.expiresAt = new Date(values.expiresAt).toISOString()
    await perform(async () => {
      await api.post('/alerts', values)
      form.reset()
    }, 'Campus alert published.')
  }

  async function updateUserRole(target, role) {
    await perform(() => api.patch(`/admin/users/${target.id}/role`, null, { params: { role } }), `Role updated for ${target.email}.`)
  }

  async function filterIncidentList(key, value) {
    const filters = { ...incidentFilters }
    if (value) filters[key] = value
    else delete filters[key]
    setIncidentFilters(filters)
    setLoading(true)
    setError('')
    try {
      const { data } = await api.get('/incidents', { params: { page: 0, size: 100, ...filters } })
      setIncidents(data.content || [])
    } catch (requestError) {
      setError(getApiError(requestError))
    } finally {
      setLoading(false)
    }
  }

  async function updateProfile(event) {
    event.preventDefault()
    const values = Object.fromEntries(new FormData(event.currentTarget))
    await perform(async () => {
      const { data } = await api.patch('/auth/me', values)
      setUser(data)
    }, 'Profile updated.')
  }

  async function createContact(event) {
    event.preventDefault()
    const form = event.currentTarget
    const values = Object.fromEntries(new FormData(form))
    values.priority = Number(values.priority)
    if (!values.email) delete values.email
    await perform(async () => {
      await api.post('/emergency-contacts', values)
      form.reset()
    }, 'Emergency contact saved.')
  }

  if (!user) return <AuthScreen onAuthenticated={setUser} />

  const isResponder = user.role === 'SECURITY_OFFICER' || user.role === 'ADMIN'
  const openIncidents = incidents.filter((incident) => !['RESOLVED', 'CLOSED'].includes(incident.status))
  const unreadCount = notifications.filter((notification) => !notification.readStatus).length

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Campus safety command · {formatLabel(user.role)}</p>
          <h1>Campus Shield</h1>
        </div>
        <div className="topbar-actions">
          <span className="user-chip">{user.firstName} {user.lastName}</span>
          <button className="ghost-button" type="button" onClick={signOut}>Sign out</button>
        </div>
      </header>

      <nav className="workspace-nav" aria-label="Main navigation">
        {tabs.map((tab) => <button key={tab} className={activeTab === tab ? 'nav-button active' : 'nav-button'} onClick={() => setActiveTab(tab)} type="button">
          {tab}{tab === 'Notifications' && unreadCount > 0 && <span className="nav-count">{unreadCount}</span>}
        </button>)}
      </nav>

      {error && <div className="error-banner" role="alert">{error}<button type="button" onClick={() => setError('')} aria-label="Dismiss error">×</button></div>}
      {notice && <div className="success-banner" role="status">{notice}</div>}
      {loading && <p className="loading-note" role="status">Loading current campus data…</p>}

      {activeTab === 'Overview' && <main className="dashboard-grid">
        <section className="panel hero-panel">
          <div><p className="eyebrow accent">Operational overview</p><h2>Review current incidents and coordinate a timely campus response.</h2></div>
          <div className="hero-badge"><span className="status-dot" />Live data from campus services</div>
        </section>
        <section className="stats-grid">
          <Stat label="Open incidents" value={isResponder ? openIncidents.length : incidents.length} note={isResponder ? 'Current response queue' : 'Your reported incidents'} tone="warning" />
          <Stat label="Active alerts" value={alerts.length} note="Published and not expired" tone="danger" />
          <Stat label="Unread notifications" value={unreadCount} note="For your account" tone="neutral" />
          <Stat label="Response time" value={adminDashboard?.averageResponseTimeMinutes == null ? '—' : `${adminDashboard.averageResponseTimeMinutes.toFixed(1)} min`} note={adminDashboard ? `${adminDashboard.responseTimeSampleSize} acknowledged incidents` : 'Admin metric only'} tone="ok" />
        </section>
        <section className="panel incidents-panel">
          <div className="panel-header"><h3>{isResponder ? 'Active incidents' : 'My recent incidents'}</h3><button className="link-button" type="button" onClick={() => setActiveTab('Incidents')}>View all</button></div>
          <IncidentList incidents={incidents.slice(0, 5)} responder={isResponder} officers={officers} assignmentSelection={assignmentSelection} onSelectOfficer={(id, officerId) => setAssignmentSelection((current) => ({ ...current, [id]: officerId }))} onStatus={updateIncidentStatus} onAssign={assignIncident} onHistory={viewHistory} />
        </section>
        <section className="panel alerts-panel">
          <div className="panel-header"><h3>Active alerts</h3><button className="link-button" type="button" onClick={() => setActiveTab('Alerts')}>View all</button></div>
          <AlertList alerts={alerts.slice(0, 5)} />
        </section>
        {user.role === 'ADMIN' && <>
          <section className="panel analytics-panel">
            <div className="panel-header"><h3>Administrative analytics</h3></div>
            <div className="signal-grid">
              <Stat label="Total incidents" value={adminDashboard?.totalIncidents ?? '—'} note="Stored reports" />
              <Stat label="Active response" value={adminDashboard?.activeEmergencies ?? '—'} note="New through on-scene" />
              <Stat label="Resolved" value={adminDashboard?.resolvedIncidents ?? '—'} note="Resolved or closed" />
            </div>
            <p className="form-hint">30-day counts: {analytics?.recentIncidentCount ?? '—'} current period · {analytics?.previousPeriodIncidentCount ?? '—'} previous period</p>
            {insights && <p className="recommendation">{insights.label}: {insights.insights.join(' ')}</p>}
            {analytics?.categories && <ul className="compact-list">{Object.entries(analytics.categories).map(([category, count]) => <li key={category}>{formatLabel(category)} <strong>{count}</strong></li>)}</ul>}
          </section>
          <section className="panel user-management">
            <div className="panel-header"><h3>User access management</h3></div>
            <div className="user-list">{users.map((item) => <div className="user-row" key={item.id}>
              <span><strong>{item.firstName} {item.lastName}</strong><small>{item.email}</small></span>
              <select aria-label={`Role for ${item.email}`} value={item.role} disabled={item.id === user.id} onChange={(event) => updateUserRole(item, event.target.value)}>
                {['STUDENT', 'SECURITY_OFFICER', 'ADMIN'].map((role) => <option value={role} key={role}>{formatLabel(role)}</option>)}
              </select>
            </div>)}</div>
          </section>
        </>}
      </main>}

      {activeTab === 'Incidents' && <main className="content-grid">
        {!isResponder && <section className="panel">
          <div className="panel-header"><h3>Report a safety incident</h3></div>
          <form className="form-stack" onSubmit={submitIncident}>
            <label>Category<select name="category" required defaultValue=""><option value="" disabled>Select a category</option>{categories.map((category) => <option key={category}>{category}</option>)}</select></label>
            <label>What happened?<textarea name="description" rows="4" minLength="8" maxLength="5000" required placeholder="Describe what you observed. Avoid including unnecessary sensitive personal details." /></label>
            <div className="inline-fields">
              <label>Location<input name="locationName" maxLength="200" required placeholder="Building and room or area" /></label>
              <label>Severity<select name="severity" defaultValue="MEDIUM">{severities.map((severity) => <option key={severity}>{severity}</option>)}</select></label>
            </div>
            <div className="form-actions"><button className="ghost-button" type="button" onClick={analyzeIncident}>Get AI recommendation</button><button className="primary-button">Submit incident</button></div>
            {recommendation && <div className="recommendation" role="status"><strong>AI suggestion — human review required</strong><p>{formatLabel(recommendation.predictedCategory)} · {recommendation.severity} · score {recommendation.priorityScore}</p><p>{recommendation.summary}</p><ul>{recommendation.reasons.map((reason) => <li key={reason}>{reason}</li>)}</ul></div>}
          </form>
        </section>}
        <section className="panel incident-worklist">
          <div className="panel-header"><h3>{isResponder ? 'Incident response queue' : 'Incident tracking'}</h3><button className="link-button" type="button" onClick={() => loadWorkspace(user)}>Refresh</button></div>
          <div className="filter-row" aria-label="Filter incidents">
            <label>Status<select value={incidentFilters.status || ''} onChange={(event) => filterIncidentList('status', event.target.value)}><option value="">All statuses</option>{['NEW', ...statuses].map((status) => <option key={status}>{status}</option>)}</select></label>
            <label>Severity<select value={incidentFilters.severity || ''} onChange={(event) => filterIncidentList('severity', event.target.value)}><option value="">All severities</option>{severities.map((severity) => <option key={severity}>{severity}</option>)}</select></label>
            <label>Category<select value={incidentFilters.category || ''} onChange={(event) => filterIncidentList('category', event.target.value)}><option value="">All categories</option>{categories.map((category) => <option key={category}>{category}</option>)}</select></label>
          </div>
          <IncidentList incidents={incidents} responder={isResponder} officers={officers} assignmentSelection={assignmentSelection} onSelectOfficer={(id, officerId) => setAssignmentSelection((current) => ({ ...current, [id]: officerId }))} onStatus={updateIncidentStatus} onAssign={assignIncident} onHistory={viewHistory} />
        </section>
      </main>}

      {activeTab === 'Alerts' && <main className="content-grid">
        {user.role === 'ADMIN' && <section className="panel">
          <div className="panel-header"><h3>Publish a campus alert</h3></div>
          <form className="form-stack" onSubmit={submitAlert}>
            <label>Title<input name="title" maxLength="160" required /></label>
            <label>Message<textarea name="message" rows="3" maxLength="4000" required /></label>
            <div className="inline-fields">
              <label>Type<select name="alertType" defaultValue="SAFETY_ANNOUNCEMENT"><option>SAFETY_ANNOUNCEMENT</option><option>EMERGENCY</option><option>WEATHER</option><option>SECURITY</option></select></label>
              <label>Severity<select name="severity" defaultValue="MEDIUM">{severities.map((severity) => <option key={severity}>{severity}</option>)}</select></label>
            </div>
            <label>Affected area<input name="affectedArea" maxLength="200" required /></label>
            <label>Expires at<input name="expiresAt" type="datetime-local" required /></label>
            <button className="primary-button">Publish alert</button>
          </form>
        </section>}
        <section className="panel">
          <div className="panel-header"><h3>Campus alerts</h3><button className="link-button" type="button" onClick={() => loadWorkspace(user)}>Refresh</button></div>
          <AlertList alerts={alerts} />
        </section>
      </main>}

      {activeTab === 'Notifications' && <main className="panel notifications-panel">
        <div className="panel-header"><h3>Notifications</h3><button className="link-button" type="button" onClick={() => loadWorkspace(user)}>Refresh</button></div>
        {notifications.length === 0 ? <p className="empty-state">You have no notifications.</p> : <ul className="notification-list">{notifications.map((item) => <li key={item.id} className={item.readStatus ? 'read' : ''}>
          <div><p>{item.message}</p><small>{formatLabel(item.type)} · {new Date(item.createdAt).toLocaleString()}</small></div>
          {!item.readStatus && <button className="ghost-button" type="button" onClick={() => markRead(item)}>Mark read</button>}
        </li>)}</ul>}
      </main>}

      {activeTab === 'Profile' && <main className="profile-grid">
        <section className="panel"><div className="panel-header"><h3>Your profile</h3></div>
          <dl className="profile-details"><dt>Name</dt><dd>{user.firstName} {user.lastName}</dd><dt>Email</dt><dd>{user.email}</dd><dt>Role</dt><dd>{formatLabel(user.role)}</dd><dt>Department</dt><dd>{user.department || 'Not provided'}</dd><dt>Account created</dt><dd>{user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'}</dd></dl>
          <form className="form-stack profile-form" onSubmit={updateProfile}>
            <label>Phone<input name="phone" type="tel" maxLength="30" defaultValue={user.phone || ''} /></label>
            <label>Department<input name="department" maxLength="120" defaultValue={user.department || ''} /></label>
            <button className="primary-button">Save profile</button>
          </form>
          <p className="form-hint">Campus access roles are managed by administrators.</p>
        </section>
        <section className="panel"><div className="panel-header"><h3>Emergency contacts</h3></div>
          {contacts.length ? <ul className="contact-list">{contacts.map((contact) => <li key={contact.id}><strong>{contact.name}</strong><span>{contact.role}</span><a href={`tel:${contact.phone}`}>{contact.phone}</a>{contact.email && <a href={`mailto:${contact.email}`}>{contact.email}</a>}</li>)}</ul> : <p className="empty-state">No emergency contacts have been configured.</p>}
          {user.role === 'ADMIN' && <form className="form-stack contact-form" onSubmit={createContact}>
            <h4>Add an emergency contact</h4>
            <label>Name<input name="name" maxLength="120" required /></label>
            <label>Role<input name="role" maxLength="120" required /></label>
            <label>Phone<input name="phone" maxLength="30" required /></label>
            <label>Email<input name="email" type="email" maxLength="254" /></label>
            <label>Priority<input name="priority" type="number" min="0" defaultValue="0" required /></label>
            <button className="primary-button">Save contact</button>
          </form>}
        </section>
        {user.role === 'ADMIN' && <section className="panel audit-panel"><div className="panel-header"><h3>Recent audit trail</h3></div>
          {auditLogs.length ? <ul className="timeline">{auditLogs.slice(0, 10).map((entry) => <li key={entry.id}><span>{new Date(entry.createdAt).toLocaleString()}</span><p><strong>{formatLabel(entry.action)}</strong> · {entry.actor?.email || 'System'} · {entry.entityType} #{entry.entityId}</p></li>)}</ul> : <p className="empty-state">No audit events recorded yet.</p>}
        </section>}
      </main>}

      {selectedIncident && <div className="modal-backdrop" role="presentation" onClick={() => setSelectedIncident(null)}>
        <section className="panel history-modal" role="dialog" aria-modal="true" aria-label={`Incident ${selectedIncident.id} history`} onClick={(event) => event.stopPropagation()}>
          <div className="panel-header"><h3>Incident #{selectedIncident.id} history</h3><button className="ghost-button" type="button" onClick={() => setSelectedIncident(null)}>Close</button></div>
          {incidentHistory.length ? <ul className="timeline">{incidentHistory.map((entry) => <li key={entry.id}><span>{new Date(entry.createdAt).toLocaleString()}</span><p><strong>{formatLabel(entry.action)}</strong> · {entry.details}</p></li>)}</ul> : <p className="empty-state">No history entries are available.</p>}
          {isResponder && <form className="form-stack note-form" onSubmit={submitNote}><label>Add a response note<textarea rows="3" value={noteDraft} onChange={(event) => setNoteDraft(event.target.value)} maxLength="3000" required /></label><button className="primary-button">Add note</button></form>}
        </section>
      </div>}
    </div>
  )
}

function Stat({ label, value, note, tone = 'neutral' }) {
  return <article className={`stat-card ${tone}`}><span>{label}</span><strong>{value}</strong><small>{note}</small></article>
}

function IncidentList({ incidents, responder, officers, assignmentSelection, onSelectOfficer, onStatus, onAssign, onHistory }) {
  if (!incidents.length) return <p className="empty-state">No incidents to show.</p>
  return <div className="incident-list">{incidents.map((incident) => <article key={incident.id} className="incident-item">
    <div className="incident-main"><span className={`severity severity-${incident.severity?.toLowerCase()}`}>{formatLabel(incident.severity)}</span><div>
      <h4>#{incident.id} · {formatLabel(incident.category)}</h4><p>{incident.description}</p><small>{incident.locationName} · {incident.reporter?.firstName || 'Reporter'} · {new Date(incident.createdAt).toLocaleString()}</small>
      {incident.assignedOfficer && <small className="assigned-note">Assigned to {incident.assignedOfficer.firstName} {incident.assignedOfficer.lastName}</small>}
    </div></div>
    <div className="incident-meta"><span>{formatLabel(incident.status)}</span><button className="link-button" type="button" onClick={() => onHistory(incident)}>History</button>
      {responder && <><select aria-label={`Assign incident ${incident.id}`} value={assignmentSelection[incident.id] || ''} onChange={(event) => onSelectOfficer(incident.id, event.target.value)}><option value="">Assign officer…</option>{officers.map((officer) => <option key={officer.id} value={officer.id}>{officer.firstName} {officer.lastName}</option>)}</select><button className="link-button" type="button" disabled={!assignmentSelection[incident.id]} onClick={() => onAssign(incident, assignmentSelection[incident.id])}>Assign</button><select aria-label={`Update status for incident ${incident.id}`} value="" onChange={(event) => event.target.value && onStatus(incident, event.target.value)}><option value="">Update status</option>{statuses.map((status) => <option key={status}>{status}</option>)}</select></>}
    </div>
  </article>)}</div>
}

function AlertList({ alerts }) {
  if (!alerts.length) return <p className="empty-state">There are no active alerts.</p>
  return <ul className="alert-list">{alerts.map((alert) => <li key={alert.id}><span className={`severity severity-${alert.severity?.toLowerCase()}`}>{formatLabel(alert.severity)}</span><strong>{alert.title}</strong><p>{alert.message}</p><small>{alert.affectedArea} · Expires {new Date(alert.expiresAt).toLocaleString()}</small></li>)}</ul>
}

export default App
