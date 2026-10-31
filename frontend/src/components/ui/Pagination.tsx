import { ChevronLeftIcon, ChevronRightIcon } from '@/components/icons';
import { cn } from '@/lib/cn';
import { formatNumber } from '@/lib/format';

export interface PaginationProps {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
  className?: string;
}

function buildPageWindow(current: number, total: number): Array<number | 'ellipsis'> {
  if (total <= 7) return Array.from({ length: total }, (_, index) => index);

  const pages = new Set<number>([0, total - 1, current]);
  if (current - 1 > 0) pages.add(current - 1);
  if (current + 1 < total - 1) pages.add(current + 1);

  const sorted = [...pages].sort((a, b) => a - b);
  const window: Array<number | 'ellipsis'> = [];
  sorted.forEach((value, index) => {
    const previous = sorted[index - 1];
    if (previous !== undefined && value - previous > 1) window.push('ellipsis');
    window.push(value);
  });
  return window;
}

export function Pagination({
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
  disabled = false,
  className,
}: PaginationProps) {
  const start = totalElements === 0 ? 0 : page * size + 1;
  const end = Math.min((page + 1) * size, totalElements);
  const pages = buildPageWindow(page, totalPages);

  return (
    <nav
      aria-label="Pagination"
      className={cn(
        'flex flex-col gap-3 border-t border-line px-4 py-3 text-sm text-ink-600 sm:flex-row sm:items-center sm:justify-between',
        className,
      )}
    >
      <p aria-live="polite">
        {totalElements === 0
          ? 'No records'
          : `Showing ${formatNumber(start)}–${formatNumber(end)} of ${formatNumber(totalElements)}`}
      </p>

      {totalPages > 1 && (
        <div className="flex flex-wrap items-center gap-1">
          <button
            type="button"
            onClick={() => onPageChange(page - 1)}
            disabled={disabled || page <= 0}
            className="inline-flex h-8 items-center gap-1 rounded-md border border-line bg-surface-card px-2.5 font-medium text-ink-700 hover:bg-surface-muted disabled:cursor-not-allowed disabled:text-ink-400"
            aria-label="Previous page"
          >
            <ChevronLeftIcon className="h-4 w-4" />
            <span className="hidden sm:inline">Previous</span>
          </button>

          {pages.map((item, index) =>
            item === 'ellipsis' ? (
              <span key={`ellipsis-${index}`} className="px-1 text-ink-400" aria-hidden="true">
                …
              </span>
            ) : (
              <button
                key={item}
                type="button"
                onClick={() => onPageChange(item)}
                disabled={disabled}
                aria-label={`Page ${item + 1}`}
                aria-current={item === page ? 'page' : undefined}
                className={cn(
                  'inline-flex h-8 min-w-8 items-center justify-center rounded-md border px-2 font-medium tabular-nums',
                  item === page
                    ? 'border-brand-700 bg-brand-700 text-white'
                    : 'border-line bg-surface-card text-ink-700 hover:bg-surface-muted',
                )}
              >
                {item + 1}
              </button>
            ),
          )}

          <button
            type="button"
            onClick={() => onPageChange(page + 1)}
            disabled={disabled || page >= totalPages - 1}
            className="inline-flex h-8 items-center gap-1 rounded-md border border-line bg-surface-card px-2.5 font-medium text-ink-700 hover:bg-surface-muted disabled:cursor-not-allowed disabled:text-ink-400"
            aria-label="Next page"
          >
            <span className="hidden sm:inline">Next</span>
            <ChevronRightIcon className="h-4 w-4" />
          </button>
        </div>
      )}
    </nav>
  );
}
