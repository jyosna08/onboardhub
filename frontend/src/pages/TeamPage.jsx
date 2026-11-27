import { useState } from 'react'
import CreateUserForm from '@/components/admin/CreateUserForm.jsx'
import UserTable from '@/components/admin/UserTable.jsx'
import EmptyState from '@/components/common/EmptyState.jsx'
import ErrorBanner from '@/components/common/ErrorBanner.jsx'
import NoticeBanner from '@/components/common/NoticeBanner.jsx'
import Skeleton from '@/components/common/Skeleton.jsx'
import { useUsers } from '@/hooks/useUsers.js'
import { usePolling } from '@/hooks/usePolling.js'
import { REFRESH_INTERVAL_MS } from '@/utils/constants.js'
import { ROLE_LABELS } from '@/utils/display.js'
import { getErrorMessage } from '@/utils/errors.js'

export default function TeamPage() {
  const { users, isLoading, error, refresh } = useUsers()
  const [notice, setNotice] = useState('')
  usePolling(refresh, REFRESH_INTERVAL_MS)
  function handleCreated(user) { setNotice(`${ROLE_LABELS[user.role] || user.role} ${user.fullName} created. They can sign in with the email and password you set.`); void refresh() }
  return <div className="space-y-6"><header><h1 className="text-2xl font-bold text-gray-900">Team</h1><p className="mt-1 text-sm text-gray-500">Manage administrator and service team accounts.</p></header><NoticeBanner message={notice} onDismiss={() => setNotice('')}/><CreateUserForm onCreated={handleCreated}/>{error && <ErrorBanner message={getErrorMessage(error)} onRetry={refresh}/ >}{isLoading ? <div className="space-y-3" role="status" aria-label="Loading team accounts"><Skeleton className="h-16"/><Skeleton className="h-16"/></div> : users.length ? <UserTable users={users}/> : <EmptyState title="No team accounts" message="Create the first team account above."/>}</div>
}
