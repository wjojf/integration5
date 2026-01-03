import { JSX } from 'react'
import { Link } from "react-router-dom";
import { ChevronRight, ChevronLeft } from "lucide-react"

import { NAV_ITEMS } from '../../../config/app.config'
import { Button } from "../input/Button";

type SidebarProps = {
    currentViewId: string,
    isSidebarCollapsed: boolean,
    setIsSidebarCollapsed: Function,
    isAdmin?: boolean
}

export const Sidebar = (props: SidebarProps): JSX.Element => {
    const { currentViewId, isSidebarCollapsed, setIsSidebarCollapsed, isAdmin } = props

    return (<aside
        className={`bg-card border-r border-border flex flex-col transition-all duration-200 
                    ${isSidebarCollapsed ? "w-20" : "w-64"}`}
    >
        <nav className="p-4 flex-1">
            <div className="space-y-2">{NavItems(currentViewId, isSidebarCollapsed, isAdmin)}</div>
            <div className="mt-4 flex items-center justify-center">
                <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 rounded-full bg-gray-800"
                    onClick={() => setIsSidebarCollapsed((prev: boolean) => !prev)}
                >
                    {isSidebarCollapsed
                        ? (<ChevronRight className="w-4 h-4"/>)
                        : (<ChevronLeft className="w-4 h-4"/>)
                    }
                    <span className="sr-only">Toggle sidebar</span>
                </Button>
            </div>
        </nav>
    </aside>)
}

const NavItems = (currentViewId: string, isSidebarCollapsed: boolean, isAdmin?: boolean): JSX.Element[] => {
    const filteredNavItems = isAdmin ? NAV_ITEMS : NAV_ITEMS.filter(item => !['analytics'].includes(item.id))

    return filteredNavItems.map((item) => {
        const Icon = item.icon;
        const isActive = currentViewId === item.id;
        const viewPath = `/app/${item.id}`

        return (
            <Link key={item.id} to={viewPath}>
                <Button
                    variant={isActive ? "default" : "ghost"}
                    className={`w-full py-6 transition-all ${isSidebarCollapsed ? "justify-center px-0" : "justify-start"}`}
                >
                    <Icon className={`w-16 h-16 ${isSidebarCollapsed ? "" : "mr-2.5"}`}/>
                    <span className={isSidebarCollapsed ? "sr-only" : ""}>{item.label}</span>
                </Button>
            </Link>
        );
    })
}

