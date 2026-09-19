import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsBoolean, IsOptional, IsString, MaxLength } from 'class-validator';
import { PaginationQueryDto } from '../../common/dto/pagination-query.dto.js';
import { TransformToBoolean } from '../../common/dto/transforms.js';

export class PatientsQueryDto extends PaginationQueryDto {
  @ApiPropertyOptional()
  @TransformToBoolean()
  @IsBoolean()
  @IsOptional()
  active?: boolean;

  @ApiPropertyOptional({ description: 'Case-insensitive name / patient code search' })
  @IsString()
  @MaxLength(120)
  @IsOptional()
  search?: string;
}
