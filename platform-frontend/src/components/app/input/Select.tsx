// components/ui/select.tsx
import * as React from "react";
import * as SelectPrimitive from "@radix-ui/react-select";
import { Check, ChevronDown } from "lucide-react";

const cn = (...classes: Array<string | undefined | false | null>) =>
    classes.filter(Boolean).join(" ");

export type SelectProps = React.ComponentPropsWithoutRef<typeof SelectPrimitive.Root>;
export const Select = (props: SelectProps) => (
    <SelectPrimitive.Root data-slot="select" {...props} />
);

export const SelectValue = SelectPrimitive.Value;

export const SelectTrigger = React.forwardRef<
    React.ElementRef<typeof SelectPrimitive.Trigger>,
    React.ComponentPropsWithoutRef<typeof SelectPrimitive.Trigger> & {
  leftIcon?: React.ReactNode;
}
>(({ className, children, leftIcon, ...props }, ref) => (
    <SelectPrimitive.Trigger
        ref={ref}
        data-slot="select-trigger"
        className={cn(
            "input h-11 w-full inline-flex items-center justify-between gap-2 pr-10",
            className
        )}
        {...props}
    >
    <span className="inline-flex min-w-0 items-center gap-2">
      {leftIcon ? (
          <span className="inline-flex h-4 w-4 items-center justify-center text-muted-foreground">
          {leftIcon}
        </span>
      ) : null}
      {children}
    </span>

      <SelectPrimitive.Icon asChild>
        <ChevronDown className="h-4 w-4 text-muted-foreground" />
      </SelectPrimitive.Icon>
    </SelectPrimitive.Trigger>
));
SelectTrigger.displayName = "SelectTrigger";

export const SelectContent = React.forwardRef<
    React.ElementRef<typeof SelectPrimitive.Content>,
    React.ComponentPropsWithoutRef<typeof SelectPrimitive.Content>
>(({ className, children, position = "popper", sideOffset = 8, ...props }, ref) => (
    <SelectPrimitive.Portal>
      <SelectPrimitive.Content
          ref={ref}
          data-slot="select-content"
          position={position}
          sideOffset={sideOffset}
          className={cn(
              "z-50 overflow-hidden rounded-md border border-border bg-card shadow-lg",
              className
          )}
          {...props}
      >
        <SelectPrimitive.Viewport className="p-1">{children}</SelectPrimitive.Viewport>
      </SelectPrimitive.Content>
    </SelectPrimitive.Portal>
));

export const SelectItem = React.forwardRef<
    React.ElementRef<typeof SelectPrimitive.Item>,
    React.ComponentPropsWithoutRef<typeof SelectPrimitive.Item>
>(({ className, children, ...props }, ref) => (
    <SelectPrimitive.Item
        ref={ref}
        data-slot="select-item"
        className={cn(
            "relative flex w-full cursor-pointer select-none items-center justify-between rounded-sm px-3 py-2 text-sm text-foreground outline-none",
            "focus:bg-secondary data-[highlighted]:bg-secondary",
            "data-[disabled]:pointer-events-none data-[disabled]:opacity-50",
            className
        )}
        {...props}
    >
      <SelectPrimitive.ItemText className="truncate">{children}</SelectPrimitive.ItemText>
      <SelectPrimitive.ItemIndicator className="ml-3 text-primary">
        <Check className="h-4 w-4" />
      </SelectPrimitive.ItemIndicator>
    </SelectPrimitive.Item>
));
