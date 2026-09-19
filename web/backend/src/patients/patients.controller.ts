import { Controller, Get, Param, ParseUUIDPipe, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { PatientsService } from './patients.service.js';
import { PatientsQueryDto } from './dto/patient-query.dto.js';
import { PatientResponseDto } from './dto/patient-response.dto.js';
import { Roles } from '../common/decorators/roles.decorator.js';
import { Role } from '../generated/prisma/client.js';

@ApiTags('patients')
@ApiBearerAuth()
@Controller('patients')
export class PatientsController {
  constructor(private readonly patientsService: PatientsService) {}

  @Get()
  @Roles(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'List patients with optional status/search filters' })
  findAll(@Query() query: PatientsQueryDto) {
    return this.patientsService.findAll(query);
  }

  @Get(':id')
  @Roles(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'Get a patient by id' })
  findOne(@Param('id', new ParseUUIDPipe()) id: string): Promise<PatientResponseDto> {
    return this.patientsService.findOne(id);
  }
}
