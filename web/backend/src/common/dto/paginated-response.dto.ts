import { ApiProperty } from '@nestjs/swagger';

export class PaginationMetaDto {
  @ApiProperty() page: number;
  @ApiProperty() limit: number;
  @ApiProperty() total: number;
  @ApiProperty() totalPages: number;

  static from(page: number, limit: number, total: number): PaginationMetaDto {
    return {
      page,
      limit,
      total,
      totalPages: limit > 0 ? Math.ceil(total / limit) : 0,
    };
  }
}

/** Envelope used by every list endpoint: `{ data, meta }`. */
export class PaginatedResponseDto<T> {
  @ApiProperty({ isArray: true })
  data: T[];

  @ApiProperty({ type: PaginationMetaDto })
  meta: PaginationMetaDto;

  static of<T>(data: T[], page: number, limit: number, total: number): PaginatedResponseDto<T> {
    return { data, meta: PaginationMetaDto.from(page, limit, total) };
  }
}
