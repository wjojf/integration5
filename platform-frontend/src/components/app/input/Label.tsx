import { Label as LabelPrimitive } from "radix-ui";
import { ComponentPropsWithoutRef } from "react"

import { mergeClassNames } from "../../../utils"

type LabelProps = ComponentPropsWithoutRef<typeof LabelPrimitive.Root>

export const Label = ({className, ...props}: LabelProps) => {
  return (
    <LabelPrimitive.Root
      data-slot="label"
      className={mergeClassNames(
        "flex items-center gap-2 text-sm leading-none font-medium select-none group-data-[disabled=true]:pointer-events-none group-data-[disabled=true]:opacity-50 peer-disabled:cursor-not-allowed peer-disabled:opacity-50",
        className,
      )}
      {...props}
    />
  );
}

