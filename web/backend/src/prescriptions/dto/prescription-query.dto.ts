import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsDate, IsOptional, IsUUID } from 'class-validator';
import { PaginationQueryDto } from '../../common/dto/pagination-query.dto.js';
import { TransformToDate } from '../../common/dto/transforms.js';

export class PrescriptionsQueryDto extends PaginationQueryDto {
  @ApiPropertyOptional({ format: 'uuid' })
  @IsUUID()
  @IsOptional()
  patientId?: string;

  @ApiPropertyOptional({ format: 'uuid' })
  @IsUUID()
  @IsOptional()
  doctorId?: string;

  @ApiPropertyOptional({ format: 'uuid' })
  @IsUUID()
  @IsOptional()
  medicalRecordId?: string;

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
