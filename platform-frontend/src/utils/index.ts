import { clsx, ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import moment from "moment";

export const mergeClassNames = (...inputs: ClassValue[]) => twMerge(clsx(...inputs));

export const formatDate = (value: string | Date, format: string = 'MMMM DD, YYYY'): string => 
    value && moment(value).utc(false).format(format);

