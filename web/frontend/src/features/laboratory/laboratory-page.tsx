import { PlannedModulePage } from '@/components/layout/planned-module-page';

/**
 * Laboratory — foundation screen.
 *
 * The v2 API has no laboratory endpoints (no test catalogue, orders or
 * results), so this page documents the workflow that will live here and links
 * to the clinical records that do exist.
 */
export function LaboratoryPage() {
  return (
    <PlannedModulePage
      module={{
        eyebrow: 'Clinical',
        title: 'Laboratory',
        description:
          'Test catalogue, sample collection, result entry and report release for diagnostic investigations.',
        plannedCapabilities: [
          'Test catalogue with reference ranges and turnaround targets',
          'Lab orders raised from a consultation or appointment',
          'Sample collection tracking with barcode or accession numbers',
          'Result entry with abnormal-value flagging and verification',
          'Released reports attached to the patient record',
        ],
        availableToday: [
          {
            label: 'Patient records',
            to: '/patients',
            description: 'Demographics and identifiers that lab orders will reference',
          },
          {
            label: 'Medical records',
            to: '/medical-records',
            description: 'Diagnoses and examination notes recorded during consultations',
          },
          {
            label: 'Appointments',
            to: '/appointments',
            description: 'Visits that would generate investigative requests',
          },
          {
            label: 'Prescriptions',
            to: '/prescriptions',
            description: 'Medication already issued to patients',
          },
        ],
      }}
    />
  );
}
