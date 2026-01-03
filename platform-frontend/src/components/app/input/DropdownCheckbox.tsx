import * as React from "react";
import { Check, ChevronDown } from "lucide-react";
import { Checkbox as CheckboxPrimitive, DropdownMenu } from "radix-ui";

import { mergeClassNames } from "../../../utils";

type CheckboxItem = {
    id: string;
    text: string;
};

type DropdownCheckboxProps = {
    items?: CheckboxItem[];
    checkedItemIds: string[];
    onValueChange: (nextIds: string[]) => void;
    placeholder?: string;
    disabled?: boolean;
    className?: string;
    itemClassName?: string;
    contentClassName?: string;
};

export const DropdownCheckbox = (props: DropdownCheckboxProps) => {
    const {
        items= [], checkedItemIds, onValueChange,
        placeholder = "Select...", disabled = false, className,
        itemClassName, contentClassName
    } = props

    const selectedText = checkedItemIds.length === 0 ? placeholder : `${checkedItemIds.length} selected`
    const toggle = (id: string, checked: boolean) => {
        const updatedCheckItemIds = checked
            ? Array.from(new Set([...checkedItemIds, id]))
            : checkedItemIds.filter((itemId) => itemId !== id);

        onValueChange(updatedCheckItemIds);
    };

    return (
        <div className={mergeClassNames("space-y-2", className)}>
            <DropdownMenu.Root>
                <DropdownMenu.Trigger asChild disabled={disabled}>
                    <button
                        type="button"
                        className={mergeClassNames(
                            "w-full inline-flex items-center justify-between gap-2 rounded-md px-3 py-2",
                            "border border-[var(--border)] bg-[color:var(--color-bg)] text-[var(--foreground)]",
                            "focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-[var(--ring)]",
                            "disabled:opacity-60 disabled:cursor-not-allowed"
                        )}
                    >
                        <span className="text-sm">
                          {selectedText}
                        </span>
                        <ChevronDown className="h-4 w-4 text-[var(--muted-foreground)]" />
                    </button>
                </DropdownMenu.Trigger>

                <DropdownMenu.Content
                    align="start"
                    sideOffset={8}
                    className={mergeClassNames(
                        "z-50 min-w-[--radix-dropdown-menu-trigger-width] rounded-md, border border-[var(--border)] bg-[var(--card)] shadow-[var(--shadow-soft)] p-2",
                        contentClassName)}
                >
                    {items.length === 0 ? (
                        <div className="px-2 py-2 text-sm text-[var(--muted-foreground)]">
                            No items.
                        </div>
                    ) : (
                        <div className="max-h-72 overflow-auto">
                            {CheckboxItems(items, disabled, toggle, checkedItemIds, itemClassName)}
                        </div>
                    )}
                </DropdownMenu.Content>
            </DropdownMenu.Root>
        </div>
    );
};

const CheckboxItems = (items:CheckboxItem[] , disabled: boolean, toggle: Function, checkedItemIds: string[], itemClassName?: string) => {
    return items.map((item) => {
        const checked = checkedItemIds.includes(item.id);
        const inputId = `checkboxItem-${item.id}`;

        return (
            <div
                key={item.id}
                className={mergeClassNames(
                    "flex items-center gap-2 rounded-md px-2 py-2 hover:bg-[var(--secondary)]",
                    itemClassName)}
                onSelect={(e) => e.preventDefault?.()}
            >
                <CheckboxPrimitive.Root
                    id={inputId}
                    checked={checked}
                    disabled={disabled}
                    onCheckedChange={(v) => toggle(item.id, v === true)}
                    className={mergeClassNames(
                        "h-5 w-5 shrink-0 rounded-[6px] border border-[var(--border)]",
                        "bg-[color:var(--color-bg)]",
                        "data-[state=checked]:bg-[var(--primary)] data-[state=checked]:border-[var(--primary)]",
                        "focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-[var(--ring)]",
                        "disabled:opacity-60 disabled:cursor-not-allowed"
                    )}
                >
                    <CheckboxPrimitive.Indicator className="flex items-center justify-center">
                        <Check className="h-4 w-4 text-[var(--primary-foreground)]" />
                    </CheckboxPrimitive.Indicator>
                </CheckboxPrimitive.Root>

                <label
                    htmlFor={inputId}
                    className={mergeClassNames(
                        "text-sm text-[var(--foreground)] select-none cursor-pointer",
                        disabled && "cursor-not-allowed opacity-60"
                    )}
                >
                    {item.text}
                </label>
            </div>
        );
    })
}
