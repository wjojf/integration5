import { Avatar as AvatarPrimitive } from "radix-ui"
import { ComponentPropsWithoutRef } from "react"

import { mergeClassNames } from "../../../utils"

type AvatarProps = ComponentPropsWithoutRef<typeof AvatarPrimitive.Root>

export const Avatar = ({className, ...props}: AvatarProps) => {
  return (
    <AvatarPrimitive.Root
      data-slot="avatar"
      className={mergeClassNames("relative flex shrink-0 size-10 overflow-hidden rounded-full", className)}
      {...props}
    />
  );
}

type AvatarFallbackProps = ComponentPropsWithoutRef<typeof AvatarPrimitive.Fallback>

export const AvatarFallback = ({className, ...props}: AvatarFallbackProps) => {
  return (
    <AvatarPrimitive.Fallback
      data-slot="avatar-fallback"
      className={mergeClassNames("bg-muted flex size-full items-center justify-center rounded-full", className)}
      {...props}
    />
  );
}

