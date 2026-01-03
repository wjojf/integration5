import {useState } from "react"
import { useLocation, Outlet } from "react-router-dom"
import { useKeycloak } from "@react-keycloak/web";

import type { NavItemId } from '../../config/app.config'
import { FeatureComponents, LayoutComponents } from '../../components/app'
import { NAV_ITEMS } from '../../config/app.config'

const { Chatbot } = FeatureComponents
const { Header, Sidebar } = LayoutComponents

export const Platform = () => {
    const { keycloak } = useKeycloak();
    const { pathname } = useLocation();
    const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

    const userRoles = keycloak.tokenParsed?.realm_access?.roles
    const currentViewPath = pathname.split("/")[2]
    const currentViewId = NAV_ITEMS.map(item => item.id).includes(currentViewPath as NavItemId)
        ? currentViewPath
        : 'games'

    return (
        <div>
            <Header/>

            <div className="min-h-screen bg-background flex">
                <Sidebar
                    currentViewId={currentViewId as string}
                    isSidebarCollapsed={isSidebarCollapsed as boolean}
                    setIsSidebarCollapsed={setIsSidebarCollapsed}
                    isAdmin={userRoles?.includes('admin')}
                />

                <main className="flex-1 overflow-auto">
                    <div className="p-8">
                        <Outlet/>
                    </div>
                </main>
            </div>

            <Chatbot />
        </div>
    );
}

