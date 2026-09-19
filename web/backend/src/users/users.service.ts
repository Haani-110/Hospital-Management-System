import { Injectable, NotFoundException } from '@nestjs/common';
import { Prisma, PrismaService } from '../prisma/prisma.service.js';
import { PaginatedResponseDto } from '../common/dto/paginated-response.dto.js';
import { UsersQueryDto } from './dto/user-query.dto.js';
import { UserResponseDto } from './dto/user-response.dto.js';

@Injectable()
export class UsersService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(query: UsersQueryDto): Promise<PaginatedResponseDto<UserResponseDto>> {
    const where: Prisma.UserWhereInput = {
      ...(query.role ? { role: query.role } : {}),
      ...(query.active !== undefined ? { active: query.active } : {}),
      ...(query.search
        ? {
            OR: [
              { username: { contains: query.search, mode: 'insensitive' } },
              { email: { contains: query.search, mode: 'insensitive' } },
            ],
          }
        : {}),
    };

    const [total, users] = await this.prisma.$transaction([
      this.prisma.user.count({ where }),
      this.prisma.user.findMany({
        where,
        orderBy: { username: 'asc' },
        skip: query.skip,
        take: query.limit,
        // Omit is enforced at the query level: hashes cannot leak by accident.
        omit: { passwordHash: true },
      }),
    ]);

    return PaginatedResponseDto.of(users, query.page, query.limit, total);
  }

  async findOne(id: string): Promise<UserResponseDto> {
    const user = await this.prisma.user.findUnique({
      where: { id },
      omit: { passwordHash: true },
    });

    if (!user) {
      throw new NotFoundException('User not found');
    }

    return user;
  }
}
