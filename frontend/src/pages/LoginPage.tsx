import { useEffect, useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';
import { normalizeError, type AppError } from '@/lib/errors';
import { SHOW_DEMO_HINT, APP_OWNER } from '@/config';
import { Alert } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { FormField } from '@/components/ui/FormField';
import { FullPageLoader } from '@/components/ui/FullPageLoader';
import { Input } from '@/components/ui/Input';
import { CheckCircleIcon, ShieldCheckIcon } from '@/components/icons';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';

interface FieldErrors {
  email?: string;
  password?: string;
}

interface GuestAccount {
  label: string;
  email: string;
}

const DEMO_PASSWORD = 'Demo@12345';

const GUEST_ACCOUNTS: readonly GuestAccount[] = [
  { label: 'Platform admin', email: 'admin@apaar.local' },
  { label: 'AICTE officer', email: 'officer@aicte.local' },
  { label: 'Institution admin', email: 'admin@sunrise.demo' },
  { label: 'Data operator', email: 'operator@sunrise.demo' },
  { label: 'Verification officer', email: 'verifier@sunrise.demo' },
  { label: 'Case reviewer', email: 'reviewer@apaar.local' },
  { label: 'Auditor', email: 'auditor@apaar.local' },
];

function validate(email: string, password: string): FieldErrors {
  const errors: FieldErrors = {};
  const trimmedEmail = email.trim();

  if (!trimmedEmail) {
    errors.email = 'Email address is required.';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
    errors.email = 'Enter a valid email address.';
  }

  if (!password) {
    errors.password = 'Password is required.';
  }

  return errors;
}

const capabilities = [
  'Verify APAAR-linked student identities against institutional records.',
  'Review enrolment discrepancies, compliance findings and case evidence.',
  'Keep an auditable trail of every privileged action.',
];

export function LoginPage() {
  const { login, status, notice } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [serverError, setServerError] = useState<AppError | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [guestEmail, setGuestEmail] = useState<string | null>(null);

  useDocumentTitle('Sign in');

  const from = (location.state as { from?: { pathname?: string; search?: string } } | null)?.from;
  const redirectTarget =
    from?.pathname && from.pathname.startsWith('/') && !from.pathname.startsWith('/login')
      ? `${from.pathname}${from.search ?? ''}`
      : '/dashboard';

  // One-click guest sign-in for the seeded demo roles. Enabled by default in
  // demo builds; VITE_SHOW_DEMO_HINT=false removes it entirely.
  const showGuestAccess = SHOW_DEMO_HINT;
  const busy = submitting || guestEmail !== null;

  useEffect(() => {
    if (status === 'authenticated') {
      navigate(redirectTarget, { replace: true });
    }
  }, [status, navigate, redirectTarget]);

  if (status === 'booting' || status === 'authenticated') {
    return (
      <FullPageLoader
        label={status === 'booting' ? 'Checking your session' : 'Opening your workspace'}
      />
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextErrors = validate(email, password);
    setFieldErrors(nextErrors);
    setServerError(null);

    if (Object.keys(nextErrors).length > 0) return;

    setSubmitting(true);
    try {
      await login(email.trim(), password);
      navigate(redirectTarget, { replace: true });
    } catch (error) {
      setServerError(normalizeError(error));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleGuestLogin(account: GuestAccount) {
    setServerError(null);
    setFieldErrors({});
    setGuestEmail(account.email);
    try {
      await login(account.email, DEMO_PASSWORD);
      navigate(redirectTarget, { replace: true });
    } catch (error) {
      setServerError(normalizeError(error));
    } finally {
      setGuestEmail(null);
    }
  }

  return (
    <div className="min-h-screen bg-brand-950 lg:grid lg:grid-cols-[1.05fr_1fr]">
      <a
        href="#login-main"
        className="sr-only z-50 rounded-md bg-white px-4 py-2 text-sm font-semibold text-brand-900 focus:not-sr-only focus:fixed focus:left-4 focus:top-4"
      >
        Skip to sign-in form
      </a>

      <section
        aria-labelledby="login-brand-heading"
        className="relative hidden overflow-hidden bg-gradient-to-br from-brand-950 via-brand-900 to-brand-800 px-12 py-14 text-white lg:flex lg:flex-col lg:justify-between"
      >
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 opacity-[0.08]"
          style={{
            backgroundImage:
              'linear-gradient(to right, white 1px, transparent 1px), linear-gradient(to bottom, white 1px, transparent 1px)',
            backgroundSize: '48px 48px',
          }}
        />

        <div className="relative">
          <div className="flex items-center gap-3">
            <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/10 text-accent-300">
              <ShieldCheckIcon className="h-7 w-7" />
            </span>
            <div>
              <p className="text-sm font-semibold tracking-widest text-accent-300">APAAR VERIFY</p>
              <p className="text-xs text-brand-100/70">{APP_OWNER}</p>
            </div>
          </div>

          <h1 id="login-brand-heading" className="mt-14 max-w-xl text-4xl font-semibold leading-tight">
            Trusted student data for AICTE-approved institutions.
          </h1>
          <p className="mt-5 max-w-lg text-base leading-relaxed text-brand-100/80">
            A single, auditable workspace for APAAR ID verification, enrolment integrity and
            regulatory oversight.
          </p>

          <ul className="mt-10 space-y-4">
            {capabilities.map((item) => (
              <li key={item} className="flex items-start gap-3 text-sm text-brand-50/90">
                <CheckCircleIcon className="mt-0.5 h-5 w-5 shrink-0 text-accent-300" />
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>

        <p className="relative mt-12 max-w-md text-xs leading-relaxed text-brand-100/60">
          Designated institutional users only. All access is attributable and logged for regulatory
          audit.
        </p>
      </section>

      <main
        id="login-main"
        className="flex min-h-screen items-center justify-center bg-surface px-4 py-10 sm:px-8"
      >
        <div className="w-full max-w-md">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-800 text-accent-300">
              <ShieldCheckIcon className="h-6 w-6" />
            </span>
            <div>
              <p className="text-sm font-semibold uppercase tracking-widest text-brand-800">
                APAAR Verify
              </p>
              <p className="text-xs text-ink-500">{APP_OWNER}</p>
            </div>
          </div>

          <Card className="p-6 sm:p-8">
            <div className="mb-6">
              <p className="text-xs font-semibold uppercase tracking-wider text-brand-600">
                Secure sign in
              </p>
              <h1 className="mt-1 text-2xl font-semibold tracking-tight text-ink-900">
                Sign in to the platform
              </h1>
              <p className="mt-2 text-sm text-ink-600">
                Use the institutional account issued to you by AICTE or your institution.
              </p>
            </div>

            {notice && (
              <Alert variant="warning" className="mb-5">
                {notice}
              </Alert>
            )}

            {serverError && (
              <Alert variant="danger" title="Sign-in failed" className="mb-5">
                <p>{serverError.message}</p>
                {serverError.requestId && (
                  <p className="mt-1 font-mono text-xs opacity-80">
                    Request ID: {serverError.requestId}
                  </p>
                )}
              </Alert>
            )}

            <form onSubmit={handleSubmit} noValidate className="space-y-5">
              <FormField id="email" label="Email address" error={fieldErrors.email} required>
                <Input
                  type="email"
                  name="email"
                  autoComplete="username"
                  inputMode="email"
                  placeholder="name@institution.ac.in"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  autoFocus
                />
              </FormField>

              <FormField id="password" label="Password" error={fieldErrors.password} required>
                <Input
                  type="password"
                  name="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </FormField>

              <Button type="submit" fullWidth loading={submitting} disabled={busy}>
                Sign in
              </Button>
            </form>

            {showGuestAccess && (
              <section
                aria-labelledby="guest-access-heading"
                className="mt-6 rounded-lg border border-line bg-surface-muted/70 px-4 py-4"
              >
                <h2
                  id="guest-access-heading"
                  className="text-xs font-semibold uppercase tracking-wider text-brand-600"
                >
                  Demo guest access
                </h2>
                <p className="mt-1 text-xs leading-relaxed text-ink-600">
                  Explore the workspace as a seeded role. Guest accounts share the documented
                  demo password and synthetic data only.
                </p>

                <ul className="mt-3 grid gap-2 sm:grid-cols-2">
                  {GUEST_ACCOUNTS.map((account) => (
                    <li key={account.email}>
                      <Button
                        type="button"
                        variant="secondary"
                        size="sm"
                        fullWidth
                        className="justify-start"
                        loading={guestEmail === account.email}
                        disabled={busy}
                        title={account.email}
                        onClick={() => handleGuestLogin(account)}
                      >
                        {account.label}
                      </Button>
                    </li>
                  ))}
                </ul>

                <p className="mt-3 text-[11px] leading-relaxed text-ink-500">
                  Guest sign-in is for demonstration only. Never enter real student data.
                </p>
              </section>
            )}

            <p className="mt-6 text-center text-xs leading-relaxed text-ink-500">
              Need access? Contact your institution&apos;s APAAR nodal officer or the AICTE
              helpdesk.
            </p>
          </Card>

          <p className="mt-6 text-center text-xs text-ink-500">
            Authorised users only. Session tokens are kept in this tab only.
          </p>
        </div>
      </main>
    </div>
  );
}
