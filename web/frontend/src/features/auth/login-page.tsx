import { Alert } from '@/components/ui/alert';
import { Logo } from '@/components/layout/logo';
import { ThemeToggle } from '@/components/layout/sidebar';
import { LoginForm } from './login-form';

/** Public sign-in screen (split layout on large viewports). */
export function LoginPage() {
  return (
    <div className="flex min-h-dvh bg-bg">
      {/* Left: form */}
      <div className="flex w-full flex-col lg:w-[46%] lg:min-w-[26rem]">
        <header className="flex h-16 items-center justify-between border-b border-border px-5 lg:border-b-0">
          <Logo />
          <ThemeToggle />
        </header>

        <div className="flex flex-1 items-center justify-center px-5 py-10 sm:px-8">
          <div className="animate-fade-up w-full max-w-sm">
            <h1 className="text-2xl font-semibold tracking-tight text-fg">Sign in</h1>
            <p className="mt-2 text-sm leading-relaxed text-muted">
              Use your hospital account to access clinical, operational and financial records.
            </p>

            <div className="mt-8">
              <LoginForm />
            </div>

            <Alert tone="info" className="mt-8">
              Accounts are created by an administrator with the backend seed or user administration —
              there is no public sign-up.
            </Alert>
          </div>
        </div>
      </div>

      {/* Right: context panel (decorative, no data) */}
      <aside className="relative hidden flex-1 border-l border-border bg-surface lg:block">
        <div aria-hidden className="surface-grid absolute inset-0 opacity-60" />
        <div className="relative flex h-full flex-col justify-between p-12">
          <p className="text-[11px] font-semibold tracking-[0.14em] text-subtle uppercase">
            Hospital Management System
          </p>

          <div className="max-w-md">
            <h2 className="text-3xl leading-tight font-semibold tracking-tight text-fg">
              One workspace for patient care and hospital operations.
            </h2>
            <p className="mt-4 text-sm leading-relaxed text-muted">
              Patient records, appointments, prescriptions and billing are served from a single
              source of truth — the records you see here are the live database, not a demo.
            </p>
          </div>

          <dl className="grid grid-cols-3 gap-6 border-t border-border pt-6">
            {[
              { label: 'Patients', to: 'Registry & history' },
              { label: 'Clinical', to: 'Visits & prescriptions' },
              { label: 'Operations', to: 'Billing & reporting' },
            ].map((item) => (
              <div key={item.label}>
                <dt className="text-[13px] font-medium text-fg">{item.label}</dt>
                <dd className="mt-1 text-xs leading-relaxed text-subtle">{item.to}</dd>
              </div>
            ))}
          </dl>
        </div>
      </aside>
    </div>
  );
}
