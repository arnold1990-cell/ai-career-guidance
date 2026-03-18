import { Bell, LogOut, Menu } from 'lucide-react';
import { Link, Outlet } from 'react-router-dom';
import { useState } from 'react';
import { resolvePrimaryRole } from '@/features/auth/roleUtils';
import { useAuth } from '@/hooks/useAuth';
import type { Role } from '@/types';
import eduriteSidebarLogo from '@/assets/Edurite-dashboard.jpeg';

const navByRole: Record<Role, Array<{ to: string; label: string }>> = {
  STUDENT: [
    { to: '/student/dashboard', label: 'Dashboard' }, { to: '/student/profile', label: 'My Profile' }, { to: '/student/recommendations/careers', label: 'AI Guidance' }, { to: '/student/saved', label: 'Career Search' }, { to: '/student/applications', label: 'Bursary Finder' }, { to: '/student/universities', label: 'Universities' }, { to: '/student/subscription', label: 'Subscription' }, { to: '/student/notifications', label: 'Notifications' }, { to: '/student/settings', label: 'Settings' },
  ],
  COMPANY: [
    { to: '/company/dashboard', label: 'Dashboard' }, { to: '/company/profile', label: 'Company Profile' }, { to: '/company/bursaries/new', label: 'Post Bursary' }, { to: '/company/bursaries', label: 'Manage Bursaries' }, { to: '/company/applicants', label: 'Applications' }, { to: '/company/verification-docs', label: 'Documents' }, { to: '/company/notifications', label: 'Notifications' }, { to: '/company/settings', label: 'Settings' },
  ],
  ADMIN: [
    { to: '/admin/dashboard', label: 'Dashboard' }, { to: '/admin/users', label: 'User Management' }, { to: '/admin/pending-approvals', label: 'Company Approvals' }, { to: '/admin/bursaries', label: 'Bursary Management' }, { to: '/admin/analytics', label: 'Analytics' }, { to: '/admin/settings', label: 'System Settings' },
  ],
};

export const DashboardLayout = () => {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  if (!user) return null;

  const primaryRole = resolvePrimaryRole(user);
  if (!primaryRole) return null;

  return (
    <div className="flex min-h-screen">
      <aside className={`fixed z-20 h-full w-64 bg-[#0A0E2B] p-4 text-white md:static ${open ? 'block' : 'hidden md:block'}`}>
        <img src={eduriteSidebarLogo} alt="EduRite logo" className="mb-8 w-full max-w-[220px]" />
        <nav className="space-y-2">
          {navByRole[primaryRole].map((item) => (
            <Link key={item.to} className="block rounded-lg px-3 py-2 hover:bg-[#13205f]" to={item.to}>{item.label}</Link>
          ))}
        </nav>
      </aside>
      <div className="flex-1">
        <header className="flex items-center justify-between border-b bg-white p-4">
          <button className="md:hidden" onClick={() => setOpen((v) => !v)} aria-label="Toggle menu"><Menu size={20} /></button>
          <div className="text-sm text-slate-500">Role: {primaryRole}</div>
          <div className="flex items-center gap-3">
            <button aria-label="Notifications"><Bell size={18} /></button>
            <button onClick={() => logout()} className="rounded p-1 hover:bg-slate-100" aria-label="Logout"><LogOut size={18} /></button>
          </div>
        </header>
        <main className="p-4 md:p-6"><Outlet /></main>
      </div>
    </div>
  );
};
