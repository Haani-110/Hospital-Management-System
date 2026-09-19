import {
  Banknote,
  BedDouble,
  CalendarDays,
  ClipboardList,
  FlaskConical,
  LayoutDashboard,
  Pill,
  Settings,
  ShieldCheck,
  Stethoscope,
  Users,
  FileText,
} from 'lucide-react';
import { Role } from '@/lib/api/types';

export interface NavItem {
  label: string;
  to: string;
  icon: React.ElementType;
  /**
   * Roles allowed to open this screen. Mirrors the backend `@Roles(...)`
   * decorators — the API remains the source of truth, this only keeps
   * unreachable screens out of the navigation.
   */
  roles?: Role[];
  /** Module is intentionally a foundation only (no backend API yet). */
  foundation?: boolean;
}

export interface NavSection {
  title: string;
  items: NavItem[];
}

export const navigation: NavSection[] = [
  {
    title: 'Overview',
    items: [{ label: 'Dashboard', to: '/', icon: LayoutDashboard }],
  },
  {
    title: 'Clinical',
    items: [
      { label: 'Patients', to: '/patients', icon: Users },
      { label: 'Doctors', to: '/doctors', icon: Stethoscope },
      { label: 'Appointments', to: '/appointments', icon: CalendarDays },
      { label: 'Medical Records', to: '/medical-records', icon: FileText },
      { label: 'Prescriptions', to: '/prescriptions', icon: ClipboardList },
      { label: 'Laboratory', to: '/laboratory', icon: FlaskConical, foundation: true },
    ],
  },
  {
    title: 'Operations',
    items: [
      { label: 'Pharmacy', to: '/pharmacy', icon: Pill, foundation: true },
      {
        label: 'Billing',
        to: '/billing',
        icon: Banknote,
        // The backend restricts /bills to these two roles; DOCTOR receives 403.
        roles: [Role.ADMIN, Role.RECEPTIONIST],
      },
      { label: 'Admissions', to: '/admissions', icon: BedDouble, foundation: true },
    ],
  },
  {
    title: 'Reports',
    items: [{ label: 'Reports', to: '/reports', icon: ClipboardList }],
  },
  {
    title: 'System',
    items: [
      { label: 'Users', to: '/users', icon: ShieldCheck, roles: [Role.ADMIN] },
      { label: 'Settings', to: '/settings', icon: Settings },
    ],
  },
];

/** Navigation sections filtered for the signed-in role. */
export function navigationForRole(role: Role | undefined): NavSection[] {
  return navigation
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => !item.roles || (role !== undefined && item.roles.includes(role))),
    }))
    .filter((section) => section.items.length > 0);
}
