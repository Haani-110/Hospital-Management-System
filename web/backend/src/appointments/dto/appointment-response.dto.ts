import { ApiProperty } from '@nestjs/swagger';
import { AppointmentStatus } from '../../generated/prisma/client.js';

export class AppointmentPatientRefDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty() patientCode: string;
  @ApiProperty() fullName: string;
}

export class AppointmentDoctorRefDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty() fullName: string;
  @ApiProperty({ nullable: true }) specialization: string | null;
}

export class AppointmentResponseDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty({ format: 'uuid' }) patientId: string;
  @ApiProperty({ format: 'uuid' }) doctorId: string;
  @ApiProperty() scheduledAt: Date;
  @ApiProperty({ enum: AppointmentStatus }) status: AppointmentStatus;
  @ApiProperty({ nullable: true }) reason: string | null;
  @ApiProperty({ nullable: true }) notes: string | null;
  @ApiProperty({ type: AppointmentPatientRefDto, nullable: true })
  patient?: AppointmentPatientRefDto;
  @ApiProperty({ type: AppointmentDoctorRefDto, nullable: true }) doctor?: AppointmentDoctorRefDto;
  @ApiProperty() createdAt: Date;
  @ApiProperty() updatedAt: Date;
}
