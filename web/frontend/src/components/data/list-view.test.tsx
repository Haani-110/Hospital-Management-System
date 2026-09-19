import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ListView } from './list-view';
import { ApiError } from '@/lib/api/client';

describe('ListView', () => {
  it('shows a skeleton while loading for the first time', () => {
    render(
      <ListView isInitialLoading error={null} isEmpty={false} emptyTitle="Nothing">
        <p>rows</p>
      </ListView>,
    );

    expect(screen.getByRole('status', { name: /loading data/i })).toBeInTheDocument();
    expect(screen.queryByText('rows')).not.toBeInTheDocument();
  });

  it('explains a forbidden response instead of showing raw errors', () => {
    render(
      <ListView
        isInitialLoading={false}
        error={new ApiError(403, ['Your role is not allowed to perform this action'], 'Forbidden')}
        isEmpty={false}
        emptyTitle="Nothing"
      >
        <p>rows</p>
      </ListView>,
    );

    expect(screen.getByText(/do not have permission/i)).toBeInTheDocument();
    expect(screen.queryByText('rows')).not.toBeInTheDocument();
  });

  it('offers a retry when the API cannot be reached', async () => {
    const onRetry = vi.fn();
    const user = userEvent.setup();

    render(
      <ListView
        isInitialLoading={false}
        error={new ApiError(0, ['Unable to reach the server.'], 'Network Error')}
        isEmpty={false}
        emptyTitle="Nothing"
        onRetry={onRetry}
      >
        <p>rows</p>
      </ListView>,
    );

    await user.click(screen.getByRole('button', { name: /try again/i }));
    expect(onRetry).toHaveBeenCalledOnce();
  });

  it('renders the empty state with an optional action', () => {
    render(
      <ListView
        isInitialLoading={false}
        error={null}
        isEmpty
        emptyTitle="No patients yet"
        emptyDescription="Records appear once created."
        emptyAction={<button type="button">Clear filters</button>}
      >
        <p>rows</p>
      </ListView>,
    );

    expect(screen.getByText('No patients yet')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Clear filters' })).toBeInTheDocument();
  });

  it('renders content once data is available', () => {
    render(
      <ListView isInitialLoading={false} error={null} isEmpty={false} emptyTitle="Nothing">
        <p>rows</p>
      </ListView>,
    );

    expect(screen.getByText('rows')).toBeInTheDocument();
  });
});
