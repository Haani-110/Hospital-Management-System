/**
 * API response contracts.
 *
 * These mirror the backend DTOs in `web/backend/src/**\/dto/*.dto.ts` exactly.
 * Update them together with the backend — never guess a field name.
 */

/* ------------------------------------------------------------------ enums */

/** `Role` enum from prisma/schema.prisma. */
export const Role = {
  ADMIN: 'ADMIN',
  DOCTOR: 'DOCTOR',
  RECEPTIONIST: 'RECEPTIONIST',
} as const;
export type Role = (typeof Role)[keyof typeof Role];

/** `Gender` enum from prisma/schema.prisma. */
export const Gender = {
  MALE: 'MALE',
  FEMALE: 'FEMALE',
  OTHER: 'OTHER',
} as const;
export type Gender = (typeof Gender)[keyof typeof Gender];

/** `AppointmentStatus` enum from prisma/schema.prisma. */
export const AppointmentStatus = {
  SCHEDULED: 'SCHEDULED',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
} as const;
export type AppointmentStatus = (typeof AppointmentStatus)[keyof typeof AppointmentStatus];

/** `BillStatus` enum from prisma/schema.prisma. */
export const BillStatus = {
  UNPAID: 'UNPAID',
  PARTIALLY_PAID: 'PARTIALLY_PAID',
  PAID: 'PAID',
  CANCELLED: 'CANCELLED',
} as const;
export type BillStatus = (typeof BillStatus)[keyof typeof BillStatus];

/* -------------------------------------------------------------- envelopes */

/** `PaginationMetaDto` — metadata returned by every list endpoint. */
export interface PaginationMeta {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}

/** `PaginatedResponseDto<T>` — envelope returned by every list endpoint. */
export interface Paginated<T> {
  data: T[];
  meta: PaginationMeta;
}

/** `ErrorResponseBody` from AllExceptionsFilter. */
export interface ApiErrorBody {
  statusCode: number;
  error: string;
  /** A single message, or an array of validation messages. */
  message: string | string[];
  path?: string;
  timestamp?: string;
}

/* ------------------------------------------------------------------ models */

/** `UserProfileDto` / `UserResponseDto` — never contains a password hash. */
export interface User {
  id: string;
  username: string;
  email: string | null;
  role: Role;
  active: boolean;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/** `LoginResponseDto`. */
export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: string;
  user: User;
}

/** `PatientResponseDto`. */
export interface Patient {
  id: string;
  patientCode: string;
  fullName: string;
  /** ISO date (`YYYY-MM-DD`). */
  dateOfBirth: string;
  gender: Gender;
  phone: string | null;
  email: string | null;
  address: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  bloodGroup: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/** `DoctorDepartmentDto`. */
export interface DoctorDepartment {
  id: string;
  name: string;
}

/** `DoctorResponseDto` — `consultationFee` is a decimal **string**. */
export interface Doctor {
  id: string;
  userId: string | null;
  departmentId: string;
  fullName: string;
  specialization: string | null;
  phone: string | null;
  email: string | null;
  consultationFee: string;
  active: boolean;
  department?: DoctorDepartment;
  createdAt: string;
  updatedAt: string;
}

export interface AppointmentPatientRef {
  id: string;
  patientCode: string;
  fullName: string;
}

export interface AppointmentDoctorRef {
  id: string;
  fullName: string;
  specialization: string | null;
}

/** `AppointmentResponseDto`. */
export interface Appointment {
  id: string;
  patientId: string;
  doctorId: string;
  scheduledAt: string;
  status: AppointmentStatus;
  reason: string | null;
  notes: string | null;
  patient?: AppointmentPatientRef;
  doctor?: AppointmentDoctorRef;
  createdAt: string;
  updatedAt: string;
}

export interface MedicalRecordPatientRef {
  id: string;
  patientCode: string;
  fullName: string;
}

export interface MedicalRecordDoctorRef {
  id: string;
  fullName: string;
}

/** `MedicalRecordResponseDto`. */
export interface MedicalRecord {
  id: string;
  appointmentId: string;
  patientId: string;
  doctorId: string;
  diagnosis: string;
  symptoms: string | null;
  examination: string | null;
  treatmentNotes: string | null;
  /** ISO date (`YYYY-MM-DD`). */
  recordDate: string;
  patient?: MedicalRecordPatientRef;
  doctor?: MedicalRecordDoctorRef;
  createdAt: string;
  updatedAt: string;
}

/** `PrescriptionItemResponseDto`. */
export interface PrescriptionItem {
  id: string;
  medicineName: string;
  dosage: string | null;
  frequency: string | null;
  duration: string | null;
  instructions: string | null;
}

export interface PrescriptionPatientRef {
  id: string;
  patientCode: string;
  fullName: string;
}

export interface PrescriptionDoctorRef {
  id: string;
  fullName: string;
}

/** `PrescriptionResponseDto`. */
export interface Prescription {
  id: string;
  medicalRecordId: string;
  patientId: string;
  doctorId: string;
  /** ISO date (`YYYY-MM-DD`). */
  prescriptionDate: string;
  notes: string | null;
  patient?: PrescriptionPatientRef;
  doctor?: PrescriptionDoctorRef;
  items: PrescriptionItem[];
  createdAt: string;
  updatedAt: string;
}

/** `BillItemResponseDto` — monetary values are decimal **strings**. */
export interface BillItem {
  id: string;
  description: string;
  quantity: number;
  unitPrice: string;
  amount: string;
}

export interface BillPatientRef {
  id: string;
  patientCode: string;
  fullName: string;
}

/** `BillResponseDto` — monetary values are decimal **strings**. */
export interface Bill {
  id: string;
  billNumber: string;
  patientId: string;
  appointmentId: string | null;
  /** ISO date (`YYYY-MM-DD`). */
  billDate: string;
  status: BillStatus;
  subtotal: string;
  discount: string;
  totalAmount: string;
  notes: string | null;
  patient?: BillPatientRef;
  items: BillItem[];
  createdAt: string;
  updatedAt: string;
}

/** `DashboardSummaryDto` — `todayRevenue` is a decimal **string**. */
export interface DashboardSummary {
  totalPatients: number;
  activeDoctors: number;
  todayAppointments: number;
  pendingAppointments: number;
  completedAppointments: number;
  unpaidBills: number;
  partiallyPaidBills: number;
  todayRevenue: string;
}

/** `GET /health` payload. */
export interface HealthStatus {
  status: string;
  timestamp: string;
}

/* ------------------------------------------------------------------ queries */

/**
 * Query objects are declared as type aliases rather than interfaces: only
 * aliases receive an implicit index signature, which is what lets them be
 * passed straight to the query-string builder.
 */
export type PaginationQuery = {
  page?: number;
  limit?: number;
};

/** `PatientsQueryDto`. */
export type PatientsQuery = PaginationQuery & {
  search?: string;
  active?: boolean;
};

/** `DoctorsQueryDto`. */
export type DoctorsQuery = PaginationQuery & {
  search?: string;
  departmentId?: string;
  active?: boolean;
};

/** `AppointmentsQueryDto`. */
export type AppointmentsQuery = PaginationQuery & {
  patientId?: string;
  doctorId?: string;
  status?: AppointmentStatus;
  /** ISO date-time; inclusive lower bound. */
  from?: string;
  /** ISO date-time; exclusive upper bound. */
  to?: string;
};

/** `MedicalRecordsQueryDto`. */
export type MedicalRecordsQuery = PaginationQuery & {
  patientId?: string;
  doctorId?: string;
  /** Matches the diagnosis field only. */
  search?: string;
  from?: string;
  to?: string;
};

/** `PrescriptionsQueryDto`. */
export type PrescriptionsQuery = PaginationQuery & {
  patientId?: string;
  doctorId?: string;
  medicalRecordId?: string;
  from?: string;
  to?: string;
};

/** `BillsQueryDto`. */
export type BillsQuery = PaginationQuery & {
  patientId?: string;
  appointmentId?: string;
  status?: BillStatus;
  from?: string;
  to?: string;
};

/** `UsersQueryDto`. */
export type UsersQuery = PaginationQuery & {
  role?: Role;
  active?: boolean;
  search?: string;
};
