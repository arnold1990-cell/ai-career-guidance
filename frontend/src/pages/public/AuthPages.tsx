import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { LoginForm, RegisterForm } from '@/components/forms/AuthForms';
import { useAuth } from '@/hooks/useAuth';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { useForm } from 'react-hook-form';
import { authService } from '@/services/authService';
import { studentService } from '@/services/studentService';
import type { CompanyRegisterPayload, Role, User } from '@/types';

const AuthCard = ({
  title,
  subtitle,
  children,
  className = 'mx-auto max-w-md'
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
  className?: string;
}) => (
  <div className={`${className} rounded-2xl border border-slate-200 bg-white p-8 shadow-sm`}>
    <h1 className="text-5xl font-bold leading-tight text-slate-900 md:text-4xl">{title}</h1>
    <p className="mt-2 text-xl text-slate-600 md:text-lg">{subtitle}</p>
    <div className="mt-6">{children}</div>
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
    <section className="min-h-[70vh] rounded-b-2xl bg-slate-100">
      <AuthCard className="max-w-[820px] lg:ml-[320px]" title="Welcome back" subtitle="Sign In to continue your EduRite journey.">
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
      <p className="mt-5 text-lg text-slate-800">Forgot password? <Link className="text-primary-600" to="/auth/forgot-password">Reset it</Link></p>
      <p className="mt-3 text-lg text-slate-800">New to EduRite? <Link className="text-primary-600" to="/auth/register/student">Sign Up</Link></p>
      </AuthCard>
    </section>
  );
};

export const RegisterStudentPage = () => {
  const { registerStudent } = useAuth();
  const navigate = useNavigate();
  return (
    <AuthCard title="Create Student Account" subtitle="Set up your profile to access careers, courses, and bursaries.">
      <RegisterForm
        type="student"
        onSubmit={async ({ firstName, lastName, email, password, interests, location, phone, dateOfBirth, gender, qualificationLevel }) => {
          await registerStudent({ fullName: `${firstName} ${lastName}`.trim(), firstName, lastName, email, password, interests, location, phone, dateOfBirth, gender, qualificationLevel });
          navigate('/student/dashboard');
        }}
      />
      <p className="mt-5 text-sm text-slate-700">Already have an account? <Link className="text-primary-600" to="/auth/login">Sign In</Link></p>
    </AuthCard>
  );
};

export const RegisterCompanyPage = () => {
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { isSubmitting } } = useForm<CompanyRegisterPayload>();

  return (
    <AuthCard title="Register your company" subtitle="Create a company account and submit for verification.">
      <form className="space-y-4" onSubmit={handleSubmit(async (data) => {
        await authService.registerCompany(data);
        navigate('/company/login', { replace: true });
      })}>
        <label className="block text-sm">Company Name<Input {...register('companyName', { required: true })} /></label>
        <label className="block text-sm">Registration Number<Input {...register('registrationNumber', { required: true })} /></label>
        <label className="block text-sm">Industry<Input {...register('industry')} /></label>
        <label className="block text-sm">Official Email<Input type="email" {...register('officialEmail', { required: true })} /></label>
        <label className="block text-sm">Mobile Number<Input {...register('mobileNumber')} /></label>
        <label className="block text-sm">Contact Person Name<Input {...register('contactPersonName')} /></label>
        <label className="block text-sm">Address<Input {...register('address')} /></label>
        <label className="block text-sm">Website<Input {...register('website')} /></label>
        <label className="block text-sm">Description<Input {...register('description')} /></label>
        <label className="block text-sm">Password<Input type="password" {...register('password', { required: true, minLength: 8 })} /></label>
        <Button disabled={isSubmitting} type="submit" className="w-full">Create company account</Button>
      </form>
    </AuthCard>
  );
};

export const ForgotPasswordPage = () => {
  const { register, handleSubmit } = useForm<{ email: string; mobileNumber: string }>();
  const [message, setMessage] = useState('');
  return (
    <AuthCard title="Forgot Password" subtitle="Enter your registered email or mobile to generate a reset token.">
      <form className="space-y-3" onSubmit={handleSubmit(async ({ email, mobileNumber }) => {
        await authService.forgotPassword({ email: email || undefined, mobileNumber: mobileNumber || undefined });
        setMessage('If the account exists, reset instructions have been generated.');
      })}>
        <label className="text-sm">Email<Input type="email" {...register('email')} /></label>
        <label className="text-sm">Mobile Number<Input {...register('mobileNumber')} /></label>
        <Button type="submit" className="w-full">Generate reset token</Button>
        {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      </form>
    </AuthCard>
  );
};

export const ResetPasswordPage = () => {
  const { register, handleSubmit } = useForm<{ newPassword: string; confirmPassword: string }>();
  const [params] = useSearchParams();
  const [message, setMessage] = useState('');
  const token = params.get('token') ?? '';
  return (
    <AuthCard title="Reset Password" subtitle="Use the token and set a new password.">
      <p className="mb-3 text-xs text-slate-500">Reset token: {token || 'missing token in URL'}</p>
      <form className="space-y-3" onSubmit={handleSubmit(async ({ newPassword, confirmPassword }) => {
        if (!token) return;
        await authService.resetPassword({ token, newPassword, confirmPassword });
        setMessage('Password reset complete. You can now Sign In.');
      })}>
        <label className="text-sm">New Password<Input type="password" {...register('newPassword', { required: true })} /></label>
        <label className="text-sm">Confirm Password<Input type="password" {...register('confirmPassword', { required: true })} /></label>
        <Button type="submit" className="w-full">Reset password</Button>
        {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      </form>
    </AuthCard>
  );
};
