import { Navigate, Route, Routes } from 'react-router-dom';
import { PublicLayout } from '@/components/layout/PublicLayout';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { RequireAuth } from '@/routes/RequireAuth';
import { RequireRole } from '@/routes/RequireRole';
import { AboutPage, BursariesPage, BursaryDetailsPage, CareerDetailsPage, CareersPage, CourseDetailsPage, CoursesPage, InstitutionDetailsPage, InstitutionsPage, LandingPage, PricingPage } from '@/pages/public/PublicPages';
import { ForgotPasswordPage, LoginPage, RegisterCompanyPage, RegisterStudentPage, ResetPasswordPage } from '@/pages/public/AuthPages';
import { StudentAcademicProfilePage, StudentApplicationsPage, StudentBursaryRecommendationsPage, StudentCareerRecommendationsPage, StudentDashboardPage, StudentDocumentsPage, StudentExperiencePage, StudentNotificationsPage, StudentProfilePage, StudentQualificationsPage, StudentSavedPage, StudentSettingsPage, StudentSubscriptionPage } from '@/pages/student/StudentPages';
import { CompanyApplicantsPage, CompanyBursariesPage, CompanyCreateBursaryPage, CompanyDashboardPage, CompanyEditBursaryPage, CompanyNotificationsPage, CompanyProfilePage, CompanySettingsPage, CompanyShortlistedPage, CompanyTalentSearchPage, CompanyVerificationDocsPage } from '@/pages/company/CompanyPages';
import { AdminAnalyticsPage, AdminAuditLogsPage, AdminBursaryModerationPage, AdminDashboardPage, AdminNotificationTemplatesPage, AdminPaymentsPage, AdminPendingApprovalsPage, AdminRolesPage, AdminSettingsPage, AdminSubscriptionsPage, AdminUsersPage } from '@/pages/admin/AdminPages';

export const App = () => (
  <Routes>
    <Route element={<PublicLayout />}>
      <Route path="/" element={<LandingPage />} />
      <Route path="/about" element={<AboutPage />} />
      <Route path="/careers" element={<CareersPage />} />
      <Route path="/careers/:id" element={<CareerDetailsPage />} />
      <Route path="/courses" element={<CoursesPage />} />
      <Route path="/courses/:id" element={<CourseDetailsPage />} />
      <Route path="/institutions" element={<InstitutionsPage />} />
      <Route path="/institutions/:id" element={<InstitutionDetailsPage />} />
      <Route path="/bursaries" element={<BursariesPage />} />
      <Route path="/bursaries/:id" element={<BursaryDetailsPage />} />
      <Route path="/pricing" element={<PricingPage />} />
      <Route path="/auth/login" element={<LoginPage />} />
      <Route path="/auth/register/student" element={<RegisterStudentPage />} />
      <Route path="/auth/register/company" element={<RegisterCompanyPage />} />
      <Route path="/auth/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/auth/reset-password" element={<ResetPasswordPage />} />
    </Route>

    <Route element={<RequireAuth />}>
      <Route element={<RequireRole role="STUDENT" />}>
        <Route element={<DashboardLayout />}>
          <Route path="/student/dashboard" element={<StudentDashboardPage />} />
          <Route path="/student/profile" element={<StudentProfilePage />} />
          <Route path="/student/academic-profile" element={<StudentAcademicProfilePage />} />
          <Route path="/student/documents" element={<StudentDocumentsPage />} />
          <Route path="/student/qualifications" element={<StudentQualificationsPage />} />
          <Route path="/student/experience" element={<StudentExperiencePage />} />
          <Route path="/student/recommendations/careers" element={<StudentCareerRecommendationsPage />} />
          <Route path="/student/recommendations/bursaries" element={<StudentBursaryRecommendationsPage />} />
          <Route path="/student/saved" element={<StudentSavedPage />} />
          <Route path="/student/applications" element={<StudentApplicationsPage />} />
          <Route path="/student/notifications" element={<StudentNotificationsPage />} />
          <Route path="/student/subscription" element={<StudentSubscriptionPage />} />
          <Route path="/student/settings" element={<StudentSettingsPage />} />
        </Route>
      </Route>

      <Route element={<RequireRole role="COMPANY" />}>
        <Route element={<DashboardLayout />}>
          <Route path="/company/dashboard" element={<CompanyDashboardPage />} />
          <Route path="/company/profile" element={<CompanyProfilePage />} />
          <Route path="/company/verification-docs" element={<CompanyVerificationDocsPage />} />
          <Route path="/company/bursaries" element={<CompanyBursariesPage />} />
          <Route path="/company/bursaries/create" element={<CompanyCreateBursaryPage />} />
          <Route path="/company/bursaries/:id/edit" element={<CompanyEditBursaryPage />} />
          <Route path="/company/applicants" element={<CompanyApplicantsPage />} />
          <Route path="/company/talent-search" element={<CompanyTalentSearchPage />} />
          <Route path="/company/shortlisted" element={<CompanyShortlistedPage />} />
          <Route path="/company/notifications" element={<CompanyNotificationsPage />} />
          <Route path="/company/settings" element={<CompanySettingsPage />} />
        </Route>
      </Route>

      <Route element={<RequireRole role="ADMIN" />}>
        <Route element={<DashboardLayout />}>
          <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
          <Route path="/admin/users" element={<AdminUsersPage />} />
          <Route path="/admin/roles" element={<AdminRolesPage />} />
          <Route path="/admin/pending-approvals" element={<AdminPendingApprovalsPage />} />
          <Route path="/admin/bursaries" element={<AdminBursaryModerationPage />} />
          <Route path="/admin/subscriptions" element={<AdminSubscriptionsPage />} />
          <Route path="/admin/payments" element={<AdminPaymentsPage />} />
          <Route path="/admin/notification-templates" element={<AdminNotificationTemplatesPage />} />
          <Route path="/admin/analytics" element={<AdminAnalyticsPage />} />
          <Route path="/admin/audit-logs" element={<AdminAuditLogsPage />} />
          <Route path="/admin/settings" element={<AdminSettingsPage />} />
        </Route>
      </Route>
    </Route>

    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
);
