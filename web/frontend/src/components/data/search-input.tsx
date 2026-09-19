import { Search, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { inputStyles } from '@/components/ui/input';

export interface SearchInputProps {
  value: string;
  onValueChange: (value: string) => void;
  placeholder?: string;
  /** Accessible name; visually hidden. */
  label: string;
  className?: string;
  id?: string;
}

/** Search field with a leading icon and a clear button. */
export function SearchInput({
  value,
  onValueChange,
  placeholder = 'Search…',
  label,
  className,
  id,
}: SearchInputProps) {
  return (
    <div className={cn('relative', className)}>
      <label htmlFor={id} className="sr-only">
        {label}
      </label>
      <Search
        aria-hidden
        className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-subtle"
      />
      <input
        id={id}
        type="search"
        role="searchbox"
        value={value}
        onChange={(event) => onValueChange(event.target.value)}
        placeholder={placeholder}
        className={cn(inputStyles, 'pl-9', value && 'pr-9')}
      />
      {value ? (
        <button
          type="button"
          onClick={() => onValueChange('')}
          aria-label="Clear search"
          className="absolute top-1/2 right-2 flex size-6 -translate-y-1/2 items-center justify-center rounded-md text-subtle hover:bg-elevated hover:text-fg"
        >
          <X aria-hidden className="size-3.5" />
        </button>
      ) : null}
    </div>
  );
}
