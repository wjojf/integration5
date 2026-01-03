import { ComponentPropsWithoutRef } from "react"
import { mergeClassNames } from "../../../utils"

type CardProps = ComponentPropsWithoutRef<"div">

export const Card = ({ className, ...props }: CardProps) => {
  return (
    <div
      data-slot="card"
      className={mergeClassNames("bg-card text-card-foreground flex flex-col gap-6 rounded-xl border", className)}
      {...props}
    />
  );
}

export const CardHeader = ({ className, ...props }: CardProps) => {
  return (
    <div
      className={mergeClassNames("flex flex-col space-y-1.5 p-6", className)}
      {...props}
    />
  );
}

export const CardTitle = ({ className, ...props }: ComponentPropsWithoutRef<"h3">) => {
  return (
    <h3
      className={mergeClassNames("font-semibold leading-none tracking-tight", className)}
      {...props}
    />
  );
}

export const CardContent = ({ className, ...props }: CardProps) => {
  return (
    <div
      className={mergeClassNames("p-6 pt-0", className)}
      {...props}
    />
  );
}

