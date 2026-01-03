import { JSX } from 'react'
import { LogOut } from "lucide-react"
import {useKeycloak} from "@react-keycloak/web";

import { Icon } from "../../shared";
import iconImg from "../../../assets/icon.png";
import { Avatar, AvatarFallback } from '../display/Avatar'
import { Button } from '../input/Button'

export const Header = (): JSX.Element => {
    const { keycloak } = useKeycloak()

    const userFullName = keycloak.tokenParsed?.name
    const userInitials = keycloak.tokenParsed?.given_name?.[0] + keycloak.tokenParsed?.family_name?.[0]
    const logout = () => keycloak.logout({redirectUri: `${window.location.origin}`})

    return (<header className="header">
        <div className="header__left">
            <a href="#" className="logo group">
                <div className="logo__mark group-hover:scale-105 transition-transform">
                    <Icon src={iconImg} imageAlt={'BanditGames Icons'} className="rounded"/>
                </div>

                <div className="flex flex-col gap-0.5">
                            <span className="logo__text tracking-[0.18em] uppercase">
                                BanditGames
                            </span>
                    <span className="text-[0.7rem] text-muted-foreground">
                                Your Gaming Platform
                            </span>
                </div>
            </a>
        </div>

        <div className="header__right gap-4">
            <div className="flex flex-col items-end leading-tight">
                        <span className="text-sm font-semibold text-foreground">{userFullName}</span>
            </div>
            <div className="h-8 w-px bg-border opacity-60"></div>
            <Avatar className="h-9 w-9 border border-border/70 bg-card shadow-sm hover:border-accent transition-colors">
                <AvatarFallback>{userInitials}</AvatarFallback>
            </Avatar>

            <Button
                onClick={() => logout()}
                variant="ghost"
                size="icon"
                className="h-9 w-9 rounded-full border border-transparent hover:border-destructive/50 hover:bg-destructive/10 hover:text-destructive transition-colors"
            >
                <LogOut className="w-4 h-4"/>
            </Button>
        </div>
    </header>)
}
