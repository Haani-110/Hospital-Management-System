import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsDate, IsOptional, IsString, IsUUID, MaxLength } from 'class-validator';
import { PaginationQueryDto } from '../../common/dto/pagination-query.dto.js';
import { TransformToDate } from '../../common/dto/transforms.js';

export class MedicalRecordsQueryDto extends PaginationQueryDto {
  @ApiPropertyOptional({ format: 'uuid' })
  @IsUUID()
  @IsOptional()
  patientId?: string;

  @ApiPropertyOptional({ format: 'uuid' })
  @IsUUID()
  @IsOptional()
  doctorId?: string;

  @ApiPropertyOptional({ description: 'Case-insensitive diagnosis search' })
  @IsString()
  @MaxLength(120)
  @IsOptional()
  search?: string;

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
