import { Global, Module } from '@nestjs/common';
import { AppConfigService } from './app-config.service.js';

/**
 * Exposes the validated configuration through a typed, injectable service.
 * The underlying ConfigModule is registered globally in AppModule.
 */
@Global()
@Module({
  providers: [AppConfigService],
  exports: [AppConfigService],
})
export class AppConfigModule {}
