import { ApiProperty } from '@nestjs/swagger';

export class DoctorDepartmentDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty() name: string;
}

export class DoctorResponseDto {
  @ApiProperty({ format: 'uuid' }) id: string;
  @ApiProperty({ format: 'uuid', nullable: true }) userId: string | null;
  @ApiProperty({ format: 'uuid' }) departmentId: string;
  @ApiProperty() fullName: string;
  @ApiProperty({ nullable: true }) specialization: string | null;
  @ApiProperty({ nullable: true }) phone: string | null;
  @ApiProperty({ nullable: true }) email: string | null;
  @ApiProperty({ description: 'Consultation fee as a decimal string' }) consultationFee: string;
  @ApiProperty() active: boolean;
  @ApiProperty({ type: DoctorDepartmentDto, nullable: true }) department?: DoctorDepartmentDto;
  @ApiProperty() createdAt: Date;
  @ApiProperty() updatedAt: Date;
}
