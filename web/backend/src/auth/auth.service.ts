import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../prisma/prisma.service.js';
import { AppConfigService } from '../config/app-config.service.js';
import { getDummyPasswordHash, verifyPassword } from '../common/security/password.util.js';
import { JwtPayload } from '../common/types/authenticated-user.type.js';
import { LoginDto } from './dto/login.dto.js';
import { LoginResponseDto, UserProfileDto } from './dto/auth-response.dto.js';

/**
 * Authentication: credential verification and JWT issuance.
 *
 * Failure responses are deliberately identical for unknown usernames, wrong
 * passwords and inactive accounts so the API does not reveal which usernames
 * exist or which accounts are disabled.
 */
@Injectable()
export class AuthService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwtService: JwtService,
    private readonly config: AppConfigService,
  ) {}

  async login(dto: LoginDto): Promise<LoginResponseDto> {
    const user = await this.prisma.user.findUnique({
      where: { username: dto.username },
    });

    if (!user) {
      // Perform equivalent work so response timing does not leak account existence.
      await verifyPassword(dto.password, await getDummyPasswordHash(this.config.bcryptRounds));
      throw new UnauthorizedException('Invalid username or password');
    }

    const passwordMatches = await verifyPassword(dto.password, user.passwordHash);
    if (!passwordMatches || !user.active) {
      throw new UnauthorizedException('Invalid username or password');
    }

    const payload: JwtPayload = {
      sub: user.id,
      username: user.username,
      role: user.role,
    };

    const accessToken = await this.jwtService.signAsync(payload);

    const updated = await this.prisma.user.update({
      where: { id: user.id },
      data: { lastLoginAt: new Date() },
    });

    return {
      accessToken,
      tokenType: 'Bearer',
      expiresIn: this.config.jwtExpiresIn,
      user: toUserProfile(updated),
    };
  }

  /** Returns the profile of the authenticated user (from the JWT subject). */
  async getProfile(userId: string): Promise<UserProfileDto> {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user || !user.active) {
      throw new UnauthorizedException('Account is not available');
    }
    return toUserProfile(user);
  }
}

export function toUserProfile(user: {
  id: string;
  username: string;
  email: string | null;
  role: UserProfileDto['role'];
  active: boolean;
  createdAt: Date;
  updatedAt: Date;
  lastLoginAt: Date | null;
}): UserProfileDto {
  return {
    id: user.id,
    username: user.username,
    email: user.email,
    role: user.role,
    active: user.active,
    createdAt: user.createdAt,
    updatedAt: user.updatedAt,
    lastLoginAt: user.lastLoginAt,
  };
}
