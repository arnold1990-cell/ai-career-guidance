import { useMemo, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { getDashboardPathForRole, getDashboardPathForUser, isAuthorizedPathForRole, resolvePrimaryRole } from '@/features/auth/roleUtils';
import { useAuth } from '@/hooks/useAuth';
import { authService } from '@/services/authService';
import { studentService } from '@/services/studentService';
import type { CompanyRegisterPayload, Role, User } from '@/types';

type AuthRole = Role;
type AuthMode = 'login' | 'register';

type LoginFormValues = {
  email: string;
  password: string;
  rememberMe: boolean;
};

const roleContent: Record<AuthRole, {
  badge: string;
  loginTitle: string;
  loginSubtitle: string;
  registerTitle: string;
  registerSubtitle: string;
  registerPath?: string;
  ctaLabel: string;
}> = {
  STUDENT: {
    badge: 'Student access',
    loginTitle: 'Welcome back',
    loginSubtitle: 'Sign in with your existing EduRite student account to continue your journey.',
    registerTitle: 'Create student account',
    registerSubtitle: 'Set up your student profile to access careers, courses, bursaries, and guidance.',
    registerPath: '/auth/register/student',
    ctaLabel: 'Sign in as Student',
  },
  COMPANY: {
    badge: 'Company portal',
    loginTitle: 'Welcome back',
    loginSubtitle: 'Access your company workspace to manage bursaries, talent pipelines, and approvals.',
    registerTitle: 'Register your company',
    registerSubtitle: 'Create a company account, submit verification details, and start engaging with talent.',
    registerPath: '/auth/register/company',
    ctaLabel: 'Sign in as Company',
  },
  ADMIN: {
    badge: 'Admin console',
    loginTitle: 'Welcome back',
    loginSubtitle: 'Use your secure admin credentials to access EduRite administration tools.',
    registerTitle: 'Admin access is restricted',
    registerSubtitle: 'Public admin registration is intentionally disabled. Admin accounts must be provisioned through secure internal setup.',
    ctaLabel: 'Sign in as Admin',
  },
};

const getRoleDashboard = (user: User): string => getDashboardPathForUser(user) ?? '/auth/login';

const resolveRoleFromPath = (pathname: string): AuthRole => {
  if (pathname.includes('/company/')) return 'COMPANY';
  if (pathname.includes('/admin/')) return 'ADMIN';
  return 'STUDENT';
};

const resolveModeFromPath = (pathname: string): AuthMode => (pathname.includes('/register') ? 'register' : 'login');

const buildAuthPath = (role: AuthRole, mode: AuthMode) => {
  if (mode === 'login') {
    if (role === 'COMPANY') return '/company/login';
    if (role === 'ADMIN') return '/admin/login';
    return '/auth/login';
  }

  if (role === 'COMPANY') return '/auth/register/company';
  return '/auth/register/student';
};

const getForgotPasswordPath = (role: AuthRole) => role === 'COMPANY' ? '/company/forgot-password' : role === 'ADMIN' ? '/admin/forgot-password' : '/auth/forgot-password';
const getResetPasswordLoginPath = (role: AuthRole) => role === 'COMPANY' ? '/company/login' : role === 'ADMIN' ? '/admin/login' : '/auth/login';

const AuthShell = ({ children, role, mode }: { children: React.ReactNode; role: AuthRole; mode: AuthMode }) => {
  const config = roleContent[role];

  return (
    <section className="min-h-[70vh] rounded-b-2xl bg-slate-100 px-0 py-6 md:px-4">
      <div className="mx-auto grid max-w-6xl gap-6 lg:grid-cols-[1.05fr_0.95fr] lg:items-center">
        <div className="hidden rounded-[32px] border border-white/60 bg-gradient-to-br from-primary-700 via-primary-600 to-sky-500 p-8 text-white shadow-2xl lg:block">
          <span className="inline-flex rounded-full border border-white/30 bg-white/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.28em] text-white/90">
            EduRite {config.badge}
          </span>
          <h1 className="mt-6 max-w-md text-4xl font-semibold leading-tight">
            {mode === 'login' ? 'Multi-role access, one familiar EduRite flow.' : 'Join EduRite with the right role for your work.'}
          </h1>
          <p className="mt-4 max-w-xl text-base leading-7 text-blue-50/95">
            Students keep the exact authentication flow they already use, while companies and admins now get a clearer role-aware experience layered on the same secure foundation.
          </p>
          <div className="mt-8 grid gap-4 sm:grid-cols-2">
            {[
              'Student login and signup remain backward-compatible.',
              'Company onboarding stays approval-aware and secure.',
              'Admin access supports sign-in only—no public registration.',
              'Role-based dashboard redirects happen immediately after login.',
            ].map((item) => (
              <div key={item} className="rounded-2xl border border-white/20 bg-white/10 p-4 text-sm leading-6 text-white/95">
                {item}
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-[32px] border border-slate-200/80 bg-white/95 p-5 shadow-[0_30px_90px_-40px_rgba(15,23,42,0.6)] backdrop-blur sm:p-8 lg:p-10">
          {children}
        </div>
      </div>
    </section>
  );
};

const RoleTabs = ({ role, mode }: { role: AuthRole; mode: AuthMode }) => {
  const tabs: AuthRole[] = ['STUDENT', 'COMPANY', 'ADMIN'];

  return (
    <div className="rounded-2xl bg-slate-100 p-1.5">
      <div className="grid grid-cols-3 gap-1.5">
        {tabs.map((tabRole) => {
          const isRegisterDisabled = mode === 'register' && tabRole === 'ADMIN';
          const isActive = tabRole === role;
          const baseClass = isActive
            ? 'bg-white text-primary-700 shadow-sm'
            : 'text-slate-500 hover:text-slate-700';

          if (isRegisterDisabled) {
            return (
              <div
                key={tabRole}
                className={`rounded-xl px-3 py-3 text-center text-sm font-semibold ${baseClass} cursor-not-allowed opacity-60`}
                title="Admin registration is restricted"
              >
                {tabRole.charAt(0) + tabRole.slice(1).toLowerCase()}
              </div>
            );
          }

          return (
            <Link key={tabRole} to={buildAuthPath(tabRole, mode)} className={`rounded-xl px-3 py-3 text-center text-sm font-semibold transition ${baseClass}`}>
              {tabRole.charAt(0) + tabRole.slice(1).toLowerCase()}
            </Link>
          );
        })}
      </div>
    </div>
  );
};

const AuthHeader = ({ role, mode }: { role: AuthRole; mode: AuthMode }) => {
  const config = roleContent[role];
  return (
    <div>
      <span className="inline-flex rounded-full bg-primary-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.24em] text-primary-700">
        {config.badge}
      </span>
      <h2 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900">{mode === 'login' ? config.loginTitle : config.registerTitle}</h2>
      <p className="mt-3 text-sm leading-6 text-slate-600">{mode === 'login' ? config.loginSubtitle : config.registerSubtitle}</p>
    </div>
  );
};

const SignInForm = ({ role }: { role: AuthRole }) => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: { pathname?: string } } | undefined)?.from?.pathname;
  const [serverError, setServerError] = useState<string | null>(null);
  const [form, setForm] = useState<LoginFormValues>({ email: '', password: '', rememberMe: true });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setServerError(null);
    setIsSubmitting(true);
    if (import.meta.env.DEV) {
      console.info('[auth] submit login form', { email: form.email, passwordLength: form.password.length, role });
    }

    try {
      const loggedInUser = await login({ email: form.email, password: form.password }, { rememberMe: form.rememberMe, portal: role });
      const primaryRole = resolvePrimaryRole(loggedInUser);
      if (import.meta.env.DEV) {
        console.info('[auth] normalized login user', loggedInUser);
      }
      if (!primaryRole) {
        throw new Error('Signed in successfully, but no supported role was returned for this account.');
      }

      const roleMismatch = primaryRole !== role
        ? `This account is signed in as ${primaryRole.toLowerCase()}, so EduRite redirected you to the ${primaryRole.toLowerCase()} workspace.`
        : undefined;

      if (primaryRole === 'ADMIN') {
        const finalPath = isAuthorizedPathForRole(from, primaryRole) ? from! : '/admin/dashboard';
        if (import.meta.env.DEV) {
          console.info('[auth] final redirect path', { email: loggedInUser.email, primaryRole, approvalStatus: loggedInUser.approvalStatus, finalPath });
        }
        navigate(finalPath, {
          replace: true,
          state: roleMismatch ? { roleMismatch } : undefined,
        });
        return;
      }

      if (primaryRole === 'STUDENT') {
        const me = await studentService.getMe();
        const finalPath = me.profileCompleted ? '/student/dashboard' : '/student/profile';
        if (import.meta.env.DEV) {
          console.info('[auth] final redirect path', { email: loggedInUser.email, primaryRole, approvalStatus: loggedInUser.approvalStatus, finalPath });
        }
        navigate(finalPath, {
          replace: true,
          state: roleMismatch ? { roleMismatch } : undefined,
        });
        return;
      }

      const dashboardPath = primaryRole === 'COMPANY'
        ? loggedInUser.approvalStatus === 'APPROVED'
          ? '/company/dashboard'
          : '/company/pending-approval'
        : getDashboardPathForRole(primaryRole);
      const finalPath = isAuthorizedPathForRole(from, primaryRole) ? from! : dashboardPath ?? '/auth/login';
      if (import.meta.env.DEV) {
        console.info('[auth] final redirect path', { email: loggedInUser.email, primaryRole, approvalStatus: loggedInUser.approvalStatus, finalPath });
      }
      navigate(finalPath, {
        replace: true,
        state: roleMismatch ? { roleMismatch } : undefined,
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to sign you in.';
      if (import.meta.env.DEV) {
        console.error('[auth] displayed login error', { error, displayedMessage: message });
      }
      setServerError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const mismatchMessage = (location.state as { roleMismatch?: string } | undefined)?.roleMismatch;

  return (
    <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
      {mismatchMessage ? <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">{mismatchMessage}</div> : null}
      <label className="block text-sm font-medium text-slate-700">
        Email address
        <Input
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
          className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5 text-sm"
          placeholder="name@example.com"
          required
        />
      </label>

      <label className="block text-sm font-medium text-slate-700">
        Password
        <Input
          type="password"
          autoComplete="current-password"
          value={form.password}
          onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
          className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5 text-sm"
          placeholder="Enter your password"
          required
        />
      </label>

      <div className="flex flex-col gap-3 text-sm text-slate-600 sm:flex-row sm:items-center sm:justify-between">
        <label className="inline-flex items-center gap-3 font-medium text-slate-700">
          <input
            type="checkbox"
            checked={form.rememberMe}
            onChange={(event) => setForm((current) => ({ ...current, rememberMe: event.target.checked }))}
            className="h-4 w-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500"
          />
          Remember me
        </label>
        <Link className="font-semibold text-primary-600 hover:text-primary-500" to={getForgotPasswordPath(role)}>
          Forgot password?
        </Link>
      </div>

      {serverError ? <p className="text-sm text-red-600">{serverError}</p> : null}

      <Button disabled={isSubmitting} type="submit" className="w-full rounded-2xl px-6 py-3.5 text-sm shadow-lg shadow-primary-600/20">
        {isSubmitting ? 'Signing in...' : roleContent[role].ctaLabel}
      </Button>

      <p className="text-center text-sm text-slate-600">
        {role === 'ADMIN' ? (
          <>
            Need admin access? <span className="font-medium text-slate-800">Use your provisioned internal credentials.</span>
          </>
        ) : (
          <>
            Don&apos;t have an account?{' '}
            <Link className="font-semibold text-primary-600 hover:text-primary-500" to={roleContent[role].registerPath ?? '/auth/register/student'}>
              Sign up
            </Link>
          </>
        )}
      </p>
    </form>
  );
};

export const LoginPage = () => {
  const { isAuthenticated, user, isHydrated } = useAuth();
  const location = useLocation();
  const role = useMemo(() => resolveRoleFromPath(location.pathname), [location.pathname]);

  if (isHydrated && isAuthenticated && user) {
    return <Navigate to={getRoleDashboard(user)} replace />;
  }

  return (
    <AuthShell role={role} mode="login">
      <RoleTabs role={role} mode="login" />
      <div className="mt-6">
        <AuthHeader role={role} mode="login" />
        <SignInForm role={role} />
      </div>
    </AuthShell>
  );
};

export const RegisterStudentPage = () => {
  const { registerStudent } = useAuth();
  const navigate = useNavigate();

  return (
    <AuthShell role="STUDENT" mode="register">
      <RoleTabs role="STUDENT" mode="register" />
      <div className="mt-6">
        <AuthHeader role="STUDENT" mode="register" />
        <form
          className="mt-8 grid gap-4 sm:grid-cols-2"
          onSubmit={async (event) => {
            event.preventDefault();
            const formData = new FormData(event.currentTarget);
            const firstName = String(formData.get('firstName') ?? '').trim();
            const lastName = String(formData.get('lastName') ?? '').trim();
            const email = String(formData.get('email') ?? '').trim();
            const password = String(formData.get('password') ?? '');
            const interests = String(formData.get('interests') ?? '').trim();
            const location = String(formData.get('location') ?? '').trim();
            const phone = String(formData.get('phone') ?? '').trim();
            const dateOfBirth = String(formData.get('dateOfBirth') ?? '').trim();
            const gender = String(formData.get('gender') ?? '').trim();
            const qualificationLevel = String(formData.get('qualificationLevel') ?? '').trim();

            await registerStudent({
              fullName: `${firstName} ${lastName}`.trim(),
              firstName,
              lastName,
              email,
              password,
              interests,
              location,
              phone,
              dateOfBirth,
              gender,
              qualificationLevel,
            });
            navigate('/student/dashboard');
          }}
        >
          <label className="block text-sm font-medium text-slate-700">
            First name
            <Input name="firstName" autoComplete="given-name" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" required />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Last name
            <Input name="lastName" autoComplete="family-name" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" required />
          </label>
          <label className="block text-sm font-medium text-slate-700 sm:col-span-2">
            Email address
            <Input name="email" type="email" autoComplete="email" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" required />
          </label>
          <label className="block text-sm font-medium text-slate-700 sm:col-span-2">
            Password
            <Input name="password" type="password" autoComplete="new-password" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" minLength={8} required />
            <span className="mt-1 block text-xs text-slate-500">Use at least 8 characters and include a number.</span>
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Interests
            <Input name="interests" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" placeholder="Engineering, coding" />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Location
            <Input name="location" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Phone
            <Input name="phone" autoComplete="tel" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Date of birth
            <Input name="dateOfBirth" type="date" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Gender
            <Input name="gender" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Qualification level
            <Input name="qualificationLevel" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" placeholder="High School" />
          </label>
          <div className="sm:col-span-2 space-y-4 pt-2">
            <Button type="submit" className="w-full rounded-2xl px-6 py-3.5 text-sm shadow-lg shadow-primary-600/20">Create student account</Button>
            <p className="text-center text-sm text-slate-600">Already have an account? <Link className="font-semibold text-primary-600 hover:text-primary-500" to="/auth/login">Sign in</Link></p>
          </div>
        </form>
      </div>
    </AuthShell>
  );
};

export const RegisterCompanyPage = () => {
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { isSubmitting } } = useForm<CompanyRegisterPayload>({
    defaultValues: {
      companyName: '',
      registrationNumber: '',
      industry: '',
      officialEmail: '',
      mobileNumber: '',
      contactPersonName: '',
      address: '',
      website: '',
      description: '',
      password: '',
    },
  });

  return (
    <AuthShell role="COMPANY" mode="register">
      <RoleTabs role="COMPANY" mode="register" />
      <div className="mt-6">
        <AuthHeader role="COMPANY" mode="register" />
        <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          New company accounts are created with <span className="font-semibold">pending</span> access. Admin approval is required before bursary posting and talent search are unlocked.
        </div>
        <form className="mt-8 grid gap-4 sm:grid-cols-2" onSubmit={handleSubmit(async (data) => {
          await authService.registerCompany(data);
          navigate('/company/login', { replace: true });
        })}>
          <label className="block text-sm font-medium text-slate-700 sm:col-span-2">Company name<Input className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('companyName', { required: true })} /></label>
          <label className="block text-sm font-medium text-slate-700">Registration number<Input className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('registrationNumber', { required: true })} /></label>
          <label className="block text-sm font-medium text-slate-700">Industry<Input className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('industry')} /></label>
          <label className="block text-sm font-medium text-slate-700 sm:col-span-2">Official email<Input type="email" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('officialEmail', { required: true })} /></label>
          <label className="block text-sm font-medium text-slate-700">Mobile number<Input className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('mobileNumber')} /></label>
          <label className="block text-sm font-medium text-slate-700">Contact person<Input className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('contactPersonName', { required: true })} /></label>
          <label className="block text-sm font-medium text-slate-700 sm:col-span-2">Address<Input className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('address')} /></label>
          <label className="block text-sm font-medium text-slate-700">Website<Input className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('website')} /></label>
          <label className="block text-sm font-medium text-slate-700">Password<Input type="password" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('password', { required: true, minLength: 8 })} /></label>
          <label className="block text-sm font-medium text-slate-700 sm:col-span-2">Description<Input className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('description')} /></label>
          <div className="sm:col-span-2 space-y-4 pt-2">
            <Button disabled={isSubmitting} type="submit" className="w-full rounded-2xl px-6 py-3.5 text-sm shadow-lg shadow-primary-600/20">{isSubmitting ? 'Creating account...' : 'Create company account'}</Button>
            <p className="text-center text-sm text-slate-600">Already have a company account? <Link className="font-semibold text-primary-600 hover:text-primary-500" to="/company/login">Sign in</Link></p>
          </div>
        </form>
      </div>
    </AuthShell>
  );
};

export const ForgotPasswordPage = () => {
  const { register, handleSubmit } = useForm<{ email: string; mobileNumber: string }>();
  const [message, setMessage] = useState('');
  const location = useLocation();
  const role = useMemo(() => resolveRoleFromPath(location.pathname), [location.pathname]);
  return (
    <AuthShell role={role} mode={resolveModeFromPath(location.pathname)}>
      <AuthHeader role={role} mode="login" />
      <form className="mt-8 space-y-4" onSubmit={handleSubmit(async ({ email, mobileNumber }) => {
        await authService.forgotPassword({ email: email || undefined, mobileNumber: mobileNumber || undefined });
        setMessage('If the account exists, reset instructions have been generated.');
      })}>
        <label className="text-sm font-medium text-slate-700">Email<Input type="email" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('email')} /></label>
        <label className="text-sm font-medium text-slate-700">Mobile number<Input className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('mobileNumber')} /></label>
        <Button type="submit" className="w-full rounded-2xl px-6 py-3.5 text-sm shadow-lg shadow-primary-600/20">Generate reset token</Button>
        {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      </form>
    </AuthShell>
  );
};

export const ResetPasswordPage = () => {
  const { register, handleSubmit } = useForm<{ newPassword: string; confirmPassword: string }>();
  const [params] = useSearchParams();
  const [message, setMessage] = useState('');
  const location = useLocation();
  const role = useMemo(() => resolveRoleFromPath(location.pathname), [location.pathname]);
  const token = params.get('token') ?? '';
  return (
    <AuthShell role={role} mode="login">
      <AuthHeader role={role} mode="login" />
      <p className="mt-6 text-xs text-slate-500">Reset token: {token || 'missing token in URL'}</p>
      <form className="mt-4 space-y-4" onSubmit={handleSubmit(async ({ newPassword, confirmPassword }) => {
        if (!token) return;
        await authService.resetPassword({ token, newPassword, confirmPassword });
        setMessage('Password reset complete. You can now sign in.');
      })}>
        <label className="text-sm font-medium text-slate-700">New password<Input type="password" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('newPassword', { required: true })} /></label>
        <label className="text-sm font-medium text-slate-700">Confirm password<Input type="password" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('confirmPassword', { required: true })} /></label>
        <Button type="submit" className="w-full rounded-2xl px-6 py-3.5 text-sm shadow-lg shadow-primary-600/20">Reset password</Button>
        {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
        {message ? <Link className="block text-center text-sm font-semibold text-primary-600 hover:text-primary-500" to={getResetPasswordLoginPath(role)}>Return to sign in</Link> : null}
      </form>
    </AuthShell>
  );
};
