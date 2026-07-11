import React, {useEffect, useMemo, useState} from 'react';
import {ConfigProvider, theme as antdTheme} from 'antd';
import {Link, Outlet, useLocation, useMatches} from 'react-router';
import {useTranslation} from 'react-i18next';
import {
    ChevronRightIcon,
    ChevronsUpDownIcon,
    Command,
    Gauge,
    Globe,
    KeyRound,
    LayoutGrid,
    LogOut,
    Settings,
    ShieldCheck,
    User as UserIcon,
    Users,
} from 'lucide-react';

import {ShieldOAuth} from '@/icons';

import {Avatar, AvatarFallback} from '@/components/ui/avatar';
import {
    Breadcrumb,
    BreadcrumbItem,
    BreadcrumbLink,
    BreadcrumbList,
    BreadcrumbPage,
    BreadcrumbSeparator,
} from '@/components/ui/breadcrumb';
import {
    Collapsible,
    CollapsibleContent,
    CollapsibleTrigger,
} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Separator} from '@/components/ui/separator';
import {Skeleton} from '@/components/ui/skeleton';
import {
    Sidebar,
    SidebarContent,
    SidebarFooter,
    SidebarGroup,
    SidebarGroupContent,
    SidebarHeader,
    SidebarInset,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarMenuSub,
    SidebarMenuSubButton,
    SidebarMenuSubItem,
    SidebarProvider,
    SidebarTrigger,
    useSidebar,
} from '@/components/ui/sidebar';
import {TooltipProvider} from '@/components/ui/tooltip';

import {SUPPORTED_LOCALES, getAntdLocale, setLocale} from '../i18n';
import LogoutConfirmModal from './LogoutConfirmModal';
import {PageTitleContext} from './_shared/pageTitle';

const FALLBACK_SITE_NAME = 'User Center';

/**
 * Resolve the site name injected by the backend Thymeleaf template
 * via <meta name="site-name" th:content="${euler.ctx.__SITE_NAME}"/>.
 * Falls back to the literal default during pure frontend dev runs.
 */
const getSiteName = () => {
    if (typeof document === 'undefined') return FALLBACK_SITE_NAME;
    const meta = document.querySelector('meta[name="site-name"]');
    const value = meta?.getAttribute('content')?.trim();
    return value && value.length > 0 ? value : FALLBACK_SITE_NAME;
};

const SITE_NAME = getSiteName();

/**
 * Menu tree consumed by the sidebar. Icons are Lucide components so
 * they inherit `currentColor` from SidebarMenuButton and gracefully
 * shrink to 16px in icon-collapsed mode.
 */
const buildMenuItems = (t) => [
    {
        key: '/dashboard',
        icon: Gauge,
        label: t('nav.dashboard'),
        to: '/dashboard',
    },
    {
        key: 'identity',
        icon: Users,
        label: t('nav.iam'),
        children: [
            {key: '/users', icon: UserIcon, label: t('nav.user'), to: '/users'},
            {key: '/roles', icon: ShieldCheck, label: t('nav.role'), to: '/roles'},
        ],
    },
    {
        key: 'oauth2',
        icon: ShieldOAuth,
        label: t('nav.oauth2'),
        children: [
            {
                key: '/oauth2/clients',
                icon: LayoutGrid,
                label: t('nav.oauth2_client'),
                to: '/oauth2/clients',
            },
            {
                key: '/oauth2/jwks',
                icon: KeyRound,
                label: t('nav.oauth2_jwk'),
                to: '/oauth2/jwks',
            },
        ],
    },
    {
        key: '/settings',
        icon: Settings,
        label: t('nav.settings'),
        to: '/settings',
    },
];

/**
 * Resolve currently selected menu key and its parent submenu key
 * based on the current location pathname. Longest-prefix wins so
 * that a nested route like `/users/:id` still selects `/users`.
 */
const resolveSelection = (pathname, items) => {
    const flatten = (list, parent) => {
        const acc = [];
        for (const item of list) {
            if (item.children) {
                acc.push(...flatten(item.children, item));
            } else {
                acc.push({leaf: item, parent});
            }
        }
        return acc;
    };
    const leaves = flatten(items, null);
    const matched = leaves
        .filter((it) => pathname === it.leaf.key || pathname.startsWith(it.leaf.key + '/'))
        .sort((a, b) => b.leaf.key.length - a.leaf.key.length)[0];
    if (!matched) {
        return {selectedKey: null, openKey: null, parent: null, leaf: null};
    }
    return {
        selectedKey: matched.leaf.key,
        openKey: matched.parent?.key ?? null,
        parent: matched.parent,
        leaf: matched.leaf,
    };
};

/**
 * useBreadcrumbTrail — derive the admin breadcrumb from React Router
 * matches plus the menu tree. Feature pages contribute zero code: each
 * titled route declares its label via `handle.title` (an i18n key) in
 * the route table (see `main.jsx`); the menu tree contributes the
 * top-level parent-group segment.
 *
 * Trail entries (rendered by InsetHeader):
 *   - `{kind: 'parent', label, children}`  — menu parent group; the
 *     separator that follows becomes a DropdownMenu switcher listing
 *     every child of this group.
 *   - `{kind: 'link',   label, to}`        — titled ancestor route;
 *     rendered as a BreadcrumbLink for back-navigation.
 *   - `{kind: 'page',   label}`            — current page; rendered
 *     as BreadcrumbPage. `override` (from `usePageTitle`) replaces its
 *     label when set; otherwise falls back to `t(handle.title)`.
 *
 * State preservation:
 * `SEARCH_CACHE` remembers each titled route's last-seen
 * `location.search`, so returning via a link crumb restores the exact
 * URL the user last stood on (e.g. `/users?page=3` survives round-
 * trips through `/users/:userId`). The cache updates only when the
 * user currently stands on a titled route (`match.pathname ===
 * location.pathname`), so descending into a child never overwrites
 * its parent's remembered `search`. It lives at module scope on
 * purpose — the console is a single-session app and re-mounting the
 * layout should not blow away in-session history; a full page reload
 * (fresh module import) is the natural reset event.
 */
const SEARCH_CACHE = new Map();

const useBreadcrumbTrail = (menuItems, override) => {
    const matches = useMatches();
    const location = useLocation();
    const {t} = useTranslation();
    useEffect(() => {
        for (const m of matches) {
            if (m.handle?.title && m.pathname === location.pathname) {
                SEARCH_CACHE.set(m.pathname, location.search);
            }
        }
    }, [matches, location.pathname, location.search]);
    return useMemo(() => {
        const titled = matches.filter((m) => m.handle?.title);
        if (titled.length === 0) return [];
        const withUrl = (path) => path + (SEARCH_CACHE.get(path) ?? '');
        const trail = [];
        const {parent} = resolveSelection(titled[0].pathname, menuItems);
        if (parent) {
            trail.push({
                kind: 'parent',
                label: parent.label,
                children: parent.children.map((child) => ({
                    label: child.label,
                    to: withUrl(child.to),
                    icon: child.icon,
                    isActive: titled.some((m) => m.pathname === child.to),
                })),
            });
        }
        titled.forEach((m, idx) => {
            const isLast = idx === titled.length - 1;
            const label = isLast && override ? override : t(m.handle.title);
            trail.push({
                kind: isLast ? 'page' : 'link',
                label,
                to: isLast ? null : withUrl(m.pathname),
            });
        });
        return trail;
    }, [matches, menuItems, override, t]);
};

/**
 * NavMainGroupItem — a top-level menu item with children. Rendering
 * splits by sidebar state:
 *
 * - Expanded desktop / mobile sheet: inline `Collapsible` where the
 *   entire parent row is the trigger (click anywhere to expand).
 * - Collapsed desktop (state === 'collapsed'): the parent tile becomes
 *   a `DropdownMenu` trigger and the children pop out to the right so
 *   they remain reachable from the 3rem icon-rail. Without this branch,
 *   the collapsible children stay hidden by `group-data-[collapsible=icon]:hidden`
 *   on `SidebarMenuSub` and there is no way to reach `/users`, `/roles`,
 *   `/oauth2/clients`, etc. while the sidebar is folded.
 *
 * `state` on mobile still reflects the persisted desktop state (see
 * SidebarProvider), so gate the dropdown branch on `!isMobile` — the
 * mobile sheet always renders fully expanded and should keep the
 * inline collapsible.
 */
const NavMainGroupItem = ({item, selectedKey, openKey}) => {
    const {state, isMobile} = useSidebar();
    const Icon = item.icon;
    const parentActive = item.children.some((c) => c.key === selectedKey);
    const defaultOpen = openKey === item.key;

    if (state === 'collapsed' && !isMobile) {
        return (
            <SidebarMenuItem>
                <DropdownMenu>
                    <DropdownMenuTrigger
                        render={
                            <SidebarMenuButton
                                isActive={parentActive}
                                tooltip={item.label}
                                className="aria-expanded:bg-sidebar-accent aria-expanded:text-sidebar-accent-foreground"
                            />
                        }
                    >
                        <Icon/>
                        <span>{item.label}</span>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent
                        side="right"
                        align="start"
                        sideOffset={4}
                        className="min-w-48 rounded-lg"
                    >
                        <DropdownMenuGroup>
                            <DropdownMenuLabel>{item.label}</DropdownMenuLabel>
                        </DropdownMenuGroup>
                        <DropdownMenuSeparator/>
                        <DropdownMenuGroup>
                            {item.children.map((child) => {
                                const ChildIcon = child.icon;
                                return (
                                    <DropdownMenuItem
                                        key={child.key}
                                        render={<Link to={child.to}/>}
                                    >
                                        <ChildIcon/>
                                        <span>{child.label}</span>
                                    </DropdownMenuItem>
                                );
                            })}
                        </DropdownMenuGroup>
                    </DropdownMenuContent>
                </DropdownMenu>
            </SidebarMenuItem>
        );
    }

    // The parent row itself is the collapsible trigger — the chevron
    // on the right is a decorative child that rotates via
    // `group-aria-expanded/menu-button:rotate-90`. SidebarMenuButton's
    // base className already carries `group/menu-button`, and Base UI
    // sets `aria-expanded="true"` on the trigger element when the
    // panel is open, so the child chevron follows the state through
    // that named group.
    return (
        <Collapsible
            defaultOpen={defaultOpen}
            render={<SidebarMenuItem/>}
        >
            <CollapsibleTrigger
                render={
                    <SidebarMenuButton
                        isActive={parentActive}
                        tooltip={item.label}
                    />
                }
            >
                <Icon/>
                <span>{item.label}</span>
                <ChevronRightIcon
                    className="ml-auto transition-transform duration-200 group-aria-expanded/menu-button:rotate-90"
                />
            </CollapsibleTrigger>
            <CollapsibleContent>
                <SidebarMenuSub>
                    {item.children.map((child) => {
                        const ChildIcon = child.icon;
                        return (
                            <SidebarMenuSubItem key={child.key}>
                                <SidebarMenuSubButton
                                    isActive={selectedKey === child.key}
                                    render={<Link to={child.to}/>}
                                >
                                    <ChildIcon/>
                                    <span>{child.label}</span>
                                </SidebarMenuSubButton>
                            </SidebarMenuSubItem>
                        );
                    })}
                </SidebarMenuSub>
            </CollapsibleContent>
        </Collapsible>
    );
};

/**
 * NavMain — mirrors the official sidebar-08 nav-main.jsx: each top-level
 * item is a SidebarMenuButton rendered as a link; items with children
 * are delegated to `NavMainGroupItem` which picks inline-collapsible vs.
 * dropdown-popover based on the current sidebar state.
 */
const NavMain = ({items, selectedKey, openKey}) => (
    <SidebarGroup>
        <SidebarMenu>
            {items.map((item) => {
                const Icon = item.icon;
                if (!item.children) {
                    return (
                        <SidebarMenuItem key={item.key}>
                            <SidebarMenuButton
                                isActive={selectedKey === item.key}
                                tooltip={item.label}
                                render={<Link to={item.to}/>}
                            >
                                <Icon/>
                                <span>{item.label}</span>
                            </SidebarMenuButton>
                        </SidebarMenuItem>
                    );
                }
                return (
                    <NavMainGroupItem
                        key={item.key}
                        item={item}
                        selectedKey={selectedKey}
                        openKey={openKey}
                    />
                );
            })}
        </SidebarMenu>
    </SidebarGroup>
);

/**
 * NavLocale — sidebar-08 nav-secondary slot (`mt-auto` pins to bottom).
 * A single SidebarMenuButton that opens a DropdownMenu with the
 * supported locales as a radio group. No chevron; the icon + label are
 * enough affordance and the user asked for a plain button.
 */
const NavLocale = ({className}) => {
    const {t, i18n} = useTranslation();
    const {isMobile} = useSidebar();
    const currentLabel = t(`language.${i18n.language}`);
    return (
        <SidebarGroup className={className}>
            <SidebarGroupContent>
                <SidebarMenu>
                    <SidebarMenuItem>
                        <DropdownMenu>
                            <DropdownMenuTrigger
                                render={
                                    <SidebarMenuButton
                                        size="sm"
                                        tooltip={currentLabel}
                                        className="aria-expanded:bg-muted"
                                    />
                                }
                            >
                                <Globe/>
                                <span>{currentLabel}</span>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent
                                className="min-w-40 rounded-lg"
                                side={isMobile ? 'bottom' : 'right'}
                                align="end"
                                sideOffset={4}
                            >
                                <DropdownMenuRadioGroup
                                    value={i18n.language}
                                    onValueChange={setLocale}
                                >
                                    {SUPPORTED_LOCALES.map((tag) => (
                                        <DropdownMenuRadioItem key={tag} value={tag}>
                                            {t(`language.${tag}`)}
                                        </DropdownMenuRadioItem>
                                    ))}
                                </DropdownMenuRadioGroup>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </SidebarMenuItem>
                </SidebarMenu>
            </SidebarGroupContent>
        </SidebarGroup>
    );
};

/**
 * NavUser — verbatim shape of the official sidebar-08 nav-user.jsx,
 * only substituting the trigger's children with the current principal's
 * identity + a "console" subtitle (email column is not exposed by the
 * /user endpoint). The visual bits (Avatar/text/chevron) sit as
 * children of DropdownMenuTrigger — that's the pattern shadcn ships;
 * do not move them inside the SidebarMenuButton render element.
 *
 * Loading state: while the parent hasn't resolved `/user` yet, the
 * identity tile renders skeleton placeholders sized to match the
 * loaded content so nothing shifts when data lands. The trigger stays
 * clickable throughout (see plan); the dropdown label shows the same
 * skeleton so its visual continuity holds even if the user opens the
 * menu during the brief loading window.
 */
const NavUser = ({currentUser, loading, onLogout}) => {
    const {t} = useTranslation();
    const {isMobile} = useSidebar();
    const username = currentUser?.username ?? '';
    const initial = username?.[0]?.toUpperCase() ?? '';
    const roleLine = useMemo(() => {
        const authorities = currentUser?.authorities;
        if (Array.isArray(authorities) && authorities.length > 0) {
            const primary = authorities[0];
            const key = `user.role.${primary.authority}`;
            const translated = t(key);
            return translated === key ? (primary.name ?? primary.authority) : translated;
        }
        return t('header.console');
    }, [currentUser, t]);

    const displayName = username || t('header.profile');

    return (
        <SidebarMenu>
            <SidebarMenuItem>
                <DropdownMenu>
                    <DropdownMenuTrigger
                        render={
                            <SidebarMenuButton
                                size="lg"
                                className="aria-expanded:bg-muted"
                            />
                        }
                    >
                        <UserIdentityTile
                            loading={loading}
                            initial={initial}
                            displayName={displayName}
                            roleLine={roleLine}
                        />
                        <ChevronsUpDownIcon className="ml-auto size-4"/>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent
                        className="min-w-56 rounded-lg"
                        side={isMobile ? 'bottom' : 'right'}
                        align="end"
                        sideOffset={4}
                    >
                        {/*
                          Base UI requires MenuGroupLabel to be inside a
                          MenuGroup (throws "MenuGroupContext is missing"
                          otherwise). Mirror the official sidebar-08
                          nav-user.jsx wrapping exactly.
                        */}
                        <DropdownMenuGroup>
                            <DropdownMenuLabel className="p-0 font-normal">
                                <div className="flex items-center gap-2 px-1 py-1.5 text-left text-sm">
                                    <UserIdentityTile
                                        loading={loading}
                                        initial={initial}
                                        displayName={displayName}
                                        roleLine={roleLine}
                                    />
                                </div>
                            </DropdownMenuLabel>
                        </DropdownMenuGroup>
                        <DropdownMenuSeparator/>
                        <DropdownMenuGroup>
                            <DropdownMenuItem>
                                <UserIcon/>
                                {t('header.profile')}
                            </DropdownMenuItem>
                        </DropdownMenuGroup>
                        <DropdownMenuSeparator/>
                        <DropdownMenuItem variant="destructive" onClick={onLogout}>
                            <LogOut/>
                            {t('header.logout')}
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </SidebarMenuItem>
        </SidebarMenu>
    );
};

/**
 * UserIdentityTile — shared "avatar + two-line label" fragment used by
 * both the NavUser trigger and its dropdown menu label. Splitting it
 * out keeps skeleton dimensions in one place, so both surfaces stay
 * pixel-identical between loading and loaded states.
 *
 * Skeleton sizing rationale:
 * - Avatar block: `size-8 rounded-lg` matches the loaded `<Avatar>`.
 * - Name line: `h-3.5 w-24` (14px) fills a `text-sm leading-tight`
 *   row (~17.5px) without overshooting; typical username length.
 * - Subtitle line: `h-3 w-16` (12px) matches `text-xs`.
 * - Outer `grid gap-1` keeps overall tile height within ~2px of the
 *   loaded state; `animate-pulse` masks the residual delta.
 */
const UserIdentityTile = ({loading, initial, displayName, roleLine}) => {
    if (loading) {
        return (
            <>
                <Skeleton className="size-8 rounded-lg"/>
                <div className="grid flex-1 gap-1 text-left text-sm leading-tight">
                    <Skeleton className="h-3.5 w-24"/>
                    <Skeleton className="h-3 w-16"/>
                </div>
            </>
        );
    }
    return (
        <>
            {/*
              `after:rounded-lg` overrides shadcn Avatar's decorative
              `::after` border overlay, which is `after:rounded-full`
              in the base string. Aligning the pseudo-element's radius
              restores the squircle look used in the shadcn nav-user
              reference.
            */}
            <Avatar className="size-8 rounded-lg after:rounded-lg">
                <AvatarFallback className="rounded-lg">
                    {initial || <UserIcon className="size-4"/>}
                </AvatarFallback>
            </Avatar>
            <div className="grid flex-1 text-left text-sm leading-tight">
                <span className="truncate font-medium">{displayName}</span>
                <span className="truncate text-xs">{roleLine}</span>
            </div>
        </>
    );
};

/**
 * AppSidebar — default `sidebar` variant (not `inset`) so the icon
 * rail sits at `--sidebar-width-icon` = 3rem = 48px, matching the
 * sidebar-09 reference. The sidebar-08 floating-island look for the
 * content on the right is applied manually to `SidebarInset` below
 * rather than via `variant="inset"` on this component, which would
 * inflate the rail width with `p-2` gutters when collapsed.
 */
const AppSidebar = ({menuItems, selectedKey, openKey, currentUser, userLoading, onLogout, ...props}) => (
    // `!border-r-0` cancels the `border-r` the default `sidebar` variant
    // paints on the container. That border makes visual sense when the
    // content is flush against the sidebar, but with the manual
    // floating-island styling applied below (SidebarInset `m-2 ml-0`)
    // the line ends up floating in the sidebar-tinted gutter and looks
    // like a stray seam next to the rounded content card.
    <Sidebar collapsible="icon" className="!border-r-0" {...props}>
        <SidebarHeader>
            <SidebarMenu>
                <SidebarMenuItem>
                    <SidebarMenuButton size="lg" render={<Link to="/"/>}>
                        <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                            <Command className="size-4"/>
                        </div>
                        <div className="grid flex-1 text-left text-sm leading-tight">
                            <span className="truncate font-medium">{SITE_NAME}</span>
                            <span className="truncate text-xs">Admin Console</span>
                        </div>
                    </SidebarMenuButton>
                </SidebarMenuItem>
            </SidebarMenu>
        </SidebarHeader>
        <SidebarContent>
            <NavMain items={menuItems} selectedKey={selectedKey} openKey={openKey}/>
            <NavLocale className="mt-auto"/>
        </SidebarContent>
        <SidebarFooter>
            <NavUser currentUser={currentUser} loading={userLoading} onLogout={onLogout}/>
        </SidebarFooter>
    </Sidebar>
);

/**
 * Slim inset header (sidebar-07 pattern): SidebarTrigger + Separator +
 * Breadcrumb. Height animates from 4rem down to 3rem when the sidebar
 * collapses to icons.
 *
 * Trail composition lives entirely in `useBreadcrumbTrail`; this
 * component is a pure renderer. Each `crumb.kind` maps to a fixed
 * primitive; the separator between two crumbs upgrades to a
 * DropdownMenu switcher iff the crumb on its left is a menu parent
 * group (i.e. carries `children`).
 */
const InsetHeader = ({trail}) => (
    <header className="flex h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-12">
        <div className="flex items-center gap-2 px-4">
            <SidebarTrigger className="-ml-1"/>
            <Separator
                orientation="vertical"
                className="mr-2 data-[orientation=vertical]:h-4"
            />
            <Breadcrumb>
                <BreadcrumbList>
                    {trail.map((crumb, idx) => (
                        <React.Fragment key={`${idx}-${crumb.label}`}>
                            {idx > 0 && (
                                <BreadcrumbSeparatorSlot leftCrumb={trail[idx - 1]}/>
                            )}
                            <BreadcrumbItem className={crumb.kind === 'page' ? undefined : 'hidden md:block'}>
                                {crumb.kind === 'page' ? (
                                    <BreadcrumbPage>{crumb.label}</BreadcrumbPage>
                                ) : crumb.kind === 'link' ? (
                                    <BreadcrumbLink render={<Link to={crumb.to}/>}>
                                        {crumb.label}
                                    </BreadcrumbLink>
                                ) : (
                                    crumb.label
                                )}
                            </BreadcrumbItem>
                        </React.Fragment>
                    ))}
                </BreadcrumbList>
            </Breadcrumb>
        </div>
    </header>
);

/**
 * Breadcrumb separator slot. Renders the default decorative chevron
 * unless the crumb on its left is a menu parent group (has
 * `children`), in which case the chevron becomes a DropdownMenu
 * trigger that lists every sibling entry under that parent. Each
 * sibling link honours the layout-scoped search cache so switching
 * to a previously-visited list restores its `?query` state.
 */
const BreadcrumbSeparatorSlot = ({leftCrumb}) => {
    const {t} = useTranslation();
    if (leftCrumb.kind !== 'parent' || !leftCrumb.children?.length) {
        return <BreadcrumbSeparator className="hidden md:block"/>;
    }
    return (
        <BreadcrumbSeparator aria-hidden="false" className="hidden md:inline-flex">
            <DropdownMenu>
                <DropdownMenuTrigger
                    className="inline-flex size-5 items-center justify-center rounded-sm text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring aria-expanded:bg-accent aria-expanded:text-accent-foreground"
                    aria-label={t('breadcrumb.expand', {group: leftCrumb.label})}
                >
                    <ChevronRightIcon className="size-3.5"/>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="start" sideOffset={4}>
                    {leftCrumb.children.map((child) => {
                        const ChildIcon = child.icon;
                        return (
                            <DropdownMenuItem
                                key={child.to}
                                data-active={child.isActive || undefined}
                                render={<Link to={child.to}/>}
                            >
                                {ChildIcon ? <ChildIcon/> : null}
                                <span>{child.label}</span>
                            </DropdownMenuItem>
                        );
                    })}
                </DropdownMenuContent>
            </DropdownMenu>
        </BreadcrumbSeparator>
    );
};

const AdminLayout = () => {
    const [logoutOpen, setLogoutOpen] = useState(false);
    const [currentUser, setCurrentUser] = useState(null);
    // `userLoading` gates the NavUser skeleton. Distinguishing "still
    // fetching" from "loaded but unauthenticated" matters: only the
    // first shows a skeleton; the second falls back to the static
    // `header.profile / header.console` text so the footer never
    // pulses forever when the user isn't signed in.
    const [userLoading, setUserLoading] = useState(true);
    // Runtime page-title override channel. Feature pages publish a
    // dynamic breadcrumb label via `usePageTitle(label)` (see
    // `_shared/pageTitle`) and get automatic cleanup on unmount;
    // AdminLayout owns the state so the trail can read it without
    // going through context.
    const [pageTitleOverride, setPageTitleOverride] = useState(null);
    // Only the setter is exposed via context. Freezing this once with
    // an empty dependency array means children's `usePageTitle` effect
    // never re-runs due to layout re-renders.
    const pageTitleCtx = useMemo(() => ({setValue: setPageTitleOverride}), []);
    const location = useLocation();
    const {t, i18n} = useTranslation();
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const menuItems = useMemo(() => buildMenuItems(t), [t, i18n.language]);
    const {selectedKey, openKey} = useMemo(
        () => resolveSelection(location.pathname, menuItems),
        [location.pathname, menuItems]
    );
    const trail = useBreadcrumbTrail(menuItems, pageTitleOverride);

    // Fetch the current principal once on mount. The /user endpoint
    // returns the EulerUserDetails of the authenticated user (with
    // credentials erased server-side); username is rendered in the
    // sidebar footer's user tile.
    useEffect(() => {
        let cancelled = false;
        fetch('/user', {headers: {Accept: 'application/json'}})
            .then((res) => (res.ok ? res.json() : null))
            .then((data) => {
                if (cancelled) return;
                if (data) setCurrentUser(data);
                setUserLoading(false);
            })
            .catch(() => {
                /* keep currentUser null; footer falls back to a blank avatar */
                if (!cancelled) setUserLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, []);

    return (
        <ConfigProvider
            locale={getAntdLocale(i18n.language)}
            theme={{
                algorithm: antdTheme.defaultAlgorithm,
                token: {
                    colorPrimary: '#1677ff',
                    borderRadius: 6,
                },
            }}
        >
            <TooltipProvider>
                {/*
                  Wrapper tint (`bg-sidebar`) fills the gutter behind
                  the sidebar and around the floating content card.
                  Injected here because the default `sidebar` variant
                  doesn't paint the outer canvas itself — the `inset`
                  variant would, but we opt out of it above for rail
                  width reasons.
                */}
                <SidebarProvider className="bg-sidebar">
                    <AppSidebar
                        menuItems={menuItems}
                        selectedKey={selectedKey}
                        openKey={openKey}
                        currentUser={currentUser}
                        userLoading={userLoading}
                        onLogout={() => setLogoutOpen(true)}
                    />
                    {/*
                      Floating card look for the content, applied
                      manually because shadcn's sidebar.jsx gates the
                      corresponding classes on `peer-data-[variant=inset]`
                      which we can't fire (see AppSidebar doc).

                      `ml-0` in the collapsed state is deliberate: an
                      8px left margin would let the `bg-sidebar` tint
                      show through as a gutter between the rail and
                      the card, widening the perceived sidebar area
                      from 48px to 56px and shifting icons 4px left of
                      the visual center. Flush against the rail keeps
                      the icons geometrically centered.

                      `min-w-0` is critical: SidebarInset is a flex
                      child with `flex-1 w-full`; without a min-width
                      override, its intrinsic content (antd Tables
                      with 5 columns) prevents flex-shrink and the
                      card grows past the viewport.
                    */}
                    <SidebarInset className="min-w-0 md:m-2 md:ml-0 md:rounded-xl md:shadow-sm">
                        <InsetHeader trail={trail}/>
                        {/*
                          Feature pages own their own surface and don't
                          touch the breadcrumb — the layout derives it
                          from route `handle.title`. Pages that need a
                          dynamic label call `usePageTitle` (from
                          `_shared/pageTitle`) which publishes into
                          `PageTitleContext` below. Inset padding matches
                          the sidebar-07/-08 reference (`p-4 pt-0`).
                        */}
                        <main className="flex flex-1 flex-col gap-4 p-4 pt-0">
                            <PageTitleContext.Provider value={pageTitleCtx}>
                                <Outlet/>
                            </PageTitleContext.Provider>
                        </main>
                    </SidebarInset>
                </SidebarProvider>
                <LogoutConfirmModal
                    open={logoutOpen}
                    onCancel={() => setLogoutOpen(false)}
                />
            </TooltipProvider>
        </ConfigProvider>
    );
};

export default AdminLayout;
