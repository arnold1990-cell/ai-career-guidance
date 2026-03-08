import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { LoginForm, RegisterForm } from '@/components/forms/AuthForms';
import { useAuth } from '@/hooks/useAuth';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { useForm } from 'react-hook-form';

const AuthCard = ({ title, children }: { title: string; children: React.ReactNode }) => (
  <div className="mx-auto max-w-md card p-6">
    <h1 className="mb-4 text-2xl font-bold">{title}</h1>
    {children}
  </div>
);

export const LoginPage = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  return <AuthCard title="Login"><LoginForm onSubmit={async (data) => { await login(data); navigate('/'); }} /><p className="mt-4 text-sm">Forgot password? <Link className="text-primary-600" to="/auth/forgot-password">Reset it</Link></p></AuthCard>;
};

export const RegisterStudentPage = () => {
  const { registerStudent } = useAuth();
  const navigate = useNavigate();
  return <AuthCard title="Register as Student"><RegisterForm type="student" onSubmit={async (data) => { await registerStudent(data); navigate('/student/dashboard'); }} /></AuthCard>;
};

export const RegisterCompanyPage = () => {
  const { registerCompany } = useAuth();
  const navigate = useNavigate();
  return <AuthCard title="Register as Company"><RegisterForm type="company" onSubmit={async (data) => { await registerCompany(data); navigate('/company/dashboard'); }} /></AuthCard>;
};

export const ForgotPasswordPage = () => {
  const { register, handleSubmit } = useForm<{ email: string }>();
  return <AuthCard title="Forgot Password"><form className="space-y-3" onSubmit={handleSubmit(async () => undefined)}><label className="text-sm">Email<Input type="email" {...register('email')} /></label><Button type="submit">Send reset link</Button></form></AuthCard>;
};

export const ResetPasswordPage = () => {
  const { register, handleSubmit } = useForm<{ password: string }>();
  const [params] = useSearchParams();
  return <AuthCard title="Reset Password"><p className="mb-3 text-xs text-slate-500">Token: {params.get('token') ?? 'missing'}</p><form className="space-y-3" onSubmit={handleSubmit(async () => undefined)}><label className="text-sm">New Password<Input type="password" {...register('password')} /></label><Button type="submit">Reset password</Button></form></AuthCard>;
};
