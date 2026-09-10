import * as React from "react"
import { cn } from "cn"
import { OTPInput, OTPInputContext } from "input-otp"
import { MinusIcon } from "lucide-react"

function InputOTP({
  className,
  containerClassName,
  ...props
}: React.ComponentProps<typeof OTPInput> & {
  containerClassName?: string
}) {
  return (
    <OTPInput
      data-slot="input-otp"
      containerClassName={cn(
        "cn-input-otp flex items-center has-disabled:opacity-50",
        containerClassName
      )}
      spellCheck={false}
      className={cn("disabled:cursor-not-allowed", className)}
      {...props}
    />
  )
}

function InputOTPGroup({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="input-otp-group"
      className={cn(
        // Detached boxes, not the registry's connected segment strip:
        // every slot carries its own full border and the group spaces
        // them apart. Row geometry (resize recipe for future edits):
        //   C = 320px  the form column (w-80);
        //   i = 12px   inset of the secondary-action labels below - the
        //              link Buttons' px-3 at size sm - which the row ends
        //              must align with;
        //   R = C - 2i = 296px  the usable row width;
        //   n = 6 slots, g = gap, s = slot size, R = n*s + (n-1)*g;
        //   pick g on the tailwind gap scale first, then s = (R - 5g)/6
        //   must come out integral: g=10 (gap-2.5) => s=41 (size-[41px]);
        //   r = slot radius ~ 0.27*s, the approved proportion carried
        //   over from the original 36px/10px boxes => r=11px here.
        // The row is centred, so a 296px row leaves exactly i per side
        // and its ends line up with the Back/Resend labels. Concentric/
        // proportion rules reserve the theme's large radius steps for
        // larger surfaces, and full circles would put the shape ahead
        // of the digits.
        "flex items-center gap-2.5 rounded-[11px] has-aria-invalid:border-destructive has-aria-invalid:ring-3 has-aria-invalid:ring-destructive/20 dark:has-aria-invalid:ring-destructive/40",
        className
      )}
      {...props}
    />
  )
}

function InputOTPSlot({
  index,
  className,
  ...props
}: React.ComponentProps<"div"> & {
  index: number
}) {
  const inputOTPContext = React.useContext(OTPInputContext)
  const { char, hasFakeCaret, isActive } = inputOTPContext?.slots[index] ?? {}

  return (
    <div
      data-slot="input-otp-slot"
      data-active={isActive}
      className={cn(
        // s and r from the row-geometry recipe on InputOTPGroup:
        // s = 41px, r = 11px.
        "relative flex size-[41px] items-center justify-center rounded-[11px] border border-input bg-input/50 text-sm transition-all outline-none aria-invalid:border-destructive data-[active=true]:z-10 data-[active=true]:border-ring data-[active=true]:ring-3 data-[active=true]:ring-ring/30 data-[active=true]:aria-invalid:ring-destructive/20 dark:data-[active=true]:aria-invalid:ring-destructive/40",
        className
      )}
      {...props}
    >
      {char}
      {hasFakeCaret && (
        <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
          <div className="h-4 w-px animate-caret-blink bg-foreground duration-1000" />
        </div>
      )}
    </div>
  )
}

function InputOTPSeparator({ ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="input-otp-separator"
      className="flex items-center [&_svg:not([class*='size-'])]:size-4"
      role="separator"
      {...props}
    >
      <MinusIcon
      />
    </div>
  )
}

export { InputOTP, InputOTPGroup, InputOTPSlot, InputOTPSeparator }
