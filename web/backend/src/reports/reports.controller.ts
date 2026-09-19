import { Controller, Get } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { ReportsService } from './reports.service.js';
import { DashboardSummaryDto } from './dto/dashboard-summary.dto.js';
import { Roles } from '../common/decorators/roles.decorator.js';
import { Role } from '../generated/prisma/client.js';

@ApiTags('reports')
@ApiBearerAuth()
@Controller('reports')
export class ReportsController {
  constructor(private readonly reportsService: ReportsService) {}

  @Get('dashboard-summary')
  @Roles(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST)
  @ApiOperation({ summary: 'Dashboard counters (all authenticated roles)' })
  @ApiResponse({ status: 200, type: DashboardSummaryDto })
  getDashboardSummary(): Promise<DashboardSummaryDto> {
    return this.reportsService.getDashboardSummary();
  }
}
