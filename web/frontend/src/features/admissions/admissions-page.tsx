import { PlannedModulePage } from '@/components/layout/planned-module-page';

/**
 * Admissions — foundation screen.
 *
 * The backend tracks outpatient activity (appointments, records,
 * prescriptions, bills). Wards, beds and inpatient stays have no endpoints yet.
 */
export function AdmissionsPage() {
  return (
    <PlannedModulePage
      module={{
        eyebrow: 'Operations',
        title: 'Admissions',
        description:
          'Inpatient admissions, ward and bed allocation, transfers and discharge summaries.',
        plannedCapabilities: [
          'Ward and bed inventory with occupancy status',
          'Admission requests approved from an appointment or emergency visit',
          'Bed allocation, transfers between wards and attending-doctor assignment',
          'Daily inpatient notes and nursing observations',
          'Discharge summaries feeding the patient record and final bill',
        ],
        availableToday: [
          {
            label: 'Patients',
            to: '/patients',
            description: 'Identities and contacts for admission paperwork',
          },
          {
            label: 'Doctors',
            to: '/doctors',
            description: 'Staff who would be assigned as attending physicians',
          },
          {
            label: 'Appointments',
            to: '/appointments',
            description: 'Outpatient visits that can lead to admission',
          },
          {
            label: 'Billing',
            to: '/billing',
            description: 'Invoices that would carry bed and inpatient charges',
          },
        ],
      }}
    />
  );
}
