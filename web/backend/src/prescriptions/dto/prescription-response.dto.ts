import { ApiProperty } from '@nestjs/swagger';

export class PrescriptionItemResponseDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty() medicineName: string;
  @ApiProperty({ nullable: true }) dosage: string | null;
  @ApiProperty({ nullable: true }) frequency: string | null;
  @ApiProperty({ nullable: true }) duration: string | null;
  @ApiProperty({ nullable: true }) instructions: string | null;
}

export class PrescriptionPatientRefDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty() patientCode: string;
  @ApiProperty() fullName: string;
}

export class PrescriptionDoctorRefDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty() fullName: string;
}

export class PrescriptionResponseDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty({ format: 'uuid' }) medicalRecordId: string;
  @ApiProperty({ format: 'uuid' }) patientId: string;
  @ApiProperty({ format: 'uuid' }) doctorId: string;
  @ApiProperty({ type: String, format: 'date' }) prescriptionDate: Date;
  @ApiProperty({ nullable: true }) notes: string | null;
  @ApiProperty({ type: PrescriptionPatientRefDto, nullable: true })
  patient?: PrescriptionPatientRefDto;
  @ApiProperty({ type: PrescriptionDoctorRefDto, nullable: true })
  doctor?: PrescriptionDoctorRefDto;
  @ApiProperty({ type: [PrescriptionItemResponseDto] }) items: PrescriptionItemResponseDto[];
  @ApiProperty() createdAt: Date;
  @ApiProperty() updatedAt: Date;
}
