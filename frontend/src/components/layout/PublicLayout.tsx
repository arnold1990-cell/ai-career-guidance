import { Link, Outlet, useLocation } from 'react-router-dom';
import eduRiteLogo from '@/assets/edurite-logo.svg';

export const PublicLayout = () => {
  const { pathname } = useLocation();
  const isAuthRoute = pathname.includes('/login');

  return (
    <div className="min-h-screen bg-slate-100">
      <header className="site-header border-b border-slate-200 bg-white">
        <div className="site-header-inner mx-auto w-full max-w-[1280px]">
          <Link to="/" aria-label="EduRite home" className="brand">
            <img src={eduRiteLogo} alt="EduRite logo" className="brand-logo" />
          </Link>
          <nav className="main-nav text-lg text-slate-800">
            <Link to="/about">About</Link>
            <Link to="/careers">Careers</Link>
            <Link to="/courses">Courses</Link>
            <Link to="/bursaries">Bursaries</Link>
            <Link to="/pricing">Pricing</Link>
            <Link to="/auth/login" className="font-semibold text-primary-600">Login</Link>
          </nav>
        </div>
      </header>
      <main className={isAuthRoute ? 'mx-auto w-full max-w-[1280px] px-8 py-8' : 'mx-auto w-full max-w-[1280px] px-4 py-4 md:px-6 md:py-6'}>
        <Outlet />
      </main>
    </div>
  );
};
