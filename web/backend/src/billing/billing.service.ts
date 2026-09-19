import { Injectable, NotFoundException } from '@nestjs/common';
import { Prisma, PrismaService } from '../prisma/prisma.service.js';
import { PaginatedResponseDto } from '../common/dto/paginated-response.dto.js';
import { BillsQueryDto } from './dto/bill-query.dto.js';
import { BillResponseDto } from './dto/bill-response.dto.js';

export const billSelect = {
  id: true,
  billNumber: true,
  patientId: true,
  appointmentId: true,
  billDate: true,
  status: true,
  subtotal: true,
  discount: true,
  totalAmount: true,
  notes: true,
  createdAt: true,
  updatedAt: true,
  patient: { select: { id: true, patientCode: true, fullName: true } },
  items: {
    select: { id: true, description: true, quantity: true, unitPrice: true, amount: true },
    orderBy: { createdAt: 'asc' },
  },
} satisfies Prisma.BillSelect;

@Injectable()
export class BillingService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(query: BillsQueryDto): Promise<PaginatedResponseDto<BillResponseDto>> {
    const where: Prisma.BillWhereInput = {
      ...(query.patientId ? { patientId: query.patientId } : {}),
      ...(query.appointmentId ? { appointmentId: query.appointmentId } : {}),
      ...(query.status ? { status: query.status } : {}),
      ...(query.from || query.to
        ? {
            billDate: {
              ...(query.from ? { gte: query.from } : {}),
              ...(query.to ? { lt: query.to } : {}),
            },
          }
        : {}),
    };

    const [total, bills] = await this.prisma.$transaction([
      this.prisma.bill.count({ where }),
      this.prisma.bill.findMany({
        where,
        orderBy: { billDate: 'desc' },
        skip: query.skip,
        take: query.limit,
        select: billSelect,
      }),
    ]);

    return PaginatedResponseDto.of(bills.map(toBillResponse), query.page, query.limit, total);
  }

  async findOne(id: string): Promise<BillResponseDto> {
    const bill = await this.prisma.bill.findUnique({ where: { id }, select: billSelect });
    if (!bill) {
      throw new NotFoundException('Bill not found');
    }
    return toBillResponse(bill);
  }
}

/** Converts Prisma bill rows into the API shape (Decimal -> string). */
export function toBillResponse(bill: {
  id: string;
  billNumber: string;
  patientId: string;
  appointmentId: string | null;
  billDate: Date;
  status: BillResponseDto['status'];
  subtotal: { toString(): string };
  discount: { toString(): string };
  totalAmount: { toString(): string };
  notes: string | null;
  createdAt: Date;
  updatedAt: Date;
  patient?: { id: string; patientCode: string; fullName: string } | null;
  items: {
    id: string;
    description: string;
    quantity: number;
    unitPrice: { toString(): string };
    amount: { toString(): string };
  }[];
}): BillResponseDto {
  return {
    ...bill,
    // Monetary values are returned as decimal strings, never floats.
    subtotal: bill.subtotal.toString(),
    discount: bill.discount.toString(),
    totalAmount: bill.totalAmount.toString(),
    patient: bill.patient ?? undefined,
    items: bill.items.map((item) => ({
      ...item,
      unitPrice: item.unitPrice.toString(),
      amount: item.amount.toString(),
    })),
  };
}
