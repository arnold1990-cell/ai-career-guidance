import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { LoginForm, RegisterForm } from '@/components/forms/AuthForms';
import { useAuth } from '@/hooks/useAuth';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { useForm } from 'react-hook-form';
import { authService } from '@/services/authService';
import { studentService } from '@/services/studentService';
import type { CompanyRegisterPayload, Role, User } from '@/types';

const AuthCard = ({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) => (
  <div className="mx-auto max-w-md card p-6">
    <h1 className="text-2xl font-bold">{title}</h1>
    <p className="mt-1 text-sm text-slate-600">{subtitle}</p>
    <div className="mt-5">{children}</div>
  </div>
);

const getRoleDashboard = (user: User): string => {
  const primaryRole = user.roles[0]?.replace('ROLE_', '') as Role | undefined;
  if (primaryRole === 'COMPANY') return '/company/dashboard';
  if (primaryRole === 'ADMIN') return '/admin/dashboard';
  return '/student/dashboard';
};

export const LoginPage = () => {
  const { login, isAuthenticated, user, isHydrated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: { pathname?: string } } | undefined)?.from?.pathname;

  if (isHydrated && isAuthenticated && user) {
    return <Navigate to={getRoleDashboard(user)} replace />;
  }

  return (
    <AuthCard title="Welcome back" subtitle="Sign in to continue your EduRite journey.">
      <LoginForm
        onSubmit={async (data) => {
          const loggedInUser = await login(data);
          if (loggedInUser.roles.includes('ROLE_STUDENT')) {
            const me = await studentService.getMe();
            navigate(me.profileCompleted ? '/student/dashboard' : '/student/profile', { replace: true });
            return;
          }
          navigate(from && from !== '/auth/login' ? from : getRoleDashboard(loggedInUser), { replace: true });
        }}
      />
      <p className="mt-4 text-sm">Forgot password? <Link className="text-primary-600" to="/auth/forgot-password">Reset it</Link></p>
      <p className="mt-2 text-sm">New here? <Link className="text-primary-600" to="/auth/register/student">Create student account</Link></p>
      <p className="mt-2 text-sm">Hiring or sponsoring talent? <Link className="text-primary-600" to="/register/company">Create company account</Link></p>
      <p className="mt-2 text-sm">Platform administrator? <Link className="text-primary-600" to="/auth/login">Admin access</Link></p>
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
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<CompanyRegisterPayload>({
    defaultValues: {
      companyName: '',
      registrationNumber: '',
      officialEmail: '',
      password: '',
      mobileNumber: '',
      contactPersonName: '',
    },
  });

  return (
    <AuthCard title="Register your company" subtitle="Launch bursaries and connect with high-potential student talent.">
      <form
        className="space-y-4"
        onSubmit={handleSubmit(async (data) => {
          await authService.registerCompany({
            companyName: data.companyName,
            registrationNumber: data.registrationNumber,
            officialEmail: data.officialEmail,
            password: data.password,
            mobileNumber: data.mobileNumber,
            contactPersonName: data.companyName,
          });
          navigate('/auth/login', { replace: true });
        })}
      >
        <label className="block text-sm">Company Name<Input {...register('companyName', { required: true })} /></label>
        <label className="block text-sm">Registration Number<Input {...register('registrationNumber', { required: true })} /></label>
        <label className="block text-sm">Official Email<Input type="email" {...register('officialEmail', { required: true })} /></label>
        <label className="block text-sm">Mobile Number<Input {...register('mobileNumber')} /></label>
        <label className="block text-sm">Password<Input type="password" {...register('password', { required: true, minLength: 8 })} /></label>
        <Button disabled={isSubmitting} type="submit" className="w-full">Create company account</Button>
      </form>
    </AuthCard>
  );
};

export const ForgotPasswordPage = () => {
  const { register, handleSubmit } = useForm<{ email: string }>();
  return (
    <AuthCard title="Forgot Password" subtitle="Enter your email and we will send a secure reset link.">
      <form className="space-y-3" onSubmit={handleSubmit(async ({ email }) => { await authService.forgotPassword({ email }); })}>
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
      <form className="space-y-3" onSubmit={handleSubmit(async ({ password }) => { if (token) await authService.resetPassword({ token, newPassword: password, confirmPassword: password }); })}>
        <label className="text-sm">New Password<Input type="password" {...register('password')} /></label>
        <Button type="submit" className="w-full">Reset password</Button>
      </form>
    </AuthCard>
  );
};
