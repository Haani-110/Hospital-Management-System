import { ApiProperty } from '@nestjs/swagger';
import { Role } from '../../generated/prisma/client.js';

/** Public user representation — never contains the password hash. */
export class UserProfileDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty() username: string;
  @ApiProperty({ nullable: true }) email: string | null;
  @ApiProperty({ enum: Role }) role: Role;
  @ApiProperty() active: boolean;
  @ApiProperty() createdAt: Date;
  @ApiProperty() updatedAt: Date;
  @ApiProperty({ nullable: true }) lastLoginAt: Date | null;
}

export class LoginResponseDto {
  @ApiProperty() accessToken: string;
  @ApiProperty({ example: 'Bearer' }) tokenType: string;
  @ApiProperty({ example: '15m' }) expiresIn: string;
  @ApiProperty({ type: UserProfileDto }) user: UserProfileDto;
}
