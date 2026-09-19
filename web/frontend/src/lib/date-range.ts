/**
 * Converts the two `YYYY-MM-DD` inputs of a range filter into the ISO bounds
 * the API expects.
 *
 * The backend documents `from` as an inclusive lower bound and `to` as an
 * exclusive upper bound, so the end date is advanced by one day to make the
 * filtering inclusive from the user's point of view.
 */
export function toApiRange(range: { from: string; to: string }): {
  from?: string;
  to?: string;
} {
  const result: { from?: string; to?: string } = {};

  if (range.from) {
    const start = new Date(`${range.from}T00:00:00`);
    if (!Number.isNaN(start.getTime())) result.from = start.toISOString();
  }

  if (range.to) {
    const end = new Date(`${range.to}T00:00:00`);
    if (!Number.isNaN(end.getTime())) {
      end.setDate(end.getDate() + 1);
      result.to = end.toISOString();
    }
  }

  return result;
}

/** Today as `YYYY-MM-DD` in the browser's timezone. */
export function todayInputValue(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}
