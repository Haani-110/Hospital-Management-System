import { Controller, Get, Param, ParseUUIDPipe, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { DoctorsService } from './doctors.service.js';
import { DoctorsQueryDto } from './dto/doctor-query.dto.js';
import { DoctorResponseDto } from './dto/doctor-response.dto.js';
import { Roles } from '../common/decorators/roles.decorator.js';
import { Role } from '../generated/prisma/client.js';

@ApiTags('doctors')
@ApiBearerAuth()
@Controller('doctors')
export class DoctorsController {
  constructor(private readonly doctorsService: DoctorsService) {}

  @Get()
  @Roles(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'List doctors with optional department/status filters' })
  findAll(@Query() query: DoctorsQueryDto) {
    return this.doctorsService.findAll(query);
  }

  @Get(':id')
  @Roles(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'Get a doctor by id' })
  findOne(@Param('id', new ParseUUIDPipe()) id: string): Promise<DoctorResponseDto> {
    return this.doctorsService.findOne(id);
  }
}
