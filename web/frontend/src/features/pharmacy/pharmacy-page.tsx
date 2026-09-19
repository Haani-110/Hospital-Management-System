import { PlannedModulePage } from '@/components/layout/planned-module-page';

/**
 * Pharmacy — foundation screen.
 *
 * Medication is recorded on prescriptions today; stock, dispensing and
 * inventory endpoints do not exist yet, so none of that is simulated.
 */
export function PharmacyPage() {
  return (
    <PlannedModulePage
      module={{
        eyebrow: 'Operations',
        title: 'Pharmacy',
        description:
          'Medicine catalogue, stock levels, dispensing against prescriptions and inventory control.',
        plannedCapabilities: [
          'Medicine catalogue with form, strength and unit pricing',
          'Stock ledger with batches, quantities and reorder thresholds',
          'Dispensing a prescription with partial-fill support',
          'Expiry monitoring and low-stock alerts',
          'Purchase and supplier records',
        ],
        availableToday: [
          {
            label: 'Prescriptions',
            to: '/prescriptions',
            description: 'Medicines, dosage, frequency and duration already prescribed',
          },
          {
            label: 'Patients',
            to: '/patients',
            description: 'Patients the pharmacy would dispense to',
          },
          {
            label: 'Billing',
            to: '/billing',
            description: 'Invoices where dispensed medicines would be charged',
          },
        ],
      }}
    />
  );
}
