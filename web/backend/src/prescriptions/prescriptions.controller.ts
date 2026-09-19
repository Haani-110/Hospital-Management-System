import { Controller, Get, Param, ParseUUIDPipe, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { PrescriptionsService } from './prescriptions.service.js';
import { PrescriptionsQueryDto } from './dto/prescription-query.dto.js';
import { PrescriptionResponseDto } from './dto/prescription-response.dto.js';
import { Roles } from '../common/decorators/roles.decorator.js';
import { Role } from '../generated/prisma/client.js';

@ApiTags('prescriptions')
@ApiBearerAuth()
@Controller('prescriptions')
export class PrescriptionsController {
  constructor(private readonly prescriptionsService: PrescriptionsService) {}

  @Get()
  @Roles(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'List prescriptions with their medicine items' })
  findAll(@Query() query: PrescriptionsQueryDto) {
    return this.prescriptionsService.findAll(query);
  }

  @Get(':id')
  @Roles(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'Get a prescription by id' })
  findOne(@Param('id', new ParseUUIDPipe()) id: string): Promise<PrescriptionResponseDto> {
    return this.prescriptionsService.findOne(id);
  }
}
