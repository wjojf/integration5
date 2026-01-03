import { createContext, useContext, useState, ComponentPropsWithoutRef } from "react";

import { mergeClassNames } from "../../../utils";

type TabsContextType = {
    activeTab: string;
    setActiveTab: (value: string) => void;
};

const TabsContext = createContext<TabsContextType | undefined>(undefined);

interface TabsProps extends ComponentPropsWithoutRef<"div"> {
    defaultValue: string;
}

export const Tab = ({ defaultValue, className, children, ...props }: TabsProps) => {
    const [activeTab, setActiveTab] = useState(defaultValue);

    return (
        <TabsContext.Provider value={{ activeTab, setActiveTab }}>
            <div
                className={mergeClassNames("w-full", className)}
                {...props}
            >
                {children}
            </div>
        </TabsContext.Provider>
    );
};

export const TabList = ({ className, ...props }: ComponentPropsWithoutRef<"div">) => {
    return (
        <div
            className={mergeClassNames(
                "inline-flex h-9 items-center justify-center rounded-lg bg-muted p-1 text-muted-foreground",
                className
            )}
            {...props}
        />
    );
};

interface TabsTriggerProps extends ComponentPropsWithoutRef<"button"> {
    value: string;
}

export const TabTrigger = ({ value, className, ...props }: TabsTriggerProps) => {
    const context = useContext(TabsContext);
    if (!context) throw new Error("TabsTrigger must be used within Tabs");

    const isActive = context.activeTab === value;

    return (
        <button
            className={mergeClassNames(
                "inline-flex items-center justify-center whitespace-nowrap rounded-md px-3 py-1 text-sm font-medium ring-offset-background transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50",
                isActive && "bg-background text-foreground shadow",
                className
            )}
            onClick={() => context.setActiveTab(value)}
            {...props}
        />
    );
};

interface TabsContentProps extends ComponentPropsWithoutRef<"div"> {
    value: string;
}

export const TabContent = ({ value, className, children, ...props }: TabsContentProps) => {
    const context = useContext(TabsContext);
    if (!context) throw new Error("TabsContent must be used within Tabs");

    if (context.activeTab !== value) return null;

    return (
        <div
            className={mergeClassNames(
                "mt-2 ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
                className
            )}
            {...props}
        >
            {children}
        </div>
    );
};
