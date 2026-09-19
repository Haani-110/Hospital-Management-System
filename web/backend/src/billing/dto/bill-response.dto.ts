import { ApiProperty } from '@nestjs/swagger';
import { BillStatus } from '../../generated/prisma/client.js';

export class BillItemResponseDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty() description: string;
  @ApiProperty() quantity: number;
  @ApiProperty({ description: 'Decimal string' }) unitPrice: string;
  @ApiProperty({ description: 'Decimal string' }) amount: string;
}

export class BillPatientRefDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty() patientCode: string;
  @ApiProperty() fullName: string;
}

export class BillResponseDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty({ example: 'BILL-000001' }) billNumber: string;
  @ApiProperty({ format: 'uuid' }) patientId: string;
  @ApiProperty({ format: 'uuid', nullable: true }) appointmentId: string | null;
  @ApiProperty({ type: String, format: 'date' }) billDate: Date;
  @ApiProperty({ enum: BillStatus }) status: BillStatus;
  @ApiProperty({ description: 'Decimal string' }) subtotal: string;
  @ApiProperty({ description: 'Decimal string' }) discount: string;
  @ApiProperty({ description: 'Decimal string' }) totalAmount: string;
  @ApiProperty({ nullable: true }) notes: string | null;
  @ApiProperty({ type: BillPatientRefDto, nullable: true }) patient?: BillPatientRefDto;
  @ApiProperty({ type: [BillItemResponseDto] }) items: BillItemResponseDto[];
  @ApiProperty() createdAt: Date;
  @ApiProperty() updatedAt: Date;
}
