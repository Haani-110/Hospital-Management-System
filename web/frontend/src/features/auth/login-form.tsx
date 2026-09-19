import { useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field } from '@/components/ui/field';
import { Input, PasswordInput } from '@/components/ui/input';
import { ApiError } from '@/lib/api/client';
import { useAuth } from '@/providers/auth-context';

interface FieldErrors {
  username?: string;
  password?: string;
}

/**
 * Credential form.
 *
 * Client-side rules mirror the backend `LoginDto` exactly
 * (username 3–50, password 8–128) so the two never disagree; anything the
 * server rejects is still surfaced from the response.
 */
export function LoginForm() {
  const { login, isSigningIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [showSuccess, setShowSuccess] = useState(false);

  const redirectTo = useMemo(() => {
    const state = location.state as { from?: string } | null;
    return state?.from && state.from !== '/login' ? state.from : '/';
  }, [location.state]);

  function validate(): FieldErrors {
    const errors: FieldErrors = {};
    if (!username) errors.username = 'Enter your username.';
    else if (username.length < 3) errors.username = 'Usernames are at least 3 characters.';
    else if (username.length > 50) errors.username = 'Usernames are at most 50 characters.';

    if (!password) errors.password = 'Enter your password.';
    else if (password.length < 8) errors.password = 'Passwords are at least 8 characters.';
    else if (password.length > 128) errors.password = 'Passwords are at most 128 characters.';

    return errors;
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);
    setShowSuccess(false);

    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    try {
      await login({ username, password });
      setShowSuccess(true);
      void navigate(redirectTo, { replace: true });
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.isUnauthorized) {
          // The API deliberately returns one message for unknown user, wrong
          // password and inactive account.
          setFormError(error.messages.join(' '));
        } else if (error.isNetworkError) {
          setFormError(
            'Cannot reach the API. Start the backend on http://localhost:4000 and try again.',
          );
        } else {
          setFormError(error.messages.join(' '));
        }
      } else {
        setFormError('Sign-in failed unexpectedly. Please try again.');
      }
    }
  }

  return (
    <form onSubmit={(event) => void handleSubmit(event)} noValidate className="space-y-5">
      {formError ? (
        <Alert tone="danger" title="Sign-in failed" live="assertive">
          {formError}
        </Alert>
      ) : null}

      {/* Announced to screen readers once the redirect is under way. */}
      <p role="status" aria-live="polite" className="sr-only">
        {showSuccess ? 'Signed in. Redirecting to the dashboard.' : ''}
      </p>

      <Field label="Username" error={fieldErrors.username} required>
        {(fieldProps) => (
          <Input
            {...fieldProps}
            name="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            placeholder="e.g. admin"
            hasError={Boolean(fieldErrors.username)}
            disabled={isSigningIn}
          />
        )}
      </Field>

      <Field label="Password" error={fieldErrors.password} required>
        {(fieldProps) => (
          <PasswordInput
            {...fieldProps}
            name="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            placeholder="Your password"
            hasError={Boolean(fieldErrors.password)}
            disabled={isSigningIn}
          />
        )}
      </Field>

      <Button type="submit" size="lg" className="w-full" isLoading={isSigningIn}>
        {isSigningIn ? 'Signing in…' : 'Sign in'}
      </Button>
    </form>
  );
}
