import { Link, Outlet } from 'react-router-dom';

export const PublicLayout = () => (
  <div className="min-h-screen">
    <header className="border-b bg-white">
      <div className="mx-auto flex max-w-7xl items-center justify-between p-4">
        <Link to="/" className="text-xl font-bold text-primary-600">EduRite</Link>
        <nav className="flex gap-4 text-sm">
          <Link to="/about">About</Link>
          <Link to="/careers">Careers</Link>
          <Link to="/courses">Courses</Link>
          <Link to="/bursaries">Bursaries</Link>
          <Link to="/pricing">Pricing</Link>
          <Link to="/auth/login" className="font-semibold text-primary-600">Login</Link>
        </nav>
      </div>
    </header>
    <main className="mx-auto max-w-7xl p-4 md:p-6"><Outlet /></main>
  </div>
);
