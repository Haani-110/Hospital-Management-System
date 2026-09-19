import { Injectable, NotFoundException } from '@nestjs/common';
import { Prisma, PrismaService } from '../prisma/prisma.service.js';
import { PaginatedResponseDto } from '../common/dto/paginated-response.dto.js';
import { AppointmentsQueryDto } from './dto/appointment-query.dto.js';
import { AppointmentResponseDto } from './dto/appointment-response.dto.js';

export const appointmentSelect = {
  id: true,
  patientId: true,
  doctorId: true,
  scheduledAt: true,
  status: true,
  reason: true,
  notes: true,
  createdAt: true,
  updatedAt: true,
  patient: { select: { id: true, patientCode: true, fullName: true } },
  doctor: { select: { id: true, fullName: true, specialization: true } },
} satisfies Prisma.AppointmentSelect;

@Injectable()
export class AppointmentsService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(
    query: AppointmentsQueryDto,
  ): Promise<PaginatedResponseDto<AppointmentResponseDto>> {
    const where: Prisma.AppointmentWhereInput = {
      ...(query.patientId ? { patientId: query.patientId } : {}),
      ...(query.doctorId ? { doctorId: query.doctorId } : {}),
      ...(query.status ? { status: query.status } : {}),
      ...(query.from || query.to
        ? {
            scheduledAt: {
              ...(query.from ? { gte: query.from } : {}),
              ...(query.to ? { lt: query.to } : {}),
            },
          }
        : {}),
    };

    const [total, appointments] = await this.prisma.$transaction([
      this.prisma.appointment.count({ where }),
      this.prisma.appointment.findMany({
        where,
        orderBy: { scheduledAt: 'desc' },
        skip: query.skip,
        take: query.limit,
        select: appointmentSelect,
      }),
    ]);

    return PaginatedResponseDto.of(appointments, query.page, query.limit, total);
  }

  async findOne(id: string): Promise<AppointmentResponseDto> {
    const appointment = await this.prisma.appointment.findUnique({
      where: { id },
      select: appointmentSelect,
    });

    if (!appointment) {
      throw new NotFoundException('Appointment not found');
    }

    return appointment;
  }
}
