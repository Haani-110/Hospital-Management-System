import { Injectable, NotFoundException } from '@nestjs/common';
import { Prisma, PrismaService } from '../prisma/prisma.service.js';
import { PaginatedResponseDto } from '../common/dto/paginated-response.dto.js';
import { DoctorsQueryDto } from './dto/doctor-query.dto.js';
import { DoctorResponseDto } from './dto/doctor-response.dto.js';

const doctorListSelect = {
  id: true,
  userId: true,
  departmentId: true,
  fullName: true,
  specialization: true,
  phone: true,
  email: true,
  consultationFee: true,
  active: true,
  createdAt: true,
  updatedAt: true,
  department: { select: { id: true, name: true } },
} satisfies Prisma.DoctorSelect;

@Injectable()
export class DoctorsService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(query: DoctorsQueryDto): Promise<PaginatedResponseDto<DoctorResponseDto>> {
    const where: Prisma.DoctorWhereInput = {
      ...(query.departmentId ? { departmentId: query.departmentId } : {}),
      ...(query.active !== undefined ? { active: query.active } : {}),
      ...(query.search
        ? {
            OR: [
              { fullName: { contains: query.search, mode: 'insensitive' } },
              { specialization: { contains: query.search, mode: 'insensitive' } },
            ],
          }
        : {}),
    };

    const [total, doctors] = await this.prisma.$transaction([
      this.prisma.doctor.count({ where }),
      this.prisma.doctor.findMany({
        where,
        orderBy: { fullName: 'asc' },
        skip: query.skip,
        take: query.limit,
        select: doctorListSelect,
      }),
    ]);

    return PaginatedResponseDto.of(doctors.map(toDoctorResponse), query.page, query.limit, total);
  }

  async findOne(id: string): Promise<DoctorResponseDto> {
    const doctor = await this.prisma.doctor.findUnique({
      where: { id },
      select: doctorListSelect,
    });

    if (!doctor) {
      throw new NotFoundException('Doctor not found');
    }

    return toDoctorResponse(doctor);
  }
}

/** Converts a Prisma doctor row into the API shape (Decimal -> string). */
export function toDoctorResponse(doctor: {
  id: string;
  userId: string | null;
  departmentId: string;
  fullName: string;
  specialization: string | null;
  phone: string | null;
  email: string | null;
  consultationFee: { toString(): string };
  active: boolean;
  createdAt: Date;
  updatedAt: Date;
  department?: { id: string; name: string } | null;
}): DoctorResponseDto {
  return {
    ...doctor,
    // Serialised as a string so monetary values keep exact decimal precision.
    consultationFee: doctor.consultationFee.toString(),
    department: doctor.department ?? undefined,
  };
}
