import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsDate, IsEnum, IsOptional, IsUUID } from 'class-validator';
import { PaginationQueryDto } from '../../common/dto/pagination-query.dto.js';
import { TransformToDate } from '../../common/dto/transforms.js';
import { BillStatus } from '../../generated/prisma/client.js';

export class BillsQueryDto extends PaginationQueryDto {
  @ApiPropertyOptional({ format: 'uuid' })
  @IsUUID()
  @IsOptional()
  patientId?: string;

  @ApiPropertyOptional({ format: 'uuid' })
  @IsUUID()
  @IsOptional()
  appointmentId?: string;

  @ApiPropertyOptional({ enum: BillStatus })
  @IsEnum(BillStatus)
  @IsOptional()
  status?: BillStatus;

  @ApiPropertyOptional({ description: 'Inclusive lower bound (ISO date)' })
  @TransformToDate()
  @IsDate()
  @IsOptional()
  from?: Date;

  @ApiPropertyOptional({ description: 'Exclusive upper bound (ISO date)' })
  @TransformToDate()
  @IsDate()
  @IsOptional()
  to?: Date;
}
