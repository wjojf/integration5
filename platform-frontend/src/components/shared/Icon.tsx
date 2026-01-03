import { ComponentPropsWithoutRef } from "react"
import { mergeClassNames } from "../../utils"

type IconProps = ComponentPropsWithoutRef<"img"> & {
    src: string
    imageAlt: string
    href?: string
}

export const Icon = ({ className, src, imageAlt, href, ...props }: IconProps) => {
    const Img = (
        <img
            src={src}
            alt={imageAlt}
            className={mergeClassNames('w-10 h-10 text-primary-foreground', className)}
            {...props}
        />
    )

    return href ? <a href={href}>{Img}</a> : Img
};

