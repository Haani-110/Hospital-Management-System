import { Controller, Get, Param, ParseUUIDPipe, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { MedicalRecordsService } from './medical-records.service.js';
import { MedicalRecordsQueryDto } from './dto/medical-record-query.dto.js';
import { MedicalRecordResponseDto } from './dto/medical-record-response.dto.js';
import { Roles } from '../common/decorators/roles.decorator.js';
import { Role } from '../generated/prisma/client.js';

@ApiTags('medical-records')
@ApiBearerAuth()
@Controller('medical-records')
export class MedicalRecordsController {
  constructor(private readonly medicalRecordsService: MedicalRecordsService) {}

  @Get()
  @Roles(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'List medical records' })
  findAll(@Query() query: MedicalRecordsQueryDto) {
    return this.medicalRecordsService.findAll(query);
  }

  @Get(':id')
  @Roles(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'Get a medical record by id' })
  findOne(@Param('id', new ParseUUIDPipe()) id: string): Promise<MedicalRecordResponseDto> {
    return this.medicalRecordsService.findOne(id);
  }
}
