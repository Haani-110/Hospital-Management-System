import { useCallback, useEffect, useId, useRef, useState } from 'react';
import { Check, ChevronsUpDown, Loader2, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { inputStyles } from '@/components/ui/input';
import { useDebouncedValue } from '@/lib/hooks/use-debounced-value';
import { doctorsApi, appointmentsApi } from '@/lib/api';
import { formatDateTime } from '@/lib/utils';
import type { Appointment, Doctor } from '@/lib/api/types';

export interface Option {
  id: string;
  primary: string;
  secondary?: string | null;
}

interface ComboboxProps {
  value: string;
  onChange: (value: string) => void;
  label: string;
  placeholder: string;
  emptyMessage: string;
  /** Resolves the option list for the current query. */
  loadOptions: (query: string, signal: AbortSignal) => Promise<Option[]>;
  /** Loads the label for a pre-selected id (e.g. from the URL). */
  loadSelected?: (id: string, signal: AbortSignal) => Promise<Option | null>;
  className?: string;
}

/**
 * Generic ARIA combobox over an async option list.
 * `PatientPicker` is the specialised variant; this covers doctors, staff and
 * appointments so every filter behaves the same way.
 */
export function AsyncCombobox({
  value,
  onChange,
  label,
  placeholder,
  emptyMessage,
  loadOptions,
  loadSelected,
  className,
}: ComboboxProps) {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [options, setOptions] = useState<Option[]>([]);
  const [activeIndex, setActiveIndex] = useState(0);
  /** Key of the option list currently on screen; used to derive `isLoading`. */
  const [loadedKey, setLoadedKey] = useState<string | null>(null);
  /** Label resolved for a pre-selected id that is not in the current list. */
  const [resolved, setResolved] = useState<{ id: string; label: string } | null>(null);

  const debouncedQuery = useDebouncedValue(query, 300);
  const containerRef = useRef<HTMLDivElement>(null);
  const listboxId = useId();
  const inputId = useId();

  const requestKey = `${open ? 'open' : 'closed'}:${debouncedQuery}`;
  useEffect(() => {
    if (!open) return;
    const controller = new AbortController();
    const key = `open:${debouncedQuery}`;
    loadOptions(debouncedQuery, controller.signal)
      .then((result) => {
        setOptions(result);
        setActiveIndex(0);
        setLoadedKey(key);
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setOptions([]);
          setLoadedKey(key);
        }
      });
    return () => controller.abort();
  }, [debouncedQuery, open, loadOptions]);

  const isLoading = open && loadedKey !== requestKey;

  // Label for the closed control: the current list first, then one lookup by id.
  const optionLabel = options.find((option) => option.id === value);
  const selectedLabel = optionLabel
    ? optionLabel.primary
    : resolved && resolved.id === value
      ? resolved.label
      : null;

  useEffect(() => {
    if (!value || !loadSelected || options.some((option) => option.id === value)) return;
    if (resolved?.id === value) return;
    const controller = new AbortController();
    loadSelected(value, controller.signal)
      .then((option) => {
        if (option) setResolved({ id: value, label: option.primary });
      })
      .catch(() => {
        // Unknown or stale id: no label is shown.
      });
    return () => controller.abort();
  }, [value, options, resolved, loadSelected]);

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onPointerDown);
    return () => document.removeEventListener('mousedown', onPointerDown);
  }, [open]);

  const commit = useCallback(
    (option: Option) => {
      onChange(option.id);
      setResolved({ id: option.id, label: option.primary });
      setQuery('');
      setOpen(false);
    },
    [onChange],
  );

  const onKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Escape') return setOpen(false);
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (!open) return setOpen(true);
      if (options.length === 0) return;
      setActiveIndex((index) =>
        event.key === 'ArrowDown'
          ? (index + 1) % options.length
          : (index - 1 + options.length) % options.length,
      );
      return;
    }
    if (event.key === 'Enter' && open) {
      const option = options[activeIndex];
      if (option) {
        event.preventDefault();
        commit(option);
      }
    }
  };

  return (
    <div ref={containerRef} className={cn('relative', className)}>
      <label htmlFor={inputId} className="sr-only">
        {label}
      </label>
      <div className="relative">
        <input
          id={inputId}
          type="text"
          role="combobox"
          aria-expanded={open}
          aria-controls={open ? listboxId : undefined}
          aria-autocomplete="list"
          aria-activedescendant={open && options[activeIndex] ? `${listboxId}-${activeIndex}` : undefined}
          aria-label={label}
          autoComplete="off"
          className={cn(inputStyles, 'pr-9')}
          placeholder={value && selectedLabel && !open ? selectedLabel : placeholder}
          value={query}
          onChange={(event) => {
            setQuery(event.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={onKeyDown}
        />
        <div className="absolute inset-y-0 right-0 flex items-center pr-2.5">
          {isLoading ? (
            <Loader2 aria-hidden className="size-3.5 animate-spin text-subtle" />
          ) : value ? (
            <button
              type="button"
              aria-label={`Clear ${label.toLowerCase()}`}
              onClick={() => {
                onChange('');
                setQuery('');
                setResolved(null);
              }}
              className="rounded p-0.5 text-subtle hover:text-fg"
            >
              <X aria-hidden className="size-3.5" />
            </button>
          ) : (
            <ChevronsUpDown aria-hidden className="size-3.5 text-subtle" />
          )}
        </div>
      </div>

      {open ? (
        <ul
          id={listboxId}
          role="listbox"
          aria-label={label}
          className="absolute z-40 mt-1.5 max-h-64 w-full overflow-y-auto rounded-md border border-border bg-elevated p-1 shadow-lg"
        >
          {options.length === 0 && !isLoading ? (
            <li className="px-3 py-2.5 text-[13px] text-muted">{emptyMessage}</li>
          ) : null}
          {options.map((option, index) => (
            <li
              key={option.id}
              id={`${listboxId}-${index}`}
              role="option"
              aria-selected={option.id === value}
              onMouseEnter={() => setActiveIndex(index)}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => commit(option)}
              className={cn(
                'flex cursor-pointer items-center justify-between gap-3 rounded-sm px-3 py-2 text-[13px]',
                index === activeIndex ? 'bg-surface text-fg' : 'text-muted',
              )}
            >
              <span className="min-w-0">
                <span className="block truncate text-fg">{option.primary}</span>
                {option.secondary ? (
                  <span className="block text-xs text-subtle">{option.secondary}</span>
                ) : null}
              </span>
              {option.id === value ? <Check aria-hidden className="size-3.5 text-accent" /> : null}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

function doctorToOption(doctor: Doctor): Option {
  return {
    id: doctor.id,
    primary: doctor.fullName,
    secondary: doctor.specialization,
  };
}

function appointmentToOption(appointment: Appointment): Option {
  return {
    id: appointment.id,
    primary: `${formatDateTime(appointment.scheduledAt)} · ${appointment.patient?.fullName ?? 'Unknown patient'}`,
    secondary: appointment.doctor?.fullName ?? null,
  };
}

/** Doctor filter backed by `GET /doctors?search=`. */
export function DoctorPicker({
  value,
  onChange,
  className,
  label = 'Filter by doctor',
}: {
  value: string;
  onChange: (value: string) => void;
  className?: string;
  label?: string;
}) {
  const loadOptions = useCallback(async (query: string, signal: AbortSignal) => {
    const response = await doctorsApi.listDoctors(
      { search: query || undefined, limit: 8, page: 1 },
      signal,
    );
    return response.data.map(doctorToOption);
  }, []);

  const loadSelected = useCallback(async (id: string, signal: AbortSignal) => {
    const doctor = await doctorsApi.getDoctor(id, signal);
    return doctorToOption(doctor);
  }, []);

  return (
    <AsyncCombobox
      value={value}
      onChange={onChange}
      label={label}
      placeholder="Any doctor"
      emptyMessage="No doctors match that search."
      loadOptions={loadOptions}
      loadSelected={loadSelected}
      className={className}
    />
  );
}

/**
 * Appointment filter.
 *
 * `GET /appointments` implements no free-text search, so this picker loads the
 * most recent appointments once and filters that list in the browser. It is
 * therefore labelled as "recent appointments" — it never pretends to search the
 * whole table, and an older visit is reachable through the billing list's own
 * date filters.
 */
export function AppointmentPicker({
  value,
  onChange,
  className,
  label = 'Filter by appointment',
}: {
  value: string;
  onChange: (value: string) => void;
  className?: string;
  label?: string;
}) {
  const loadOptions = useCallback(async (query: string, signal: AbortSignal) => {
    const response = await appointmentsApi.listAppointments({ limit: 25, page: 1 }, signal);
    const needle = query.trim().toLowerCase();
    return response.data
      .map(appointmentToOption)
      .filter(
        (option) =>
          !needle ||
          `${option.primary} ${option.secondary ?? ''}`.toLowerCase().includes(needle),
      );
  }, []);

  const loadSelected = useCallback(async (id: string, signal: AbortSignal) => {
    const appointment = await appointmentsApi.getAppointment(id, signal);
    return appointmentToOption(appointment);
  }, []);

  return (
    <AsyncCombobox
      value={value}
      onChange={onChange}
      label={label}
      placeholder="Recent appointments"
      emptyMessage="No recent appointments match."
      loadOptions={loadOptions}
      loadSelected={loadSelected}
      className={className}
    />
  );
}
