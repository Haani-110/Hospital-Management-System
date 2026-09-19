import { Transform } from 'class-transformer';

/** Query strings arrive as text; convert `"true"`/`"false"` to real booleans. */
export const TransformToBoolean = (): PropertyDecorator =>
  Transform(({ value }) => {
    if (typeof value === 'boolean' || value === undefined) return value;
    if (value === 'true') return true;
    if (value === 'false') return false;
    return value;
  });

/** Converts an ISO date string into a Date; invalid input stays invalid for validators to reject. */
export const TransformToDate = (): PropertyDecorator =>
  Transform(({ value }) => (typeof value === 'string' ? new Date(value) : value));
