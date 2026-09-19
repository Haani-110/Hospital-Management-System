import { Controller, Get, Param, ParseUUIDPipe, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { AppointmentsService } from './appointments.service.js';
import { AppointmentsQueryDto } from './dto/appointment-query.dto.js';
import { AppointmentResponseDto } from './dto/appointment-response.dto.js';
import { Roles } from '../common/decorators/roles.decorator.js';
import { Role } from '../generated/prisma/client.js';

@ApiTags('appointments')
@ApiBearerAuth()
@Controller('appointments')
export class AppointmentsController {
  constructor(private readonly appointmentsService: AppointmentsService) {}

  @Get()
  @Roles(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'List appointments (filter by patient, doctor, status, date range)' })
  findAll(@Query() query: AppointmentsQueryDto) {
    return this.appointmentsService.findAll(query);
  }

  @Get(':id')
  @Roles(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'Get an appointment by id' })
  findOne(@Param('id', new ParseUUIDPipe()) id: string): Promise<AppointmentResponseDto> {
    return this.appointmentsService.findOne(id);
  }
}
