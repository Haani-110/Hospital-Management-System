import { ApiProperty } from '@nestjs/swagger';

/**
 * Dashboard metrics mirroring the v1.0 dashboard, computed from PostgreSQL.
 */
export class DashboardSummaryDto {
  @ApiProperty({ description: 'All patient records, active and inactive' })
  totalPatients: number;

  @ApiProperty({ description: 'Doctors with active = true' })
  activeDoctors: number;

  @ApiProperty({ description: 'Appointments scheduled for today (any status)' })
  todayAppointments: number;

  @ApiProperty({ description: 'Appointments with status SCHEDULED' })
  pendingAppointments: number;

  @ApiProperty({ description: 'Appointments with status COMPLETED' })
  completedAppointments: number;

  @ApiProperty({ description: 'Bills with status UNPAID' })
  unpaidBills: number;

  @ApiProperty({ description: 'Bills with status PARTIALLY_PAID' })
  partiallyPaidBills: number;

  @ApiProperty({ description: 'Sum of totals for bills dated today with status PAID' })
  todayRevenue: string;
}
