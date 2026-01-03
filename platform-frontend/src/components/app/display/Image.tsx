import {useState, ComponentPropsWithoutRef} from 'react'
import errorImage from '../../../assets/error.svg'

type ImageProps = ComponentPropsWithoutRef<"img"> & {
    src: string
    alt: string
}

export const Image = ({ src, alt, className, ...rest }: ImageProps) => {
    const [didError, setDidError] = useState(false)

    const imageElement = (
        <img src={src} alt={alt} className={className} {...rest} onError={() => setDidError(true)}/>
    )
    const errorElement = (
        <div className={`inline-block bg-gray-100 text-center align-middle ${className ?? ''}`}>
            <div className="flex items-center justify-center w-full h-full">
                <img src={errorImage} alt="Error loading image" {...rest} data-original-url={src}/>
            </div>
        </div>
    )

    return didError ? errorElement : imageElement
}

