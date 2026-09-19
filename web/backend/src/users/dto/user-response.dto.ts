import { ApiProperty } from '@nestjs/swagger';
import { Role } from '../../generated/prisma/client.js';

/** User representation for admin endpoints — password hash is never selected. */
export class UserResponseDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty() username: string;
  @ApiProperty({ nullable: true }) email: string | null;
  @ApiProperty({ enum: Role }) role: Role;
  @ApiProperty() active: boolean;
  @ApiProperty({ nullable: true }) lastLoginAt: Date | null;
  @ApiProperty() createdAt: Date;
  @ApiProperty() updatedAt: Date;
}
