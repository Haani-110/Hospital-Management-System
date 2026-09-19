import { Link } from 'react-router-dom';
import { ArrowRight, CircleDashed, Info } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Alert } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import {  } from '@/components/ui/button';

export interface PlannedModule {
  eyebrow: string;
  title: string;
  description: string;
  /** Workflows the module will own once its endpoints exist. */
  plannedCapabilities: string[];
  /** Records the existing API already provides that belong to this workflow. */
  availableToday: { label: string; to: string; description: string }[];
}

/**
 * Foundation screen for a module whose backend endpoints do not exist yet.
 *
 * It states plainly what is missing, links to the related data that *is* live,
 * and shows no placeholder records — an empty module stays honestly empty.
 */
export function PlannedModulePage({ module }: { module: PlannedModule }) {
  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow={module.eyebrow}
        title={module.title}
        description={module.description}
        actions={<Badge tone="neutral">Not yet connected</Badge>}
      />

      <Alert tone="info" title="This module has no backend API yet">
        {module.title} is included so the navigation reflects the full hospital workflow. Nothing on
        this screen is simulated — the screens below link to the records that are stored today.
      </Alert>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Planned capabilities</CardTitle>
              <CardDescription>
                What this module will do once the corresponding endpoints exist.
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            <ul className="space-y-3">
              {module.plannedCapabilities.map((capability) => (
                <li key={capability} className="flex items-start gap-3 text-[13px] text-muted">
                  <CircleDashed aria-hidden className="mt-0.5 size-4 shrink-0 text-subtle" />
                  <span>{capability}</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div>
              <CardTitle>Available today</CardTitle>
              <CardDescription>Live data in the current API related to this workflow.</CardDescription>
            </div>
          </CardHeader>
          <CardContent className="space-y-1">
            {module.availableToday.map((item, index) => (
              <div key={item.to}>
                {index > 0 ? <Separator className="my-1" /> : null}
                <Link
                  to={item.to}
                  className="group flex items-start justify-between gap-4 rounded-md px-2 py-3 transition-colors hover:bg-elevated/60"
                >
                  <span className="min-w-0">
                    <span className="block text-[13px] font-medium text-fg">{item.label}</span>
                    <span className="mt-0.5 block text-xs text-subtle">{item.description}</span>
                  </span>
                  <ArrowRight
                    aria-hidden
                    className="mt-1 size-4 shrink-0 text-subtle transition-transform group-hover:translate-x-0.5 group-hover:text-accent"
                  />
                </Link>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>

      <p className="flex items-center gap-2 text-xs text-subtle">
        <Info aria-hidden className="size-3.5" />
        Adding this module means adding endpoints first — the interface is deliberately not faking
        them.
      </p>
    </div>
  );
}
