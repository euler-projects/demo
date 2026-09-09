import { CircleAlertIcon } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { cn } from '@/lib/utils';
import type { FormError } from '@/login/errors';

/**
 * The single error callout of the entrance app: a destructive-tinted
 * Alert (the registry's custom-colour pattern, in error semantics) with
 * an error icon, the localized headline and - only when the server
 * envelope carried an explicit message - a verbatim description.
 *
 * Positioning is the caller's: SignIn parks it absolutely below the
 * centred column so an appearing error never shifts the layout, SignOut
 * lets it flow under its form.
 */
export function ErrorAlert({
  error,
  className,
}: {
  error: FormError | null;
  className?: string;
}) {
  if (!error) return null;
  return (
    <Alert
      variant="destructive"
      className={cn('border-destructive/30 bg-destructive/10', className)}
    >
      <CircleAlertIcon />
      <AlertTitle>{error.title}</AlertTitle>
      {error.description && <AlertDescription>{error.description}</AlertDescription>}
    </Alert>
  );
}
