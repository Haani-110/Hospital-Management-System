import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';

export interface DateRangeValue {
  /** `YYYY-MM-DD`, inclusive. */
  from: string;
  /** `YYYY-MM-DD`, inclusive for the user (sent to the API as an exclusive bound). */
  to: string;
}

interface DateRangeFilterProps {
  value: DateRangeValue;
  onChange: (next: DateRangeValue) => void;
  /** Id prefix so the two inputs keep unique, labelled ids per screen. */
  idPrefix: string;
  className?: string;
}

/**
 * Two-date range filter.
 *
 * The backend treats `to` as an exclusive upper bound, so the screens convert
 * the chosen end date into the start of the following day before requesting —
 * selecting 12 March therefore includes all of 12 March.
 */
export function DateRangeFilter({ value, onChange, idPrefix, className }: DateRangeFilterProps) {
  const hasValue = Boolean(value.from || value.to);

  return (
    <div className={className}>
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-2">
          <label htmlFor={`${idPrefix}-from`} className="text-[13px] text-muted">
            From
          </label>
          <Input
            id={`${idPrefix}-from`}
            type="date"
            className="w-40"
            max={value.to || undefined}
            value={value.from}
            onChange={(event) => onChange({ ...value, from: event.target.value })}
          />
        </div>
        <div className="flex items-center gap-2">
          <label htmlFor={`${idPrefix}-to`} className="text-[13px] text-muted">
            To
          </label>
          <Input
            id={`${idPrefix}-to`}
            type="date"
            className="w-40"
            min={value.from || undefined}
            value={value.to}
            onChange={(event) => onChange({ ...value, to: event.target.value })}
          />
        </div>
        {hasValue ? (
          <Button variant="ghost" size="sm" onClick={() => onChange({ from: '', to: '' })}>
            Clear dates
          </Button>
        ) : null}
      </div>
    </div>
  );
}
