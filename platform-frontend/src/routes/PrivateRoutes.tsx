import { JSX } from 'react'
import { useKeycloak } from '@react-keycloak/web'
import { Outlet } from 'react-router-dom'

export const PrivateRoutes = (): JSX.Element => {
    const { keycloak, initialized } = useKeycloak()

    if(!initialized){
        return (<div>Checking authentication...</div>)
    }

    if(!keycloak.authenticated){
        keycloak.login({ redirectUri: `${window.location.origin}/app/games`})
        return <></>
    }

    return <Outlet/>
}

