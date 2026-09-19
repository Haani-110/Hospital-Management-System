import { useCallback, useEffect, useId, useRef, useState } from 'react';
import { Check, ChevronsUpDown, Loader2, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { inputStyles } from '@/components/ui/input';
import { useDebouncedValue } from '@/lib/hooks/use-debounced-value';
import { patientsApi } from '@/lib/api';
import type { Patient } from '@/lib/api/types';

interface PatientPickerProps {
  /** Selected patient id, or empty string for "any patient". */
  value: string;
  onChange: (value: string) => void;
  label: string;
  placeholder?: string;
  /** Id of the filter group this belongs to, for `aria-describedby`. */
  className?: string;
}

/**
 * Searchable patient selector backed by `GET /patients?search=`.
 *
 * Implemented as an ARIA combobox over a listbox: Arrow keys move the active
 * option, Enter selects, Escape closes. No patient data is invented — the
 * options are whatever the API returns.
 */
export function PatientPicker({
  value,
  onChange,
  label,
  placeholder = 'Any patient',
  className,
}: PatientPickerProps) {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [options, setOptions] = useState<Patient[]>([]);
  const [activeIndex, setActiveIndex] = useState(0);
  /** Key of the option list currently on screen; used to derive `isLoading`. */
  const [loadedKey, setLoadedKey] = useState<string | null>(null);
  /** Label resolved for a pre-selected id that is not in the current list. */
  const [resolved, setResolved] = useState<{ id: string; label: string } | null>(null);

  const debouncedQuery = useDebouncedValue(query, 300);
  const containerRef = useRef<HTMLDivElement>(null);
  const listboxId = useId();
  const inputId = useId();

  // Load candidates whenever the query changes and the popup is open.
  const requestKey = `${open ? 'open' : 'closed'}:${debouncedQuery}`;
  useEffect(() => {
    if (!open) return;
    const controller = new AbortController();
    const key = `open:${debouncedQuery}`;

    patientsApi
      .listPatients({ search: debouncedQuery || undefined, limit: 8, page: 1 }, controller.signal)
      .then((response) => {
        setOptions(response.data);
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
  }, [debouncedQuery, open]);

  const isLoading = open && loadedKey !== requestKey;

  // Resolve the selected patient's label so the closed control is readable.
  // Preferred source is the current option list; otherwise one lookup by id.
  const optionLabel = options.find((option) => option.id === value);
  const selectedLabel = optionLabel
    ? `${optionLabel.fullName} · ${optionLabel.patientCode}`
    : resolved && resolved.id === value
      ? resolved.label
      : null;

  useEffect(() => {
    if (!value || options.some((option) => option.id === value) || resolved?.id === value) return;
    const controller = new AbortController();
    patientsApi
      .getPatient(value, controller.signal)
      .then((patient) =>
        setResolved({ id: value, label: `${patient.fullName} · ${patient.patientCode}` }),
      )
      .catch(() => {
        // A stale or unknown id simply shows no label.
      });
    return () => controller.abort();
  }, [value, options, resolved]);

  // Close when focus leaves the widget.
  useEffect(() => {
    if (!open) return;
    const onPointerDown = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onPointerDown);
    return () => document.removeEventListener('mousedown', onPointerDown);
  }, [open]);

  const commit = useCallback(
    (option: Patient) => {
      onChange(option.id);
      setResolved({ id: option.id, label: `${option.fullName} · ${option.patientCode}` });
      setQuery('');
      setOpen(false);
    },
    [onChange],
  );

  const onKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Escape') {
      setOpen(false);
      return;
    }
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (!open) {
        setOpen(true);
        return;
      }
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

  const activeOptionId = open && options[activeIndex] ? `${listboxId}-${activeIndex}` : undefined;

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
          aria-activedescendant={activeOptionId}
          aria-label={label}
          autoComplete="off"
          className={cn(inputStyles, value && !query && 'pr-16', 'pr-9')}
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
              aria-label="Clear patient filter"
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
            <li className="px-3 py-2.5 text-[13px] text-muted">No patients match that search.</li>
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
                <span className="block truncate text-fg">{option.fullName}</span>
                <span className="block text-xs text-subtle">{option.patientCode}</span>
              </span>
              {option.id === value ? <Check aria-hidden className="size-3.5 text-accent" /> : null}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
