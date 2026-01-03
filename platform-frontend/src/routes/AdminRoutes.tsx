import { JSX } from 'react'
import { useKeycloak } from '@react-keycloak/web'
import { Outlet, Navigate } from "react-router-dom";

export const AdminRoutes = (): JSX.Element => {
    const { keycloak } = useKeycloak()
    const userRoles = keycloak.tokenParsed?.realm_access?.roles

    if(!userRoles?.includes('admin')){
        return <Navigate to="/app/games" replace />
    }

    return <Outlet/>
}
