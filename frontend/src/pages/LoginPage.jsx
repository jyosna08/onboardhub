import { Link } from 'react-router-dom'
import AuthLayout from '@/components/auth/AuthLayout.jsx'
import LoginForm from '@/components/auth/LoginForm.jsx'

export default function LoginPage() {
  return <AuthLayout title="Welcome back" subtitle="Log in to continue to OnboardHub." footer={<>New to OnboardHub? <Link className="font-medium text-brand-600 hover:text-brand-700" to="/register">Create an account</Link></>}><LoginForm /></AuthLayout>
}
