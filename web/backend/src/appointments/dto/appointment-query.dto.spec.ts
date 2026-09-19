import { describe, expect, it } from '@jest/globals';
import { plainToInstance } from 'class-transformer';
import { validate } from 'class-validator';
import { AppointmentsQueryDto } from './appointment-query.dto.js';
import { AppointmentStatus } from '../../generated/prisma/client.js';

async function validateQuery(query: Record<string, unknown>) {
  const dto = plainToInstance(AppointmentsQueryDto, query, { enableImplicitConversion: true });
  return { dto, errors: await validate(dto) };
}

describe('AppointmentsQueryDto validation', () => {
  it('applies default pagination when nothing is supplied', async () => {
    const { dto, errors } = await validateQuery({});

    expect(errors).toHaveLength(0);
    expect(dto.page).toBe(1);
    expect(dto.limit).toBe(20);
    expect(dto.skip).toBe(0);
  });

  it('computes the skip offset from page and limit', async () => {
    const { dto } = await validateQuery({ page: 4, limit: 15 });

    expect(dto.skip).toBe(45);
  });

  it('converts ISO date strings into Date instances', async () => {
    const { dto, errors } = await validateQuery({ from: '2026-03-01', to: '2026-04-01' });

    expect(errors).toHaveLength(0);
    expect(dto.from).toBeInstanceOf(Date);
    expect(dto.to).toBeInstanceOf(Date);
  });

  it('rejects an invalid date', async () => {
    const { errors } = await validateQuery({ from: 'not-a-date' });

    expect(errors.some((error) => error.property === 'from')).toBe(true);
  });

  it('rejects an unknown status value', async () => {
    const { errors } = await validateQuery({ status: 'PENDING' });

    expect(errors.some((error) => error.property === 'status')).toBe(true);
  });

  it('accepts a valid status and rejects a malformed UUID', async () => {
    const valid = await validateQuery({ status: AppointmentStatus.SCHEDULED });
    expect(valid.errors).toHaveLength(0);

    const invalid = await validateQuery({ patientId: 'not-a-uuid' });
    expect(invalid.errors.some((error) => error.property === 'patientId')).toBe(true);
  });

  it('rejects a page size above the maximum', async () => {
    const { errors } = await validateQuery({ limit: 500 });

    expect(errors.some((error) => error.property === 'limit')).toBe(true);
  });
});
