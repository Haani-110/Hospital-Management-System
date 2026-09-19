import { Controller, Get, Param, ParseUUIDPipe, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { BillingService } from './billing.service.js';
import { BillsQueryDto } from './dto/bill-query.dto.js';
import { BillResponseDto } from './dto/bill-response.dto.js';
import { Roles } from '../common/decorators/roles.decorator.js';
import { Role } from '../generated/prisma/client.js';

@ApiTags('bills')
@ApiBearerAuth()
@Controller('bills')
export class BillingController {
  constructor(private readonly billingService: BillingService) {}

  @Get()
  // Doctors have no billing access, matching the v1.0 role matrix.
  @Roles(Role.ADMIN, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'List bills with line items (Admin and Receptionist)' })
  findAll(@Query() query: BillsQueryDto) {
    return this.billingService.findAll(query);
  }

  @Get(':id')
  @Roles(Role.ADMIN, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'Get a bill by id (Admin and Receptionist)' })
  findOne(@Param('id', new ParseUUIDPipe()) id: string): Promise<BillResponseDto> {
    return this.billingService.findOne(id);
  }
}
