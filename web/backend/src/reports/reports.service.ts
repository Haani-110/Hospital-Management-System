import { Injectable } from '@nestjs/common';
import { AppointmentStatus, BillStatus } from '../generated/prisma/client.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { DashboardSummaryDto } from './dto/dashboard-summary.dto.js';

/** Inclusive start of the current day and exclusive start of the next day. */
export function getTodayRange(now: Date = new Date()): { startOfDay: Date; startOfNextDay: Date } {
  const startOfDay = new Date(now);
  startOfDay.setHours(0, 0, 0, 0);
  const startOfNextDay = new Date(startOfDay);
  startOfNextDay.setDate(startOfNextDay.getDate() + 1);
  return { startOfDay, startOfNextDay };
}

@Injectable()
export class ReportsService {
  constructor(private readonly prisma: PrismaService) {}

  async getDashboardSummary(now: Date = new Date()): Promise<DashboardSummaryDto> {
    const { startOfDay, startOfNextDay } = getTodayRange(now);
    const todayRange = { gte: startOfDay, lt: startOfNextDay };

    const [
      totalPatients,
      activeDoctors,
      todayAppointments,
      pendingAppointments,
      completedAppointments,
      unpaidBills,
      partiallyPaidBills,
      todayRevenue,
    ] = await this.prisma.$transaction([
      this.prisma.patient.count(),
      this.prisma.doctor.count({ where: { active: true } }),
      this.prisma.appointment.count({ where: { scheduledAt: todayRange } }),
      this.prisma.appointment.count({ where: { status: AppointmentStatus.SCHEDULED } }),
      this.prisma.appointment.count({ where: { status: AppointmentStatus.COMPLETED } }),
      this.prisma.bill.count({ where: { status: BillStatus.UNPAID } }),
      this.prisma.bill.count({ where: { status: BillStatus.PARTIALLY_PAID } }),
      this.prisma.bill.aggregate({
        _sum: { totalAmount: true },
        where: { status: BillStatus.PAID, billDate: todayRange },
      }),
    ]);

    return {
      totalPatients,
      activeDoctors,
      todayAppointments,
      pendingAppointments,
      completedAppointments,
      unpaidBills,
      partiallyPaidBills,
      // Decimal is serialised as a string to preserve precision.
      todayRevenue: (todayRevenue._sum.totalAmount ?? 0).toString(),
    };
  }
}
