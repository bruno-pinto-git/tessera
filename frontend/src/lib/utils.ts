import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Compose className strings safely. The shadcn/ui convention — every UI
 * primitive accepts `className` and uses `cn(base, props.className)` so
 * downstream callers can override sensibly without specificity wars.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
