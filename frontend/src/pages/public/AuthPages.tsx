import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { LoginForm, RegisterForm } from '@/components/forms/AuthForms';
import { useAuth } from '@/hooks/useAuth';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { useForm } from 'react-hook-form';
import { authService } from '@/services/authService';

const AuthCard = ({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) => (
  <div className="mx-auto max-w-md card p-6">
    <h1 className="text-2xl font-bold">{title}</h1>
    <p className="mt-1 text-sm text-slate-600">{subtitle}</p>
    <div className="mt-5">{children}</div>
  </div>
);

export const LoginPage = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  return (
    <AuthCard title="Welcome back" subtitle="Sign in to continue your EduRite journey.">
      <LoginForm onSubmit={async (data) => { await login(data); navigate('/'); }} />
      <p className="mt-4 text-sm">Forgot password? <Link className="text-primary-600" to="/auth/forgot-password">Reset it</Link></p>
      <p className="mt-2 text-sm">New here? <Link className="text-primary-600" to="/auth/register/student">Create student account</Link></p>
    </AuthCard>
  );
};

export const RegisterStudentPage = () => {
  const { registerStudent } = useAuth();
  const navigate = useNavigate();
  return (
    <AuthCard title="Create student account" subtitle="Set up your profile to access careers, courses, and bursaries.">
      <RegisterForm
        type="student"
        onSubmit={async ({ fullName, email, password }) => {
          await registerStudent({ fullName, email, password });
          navigate('/student/dashboard');
        }}
      />
    </AuthCard>
  );
};

export const RegisterCompanyPage = () => {
  const { registerCompany } = useAuth();
  const navigate = useNavigate();
  return (
    <AuthCard title="Register your company" subtitle="Launch bursaries and connect with high-potential student talent.">
      <RegisterForm
        type="company"
        onSubmit={async ({ companyName, email, password }) => {
          await registerCompany({ companyName: companyName ?? '', email, password });
          navigate('/company/dashboard');
        }}
      />
    </AuthCard>
  );
};

export const ForgotPasswordPage = () => {
  const { register, handleSubmit } = useForm<{ email: string }>();
  return (
    <AuthCard title="Forgot Password" subtitle="Enter your email and we will send a secure reset link.">
      <form className="space-y-3" onSubmit={handleSubmit(async ({ email }) => { await authService.forgotPassword(email); })}>
        <label className="text-sm">Email<Input type="email" {...register('email')} /></label>
        <Button type="submit" className="w-full">Send reset link</Button>
      </form>
    </AuthCard>
  );
};

export const ResetPasswordPage = () => {
  const { register, handleSubmit } = useForm<{ password: string }>();
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  return (
    <AuthCard title="Reset Password" subtitle="Choose a strong password to secure your account.">
      <p className="mb-3 text-xs text-slate-500">Reset token: {token || 'missing token in URL'}</p>
      <form className="space-y-3" onSubmit={handleSubmit(async ({ password }) => { if (token) await authService.resetPassword({ token, newPassword: password }); })}>
        <label className="text-sm">New Password<Input type="password" {...register('password')} /></label>
        <Button type="submit" className="w-full">Reset password</Button>
      </form>
    </AuthCard>
  );
};
