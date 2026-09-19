import { ApiProperty } from '@nestjs/swagger';

export class MedicalRecordPatientRefDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty() patientCode: string;
  @ApiProperty() fullName: string;
}

export class MedicalRecordDoctorRefDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty() fullName: string;
}

export class MedicalRecordResponseDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty({ format: 'uuid' }) appointmentId: string;
  @ApiProperty({ format: 'uuid' }) patientId: string;
  @ApiProperty({ format: 'uuid' }) doctorId: string;
  @ApiProperty() diagnosis: string;
  @ApiProperty({ nullable: true }) symptoms: string | null;
  @ApiProperty({ nullable: true }) examination: string | null;
  @ApiProperty({ nullable: true }) treatmentNotes: string | null;
  @ApiProperty({ type: String, format: 'date' }) recordDate: Date;
  @ApiProperty({ type: MedicalRecordPatientRefDto, nullable: true })
  patient?: MedicalRecordPatientRefDto;
  @ApiProperty({ type: MedicalRecordDoctorRefDto, nullable: true })
  doctor?: MedicalRecordDoctorRefDto;
  @ApiProperty() createdAt: Date;
  @ApiProperty() updatedAt: Date;
}
