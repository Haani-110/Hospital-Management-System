import 'dotenv/config';
import { PrismaPg } from '@prisma/adapter-pg';
import { PrismaClient, Role } from '../src/generated/prisma/client.js';
import { generateRandomPassword, hashPassword } from '../src/common/security/password.util.js';

/**
 * Development seed.
 *
 * Safety rules:
 *   * No real credentials live in this file or in source control.
 *   * Passwords come from the environment (SEED_ADMIN_PASSWORD, ...).
 *   * If a password variable is empty, a random one is generated and printed
 *     once, so local development never depends on a committed default.
 *   * Demo clinical data is only created when SEED_DEMO_DATA=true.
 */

const connectionString = process.env.DATABASE_URL;
if (!connectionString) {
  throw new Error('DATABASE_URL is required to run the seed script');
}

const prisma = new PrismaClient({ adapter: new PrismaPg({ connectionString }) });

const parsedRounds = Number(process.env.BCRYPT_ROUNDS ?? 12);
const bcryptRounds =
  Number.isInteger(parsedRounds) && parsedRounds >= 10 && parsedRounds <= 14 ? parsedRounds : 12;

type SeedAccount = {
  username: string;
  role: Role;
  envVar: string;
  password: string;
  generated: boolean;
};

function resolvePassword(envVar: string): { password: string; generated: boolean } {
  const value = process.env[envVar];
  if (value && value.trim().length >= 8) {
    return { password: value, generated: false };
  }
  return { password: generateRandomPassword(), generated: true };
}

async function upsertUser(account: SeedAccount): Promise<void> {
  const passwordHash = await hashPassword(account.password, bcryptRounds);

  await prisma.user.upsert({
    where: { username: account.username },
    update: { role: account.role, active: true },
    create: {
      username: account.username,
      role: account.role,
      passwordHash,
      active: true,
    },
  });

  if (account.generated) {
    console.log(
      `  [generated] ${account.role.padEnd(12)} username: ${account.username.padEnd(14)} password: ${account.password}`,
    );
  } else {
    console.log(`  [from env]  ${account.role.padEnd(12)} username: ${account.username}`);
  }
}

async function seedDemoData(): Promise<void> {
  const department = await prisma.department.upsert({
    where: { name: 'Cardiology' },
    update: {},
    create: { name: 'Cardiology', description: 'Heart and vascular care' },
  });

  await prisma.department.upsert({
    where: { name: 'General Medicine' },
    update: {},
    create: { name: 'General Medicine', description: 'General consultations' },
  });

  const doctorUser = await prisma.user.findUnique({ where: { username: 'doctor' } });

  // Doctors have no natural unique key other than the generated id, so the
  // demo profile is matched on its name + department to stay idempotent.
  const existingDoctor = await prisma.doctor.findFirst({
    where: { fullName: 'Dr. Demo Cardiologist', departmentId: department.id },
  });

  const doctor =
    existingDoctor ??
    (await prisma.doctor.create({
      data: {
        fullName: 'Dr. Demo Cardiologist',
        specialization: 'Cardiology',
        departmentId: department.id,
        consultationFee: '2500.00',
        userId: doctorUser?.id ?? null,
        active: true,
      },
    }));

  const patient = await prisma.patient.upsert({
    where: { patientCode: 'PAT-000001' },
    update: {},
    create: {
      patientCode: 'PAT-000001',
      fullName: 'Demo Patient',
      dateOfBirth: new Date('1990-05-20'),
      gender: 'OTHER',
      phone: '+92-300-0000000',
      bloodGroup: 'O+',
      active: true,
    },
  });

  console.log(
    `  demo data ready — department: ${department.name}, doctor: ${doctor.fullName}, patient: ${patient.patientCode}`,
  );
}

async function main(): Promise<void> {
  console.log('Seeding development accounts (no real credentials are stored in the repository):');

  const accounts: SeedAccount[] = [
    {
      username: process.env.SEED_ADMIN_USERNAME?.trim() || 'admin',
      role: Role.ADMIN,
      envVar: 'SEED_ADMIN_PASSWORD',
      ...resolvePassword('SEED_ADMIN_PASSWORD'),
    },
  ];

  if (process.env.SEED_DEMO_DATA === 'true') {
    accounts.push(
      {
        username: 'doctor',
        role: Role.DOCTOR,
        envVar: 'SEED_DOCTOR_PASSWORD',
        ...resolvePassword('SEED_DOCTOR_PASSWORD'),
      },
      {
        username: 'receptionist',
        role: Role.RECEPTIONIST,
        envVar: 'SEED_RECEPTIONIST_PASSWORD',
        ...resolvePassword('SEED_RECEPTIONIST_PASSWORD'),
      },
    );
  }

  for (const account of accounts) {
    await upsertUser(account);
  }

  if (process.env.SEED_DEMO_DATA === 'true') {
    await seedDemoData();
  }

  console.log('Seed complete. Change any generated passwords before sharing an environment.');
}

main()
  .catch((error: unknown) => {
    console.error('Seed failed:', error instanceof Error ? error.message : error);
    process.exitCode = 1;
  })
  .finally(() => {
    void prisma.$disconnect();
  });
