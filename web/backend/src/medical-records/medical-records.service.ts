import { Injectable, NotFoundException } from '@nestjs/common';
import { Prisma, PrismaService } from '../prisma/prisma.service.js';
import { PaginatedResponseDto } from '../common/dto/paginated-response.dto.js';
import { MedicalRecordsQueryDto } from './dto/medical-record-query.dto.js';
import { MedicalRecordResponseDto } from './dto/medical-record-response.dto.js';

export const medicalRecordSelect = {
  id: true,
  appointmentId: true,
  patientId: true,
  doctorId: true,
  diagnosis: true,
  symptoms: true,
  examination: true,
  treatmentNotes: true,
  recordDate: true,
  createdAt: true,
  updatedAt: true,
  patient: { select: { id: true, patientCode: true, fullName: true } },
  doctor: { select: { id: true, fullName: true } },
} satisfies Prisma.MedicalRecordSelect;

@Injectable()
export class MedicalRecordsService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(
    query: MedicalRecordsQueryDto,
  ): Promise<PaginatedResponseDto<MedicalRecordResponseDto>> {
    const where: Prisma.MedicalRecordWhereInput = {
      ...(query.patientId ? { patientId: query.patientId } : {}),
      ...(query.doctorId ? { doctorId: query.doctorId } : {}),
      ...(query.search ? { diagnosis: { contains: query.search, mode: 'insensitive' } } : {}),
      ...(query.from || query.to
        ? {
            recordDate: {
              ...(query.from ? { gte: query.from } : {}),
              ...(query.to ? { lt: query.to } : {}),
            },
          }
        : {}),
    };

    const [total, records] = await this.prisma.$transaction([
      this.prisma.medicalRecord.count({ where }),
      this.prisma.medicalRecord.findMany({
        where,
        orderBy: { recordDate: 'desc' },
        skip: query.skip,
        take: query.limit,
        select: medicalRecordSelect,
      }),
    ]);

    return PaginatedResponseDto.of(records, query.page, query.limit, total);
  }

  async findOne(id: string): Promise<MedicalRecordResponseDto> {
    const record = await this.prisma.medicalRecord.findUnique({
      where: { id },
      select: medicalRecordSelect,
    });

    if (!record) {
      throw new NotFoundException('Medical record not found');
    }

    return record;
  }
}
