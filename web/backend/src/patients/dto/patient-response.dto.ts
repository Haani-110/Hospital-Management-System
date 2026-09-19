import { ApiProperty } from '@nestjs/swagger';
import { Gender } from '../../generated/prisma/client.js';

export class PatientResponseDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty({ example: 'PAT-000001' }) patientCode: string;
  @ApiProperty() fullName: string;
  @ApiProperty({ type: String, format: 'date' }) dateOfBirth: Date;
  @ApiProperty({ enum: Gender }) gender: Gender;
  @ApiProperty({ nullable: true }) phone: string | null;
  @ApiProperty({ nullable: true }) email: string | null;
  @ApiProperty({ nullable: true }) address: string | null;
  @ApiProperty({ nullable: true }) emergencyContactName: string | null;
  @ApiProperty({ nullable: true }) emergencyContactPhone: string | null;
  @ApiProperty({ nullable: true }) bloodGroup: string | null;
  @ApiProperty() active: boolean;
  @ApiProperty() createdAt: Date;
  @ApiProperty() updatedAt: Date;
}
