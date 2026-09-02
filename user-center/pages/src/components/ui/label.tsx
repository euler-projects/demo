"use client"

import * as React from "react"

import { cn } from "@/lib/utils"

// Upstream shadcn (base-nova) also builds Label on the native element:
// Base UI 1.x ships no standalone label primitive, so `htmlFor`
// association behaves identically to a styled `<label>` wrapper.
function Label({ className, ...props }: React.ComponentProps<"label">) {
  return (
    <label
      data-slot="label"
      className={cn(
        "flex items-center gap-2 text-sm leading-none font-medium select-none group-data-[disabled=true]:pointer-events-none group-data-[disabled=true]:opacity-50 peer-disabled:cursor-not-allowed peer-disabled:opacity-50",
        className
      )}
      {...props}
    />
  )
}

export { Label }
