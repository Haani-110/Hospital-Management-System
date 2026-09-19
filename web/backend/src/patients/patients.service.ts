import { Injectable, NotFoundException } from '@nestjs/common';
import { Prisma, PrismaService } from '../prisma/prisma.service.js';
import { PaginatedResponseDto } from '../common/dto/paginated-response.dto.js';
import { PatientsQueryDto } from './dto/patient-query.dto.js';
import { PatientResponseDto } from './dto/patient-response.dto.js';

export const patientSelect = {
  id: true,
  patientCode: true,
  fullName: true,
  dateOfBirth: true,
  gender: true,
  phone: true,
  email: true,
  address: true,
  emergencyContactName: true,
  emergencyContactPhone: true,
  bloodGroup: true,
  active: true,
  createdAt: true,
  updatedAt: true,
} satisfies Prisma.PatientSelect;

@Injectable()
export class PatientsService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(query: PatientsQueryDto): Promise<PaginatedResponseDto<PatientResponseDto>> {
    const where: Prisma.PatientWhereInput = {
      ...(query.active !== undefined ? { active: query.active } : {}),
      ...(query.search
        ? {
            OR: [
              { fullName: { contains: query.search, mode: 'insensitive' } },
              { patientCode: { contains: query.search, mode: 'insensitive' } },
            ],
          }
        : {}),
    };

    const [total, patients] = await this.prisma.$transaction([
      this.prisma.patient.count({ where }),
      this.prisma.patient.findMany({
        where,
        orderBy: { fullName: 'asc' },
        skip: query.skip,
        take: query.limit,
        select: patientSelect,
      }),
    ]);

    return PaginatedResponseDto.of(patients, query.page, query.limit, total);
  }

  async findOne(id: string): Promise<PatientResponseDto> {
    const patient = await this.prisma.patient.findUnique({ where: { id }, select: patientSelect });
    if (!patient) {
      throw new NotFoundException('Patient not found');
    }
    return patient;
  }
}
