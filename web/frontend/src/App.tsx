import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from '@/components/layout/app-shell';
import { FullPageLoading } from '@/components/layout/full-page-loading';
import { ProtectedRoute } from '@/routes/protected-route';
import { RoleRoute } from '@/routes/role-route';
import { ForbiddenPage } from '@/routes/forbidden-page';
import { NotFoundPage } from '@/routes/not-found-page';
import { LoginPage } from '@/features/auth/login-page';
import { DashboardPage } from '@/features/dashboard/dashboard-page';
import { PatientsPage } from '@/features/patients/patients-page';
import { PatientDetailPage } from '@/features/patients/patient-detail-page';
import { DoctorsPage } from '@/features/doctors/doctors-page';
import { DoctorDetailPage } from '@/features/doctors/doctor-detail-page';
import { AppointmentsPage } from '@/features/appointments/appointments-page';
import { MedicalRecordsPage } from '@/features/medical-records/medical-records-page';
import { PrescriptionsPage } from '@/features/prescriptions/prescriptions-page';
import { BillingPage } from '@/features/billing/billing-page';
import { BillDetailPage } from '@/features/billing/bill-detail-page';
import { ReportsPage } from '@/features/reports/reports-page';
import { UsersPage } from '@/features/users/users-page';
import { SettingsPage } from '@/features/settings/settings-page';
import { LaboratoryPage } from '@/features/laboratory/laboratory-page';
import { PharmacyPage } from '@/features/pharmacy/pharmacy-page';
import { AdmissionsPage } from '@/features/admissions/admissions-page';
import { Role } from '@/lib/api/types';
import { useAuth } from '@/providers/auth-context';

/** Sends already-authenticated users away from the public login screen. */
function PublicOnlyRoute({ children }: { children: React.ReactNode }) {
  const { status } = useAuth();
  if (status === 'loading') return <FullPageLoading />;
  if (status === 'authenticated') return <Navigate to="/" replace />;
  return <>{children}</>;
}

export function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <PublicOnlyRoute>
            <LoginPage />
          </PublicOnlyRoute>
        }
      />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />

          <Route path="patients" element={<PatientsPage />} />
          <Route path="patients/:patientId" element={<PatientDetailPage />} />

          <Route path="doctors" element={<DoctorsPage />} />
          <Route path="doctors/:doctorId" element={<DoctorDetailPage />} />

          <Route path="appointments" element={<AppointmentsPage />} />
          <Route path="medical-records" element={<MedicalRecordsPage />} />
          <Route path="prescriptions" element={<PrescriptionsPage />} />

          <Route path="laboratory" element={<LaboratoryPage />} />
          <Route path="pharmacy" element={<PharmacyPage />} />
          <Route path="admissions" element={<AdmissionsPage />} />

          <Route path="reports" element={<ReportsPage />} />

          <Route path="billing" element={<BillingPage />} />
          <Route path="billing/:billId" element={<BillDetailPage />} />

          {/* Admin-only screens: the API enforces the same restriction. */}
          <Route element={<RoleRoute allow={[Role.ADMIN]} />}>
            <Route path="users" element={<UsersPage />} />
          </Route>

          <Route path="settings" element={<SettingsPage />} />
          <Route path="forbidden" element={<ForbiddenPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
