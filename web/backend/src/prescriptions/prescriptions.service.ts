import { Injectable, NotFoundException } from '@nestjs/common';
import { Prisma, PrismaService } from '../prisma/prisma.service.js';
import { PaginatedResponseDto } from '../common/dto/paginated-response.dto.js';
import { PrescriptionsQueryDto } from './dto/prescription-query.dto.js';
import { PrescriptionResponseDto } from './dto/prescription-response.dto.js';

export const prescriptionSelect = {
  id: true,
  medicalRecordId: true,
  patientId: true,
  doctorId: true,
  prescriptionDate: true,
  notes: true,
  createdAt: true,
  updatedAt: true,
  patient: { select: { id: true, patientCode: true, fullName: true } },
  doctor: { select: { id: true, fullName: true } },
  items: {
    select: {
      id: true,
      medicineName: true,
      dosage: true,
      frequency: true,
      duration: true,
      instructions: true,
    },
    orderBy: { createdAt: 'asc' },
  },
} satisfies Prisma.PrescriptionSelect;

@Injectable()
export class PrescriptionsService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(
    query: PrescriptionsQueryDto,
  ): Promise<PaginatedResponseDto<PrescriptionResponseDto>> {
    const where: Prisma.PrescriptionWhereInput = {
      ...(query.patientId ? { patientId: query.patientId } : {}),
      ...(query.doctorId ? { doctorId: query.doctorId } : {}),
      ...(query.medicalRecordId ? { medicalRecordId: query.medicalRecordId } : {}),
      ...(query.from || query.to
        ? {
            prescriptionDate: {
              ...(query.from ? { gte: query.from } : {}),
              ...(query.to ? { lt: query.to } : {}),
            },
          }
        : {}),
    };

    const [total, prescriptions] = await this.prisma.$transaction([
      this.prisma.prescription.count({ where }),
      this.prisma.prescription.findMany({
        where,
        orderBy: { prescriptionDate: 'desc' },
        skip: query.skip,
        take: query.limit,
        select: prescriptionSelect,
      }),
    ]);

    return PaginatedResponseDto.of(prescriptions, query.page, query.limit, total);
  }

  async findOne(id: string): Promise<PrescriptionResponseDto> {
    const prescription = await this.prisma.prescription.findUnique({
      where: { id },
      select: prescriptionSelect,
    });

    if (!prescription) {
      throw new NotFoundException('Prescription not found');
    }

    return prescription;
  }
}
