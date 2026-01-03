import {ReactNode} from "react";

type FeatureCardProps = {
    icon: ReactNode
    title: string
    description: string
}

export const FeatureCard = ({ icon, title, description }: FeatureCardProps) => {
    return (
        <div
            className="rounded-xl border border-border bg-card/60 p-6 hover:bg-card/80 hover:border-primary/40 transition-all group">
            <div
                className="inline-flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10 border border-primary/40 text-primary mb-4 group-hover:bg-primary/20 transition-colors">
                {icon}
            </div>
            <h3 className="text-base mb-2">{title}</h3>
            <p className="text-sm text-muted-foreground leading-relaxed">{description}</p>
        </div>
    );
}
