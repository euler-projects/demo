import React, {useEffect, useMemo, useRef, useState} from 'react';
import type {ReactElement} from 'react';
import {ConfigProvider, theme as antdTheme} from 'antd';
import {Link, Outlet, useLocation, useMatches} from 'react-router';
import {useTranslation} from 'react-i18next';
import type {TFunction} from 'i18next';
import type {LucideIcon} from 'lucide-react';
import {
    ChevronRightIcon,
    ChevronsUpDownIcon,
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

import {Brand, ShieldLock} from '@/icons';
import {cn} from '@/lib/utils';

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
// Console-scoped CSS travels with the console (sidebar icon-rail
// padding fix etc.), keeping index.css neutral.
import './ConsoleLayout.css';

const FALLBACK_SITE_NAME = 'User Center';

/**
 * Resolve the site name injected by the backend Thymeleaf template
 * via <meta name="site-name" th:content="${euler.ctx.__SITE_NAME}"/>.
 * Falls back to the literal default during pure frontend dev runs.
 */
const getSiteName = (): string => {
    if (typeof document === 'undefined') return FALLBACK_SITE_NAME;
    const meta = document.querySelector('meta[name="site-name"]');
    const value = meta?.getAttribute('content')?.trim();
    return value && value.length > 0 ? value : FALLBACK_SITE_NAME;
};

const SITE_NAME = getSiteName();

/** Menu tree node types consumed by the sidebar and breadcrumb derivation. */
interface MenuLeaf {
    key: string;
    icon: LucideIcon;
    label: string;
    to: string;
}

interface MenuGroup {
    key: string;
    icon: LucideIcon;
    label: string;
    children: MenuLeaf[];
}

type MenuItem = MenuLeaf | MenuGroup;

interface CurrentUser {
    username: string;
    authorities: Array<{authority: string; name?: string}>;
}

/** Breadcrumb trail entries rendered by InsetHeader. */
interface TrailParentChild {
    label: string;
    to: string;
    icon: LucideIcon;
    isActive: boolean;
}

type TrailCrumb =
    | {kind: 'parent'; label: string; children: TrailParentChild[]}
    | {kind: 'link'; label: string; to: string}
    | {kind: 'page'; label: string};

/** `handle.title` i18n key declared on titled routes (see router.tsx). */
interface RouteHandle {
    title?: string;
}

const handleOf = (match: {handle?: unknown}): RouteHandle | undefined =>
    match.handle as RouteHandle | undefined;

/**
 * Menu tree consumed by the sidebar. Icons are Lucide components so
 * they inherit `currentColor` from SidebarMenuButton and gracefully
 * shrink to 16px in icon-collapsed mode.
 */
const buildMenuItems = (t: TFunction): MenuItem[] => [
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
        icon: ShieldLock,
        label: t('nav.oauth2'),
        children: [
            {key: '/oauth2/clients', icon: LayoutGrid, label: t('nav.oauth2_client'), to: '/oauth2/clients'},
            {key: '/oauth2/jwks', icon: KeyRound, label: t('nav.oauth2_jwk'), to: '/oauth2/jwks'},
        ],
    },
    {
        key: '/settings',
        icon: Settings,
        label: t('nav.settings'),
        to: '/settings',
    },
];

interface Selection {
    selectedKey: string | null;
    openKey: string | null;
    parent: MenuGroup | null;
    leaf: MenuLeaf | null;
}

/**
 * Resolve currently selected menu key and its parent submenu key
 * based on the current location pathname. Longest-prefix wins so
 * that a nested route like `/users/:id` still selects `/users`.
 */
const resolveSelection = (pathname: string, items: MenuItem[]): Selection => {
    const flatten = (
        list: MenuItem[],
        parent: MenuGroup | null,
    ): Array<{leaf: MenuLeaf; parent: MenuGroup | null}> => {
        const acc: Array<{leaf: MenuLeaf; parent: MenuGroup | null}> = [];
        for (const item of list) {
            if ('children' in item) {
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
 * the route table (see `main.tsx`); the menu tree contributes the
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
 * URL the user last stood on. The cache updates only when the user
 * currently stands on a titled route (`match.pathname ===
 * location.pathname`), so descending into a child never overwrites
 * its parent's remembered `search`. It lives at module scope on
 * purpose — the console is a single-session app and re-mounting the
 * layout should not blow away in-session history; a full page reload
 * (fresh module import) is the natural reset event.
 */
const SEARCH_CACHE = new Map<string, string>();

const useBreadcrumbTrail = (
    menuItems: MenuItem[],
    override: string | null,
): TrailCrumb[] => {
    const matches = useMatches();
    const location = useLocation();
    const {t} = useTranslation();
    useEffect(() => {
        for (const m of matches) {
            if (handleOf(m)?.title && m.pathname === location.pathname) {
                SEARCH_CACHE.set(m.pathname, location.search);
            }
        }
    }, [matches, location.pathname, location.search]);
    return useMemo(() => {
        const titled = matches.filter((m) => handleOf(m)?.title);
        if (titled.length === 0) return [];
        const withUrl = (path: string) => path + (SEARCH_CACHE.get(path) ?? '');
        const trail: TrailCrumb[] = [];
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
            const label = isLast && override ? override : t(handleOf(m)!.title!);
            if (isLast) {
                trail.push({kind: 'page', label});
            } else {
                trail.push({kind: 'link', label, to: withUrl(m.pathname)});
            }
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
 *   on `SidebarMenuSub` and there is no way to reach the child routes
 *   while the sidebar is folded.
 *
 * `state` on mobile still reflects the persisted desktop state (see
 * SidebarProvider), so gate the dropdown branch on `!isMobile` — the
 * mobile sheet always renders fully expanded and should keep the
 * inline collapsible.
 */
const NavMainGroupItem = ({
    item,
    selectedKey,
    openKey,
}: {
    item: MenuGroup;
    selectedKey: string | null;
    openKey: string | null;
}): ReactElement => {
    const {state, isMobile} = useSidebar();
    const Icon = item.icon;
    const parentActive = item.children.some((c) => c.key === selectedKey);
    // Controlled open state instead of `defaultOpen`. `defaultOpen` is
    // read only once at mount, which loses the redirect case: landing on
    // a route that redirects into this group's child mounts the group
    // before `openKey` resolves, so it would stay collapsed even after
    // the location settles on the child. Seeding from the initial
    // `openKey` and re-opening whenever the group owns the active leaf
    // covers that, while `onOpenChange` still lets the user collapse it
    // by hand afterwards.
    const [open, setOpen] = useState(openKey === item.key);
    useEffect(() => {
        if (parentActive) setOpen(true);
    }, [parentActive]);

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
    // `group-aria-expanded/menu-button:rotate-90`. Official sidebar-08
    // uses `aria-expanded:rotate-90` on a dedicated SidebarMenuAction
    // trigger; Base UI's Collapsible.Trigger sets `aria-expanded` and
    // `data-panel-open` (NOT `data-open`, which only appears on the
    // panel), so the aria variant is the robust, upstream-aligned key.
    return (
        <Collapsible
            open={open}
            onOpenChange={setOpen}
            render={<SidebarMenuItem/>}
        >
            <CollapsibleTrigger
                render={
                    <SidebarMenuButton
                        tooltip={item.label}
                        className={
                            parentActive
                                ? 'font-medium text-sidebar-accent-foreground'
                                : undefined
                        }
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
                <SidebarMenuSub className="mr-0 pr-0">
                    {item.children.map((child) => {
                        const ChildIcon = child.icon;
                        return (
                            <SidebarMenuSubItem key={child.key}>
                                <SidebarMenuSubButton
                                    isActive={selectedKey === child.key}
                                    className="h-8 data-active:font-medium"
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
 * NavMain — each top-level item is a SidebarMenuButton rendered as a
 * link; items with children are delegated to `NavMainGroupItem` which
 * picks inline-collapsible vs. dropdown-popover based on the current
 * sidebar state.
 */
const NavMain = ({
    items,
    selectedKey,
    openKey,
}: {
    items: MenuItem[];
    selectedKey: string | null;
    openKey: string | null;
}): ReactElement => (
    <SidebarGroup>
        <SidebarMenu>
            {items.map((item) => {
                const Icon = item.icon;
                if (!('children' in item)) {
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
 * NavLocale — nav-secondary slot (`mt-auto` pins it to the bottom of
 * the sidebar content). A single SidebarMenuButton that opens a
 * DropdownMenu with the supported locales as a radio group. No
 * chevron; the icon + label are enough affordance.
 *
 * Locales render straight from `SUPPORTED_LOCALES` — each entry is
 * labelled by its `language.<tag>` endonym, so new locales only need
 * a resource bundle plus the matching entries in `match.ts` / the
 * `language` sections.
 */
const NavLocale = ({className}: {className?: string}): ReactElement => {
    const {t, i18n} = useTranslation();
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
                                side="top"
                                align="start"
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
 * NavUser — sidebar footer identity tile + dropdown. Shape mirrors the
 * reference console's nav-user; the identity source here is the /user
 * fetch owned by ConsoleLayout, so `loading` shows a skeleton only
 * during the brief in-flight window on first mount.
 */
const NavUser = ({
    currentUser,
    loading,
    onLogout,
}: {
    currentUser: CurrentUser | null;
    loading: boolean;
    onLogout: () => void;
}): ReactElement => {
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
 */
const UserIdentityTile = ({
    loading,
    initial,
    displayName,
    roleLine,
}: {
    loading: boolean;
    initial: string;
    displayName: string;
    roleLine: string;
}): ReactElement => {
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
 * rail sits at `--sidebar-width-icon` = 3rem = 48px. The floating-island
 * look for the content on the right is applied manually to `SidebarInset`
 * below rather than via `variant="inset"` on this component, which would
 * inflate the rail width with `p-2` gutters when collapsed.
 */
const AppSidebar = ({
    menuItems,
    selectedKey,
    openKey,
    currentUser,
    userLoading,
    onLogout,
    ...props
}: {
    menuItems: MenuItem[];
    selectedKey: string | null;
    openKey: string | null;
    currentUser: CurrentUser | null;
    userLoading: boolean;
    onLogout: () => void;
} & Omit<React.ComponentProps<typeof Sidebar>, 'children'>): ReactElement => (
    // `!border-r-0` cancels the `border-r` the default `sidebar` variant
    // paints on the container — with the manual floating-island styling
    // the line ends up floating in the sidebar-tinted gutter and looks
    // like a stray seam next to the rounded content card.
    <Sidebar collapsible="icon" className="!border-r-0" {...props}>
        <SidebarHeader>
            <SidebarMenu>
                <SidebarMenuItem>
                    <SidebarMenuButton size="lg" render={<Link to="/"/>}>
                        <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                            <Brand className="size-4"/>
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
 * Slim inset header: SidebarTrigger + Separator + Breadcrumb. Height
 * animates from 4rem down to 3rem when the sidebar collapses to icons.
 *
 * Frosted-glass treatment: the header lives INSIDE the page scroll
 * surface and sticks to its top (`sticky top-0`), so long pages slide
 * underneath it. `bg-background/60 backdrop-blur` keeps it legible
 * there — the canonical Tailwind glass recipe on shadcn's semantic
 * background token. The bottom seam follows the scroll position
 * (`scrolled` prop from ConsoleLayout): hidden via `border-transparent`
 * while the page rests at the very top — matching the reference
 * console's seamless look — and fades in (`border-border`, color
 * transitioned) the moment content starts sliding underneath.
 *
 * Trail composition lives entirely in `useBreadcrumbTrail`; this
 * component is a pure renderer. Each `crumb.kind` maps to a fixed
 * primitive; the separator between two crumbs upgrades to a
 * DropdownMenu switcher iff the crumb on its left is a menu parent
 * group (i.e. carries `children`).
 */
const InsetHeader = ({trail, scrolled}: {trail: TrailCrumb[]; scrolled: boolean}): ReactElement => (
    <header className={cn(
        'sticky top-0 z-10 flex h-16 shrink-0 items-center gap-2 border-b bg-background/60 backdrop-blur transition-[width,height,border-color] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-12',
        scrolled ? 'border-border' : 'border-transparent'
    )}>
        <div className="flex items-center gap-2 px-4">
            <SidebarTrigger className="-ml-1"/>
            {/*
              Verbatim with official sidebar-08 (base-nova): the base
              Separator carries `data-vertical:self-stretch`, which with
              a fixed h-4 degenerates to top alignment per the flexbox
              spec — upstream cancels it with `data-vertical:self-auto`
              (same variant spelling, so tailwind-merge drops the base
              `self-stretch` deterministically) and falls back to the
              parent's `items-center`.
            */}
            <Separator
                orientation="vertical"
                className="mr-2 data-vertical:h-4 data-vertical:self-auto"
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
const BreadcrumbSeparatorSlot = ({leftCrumb}: {leftCrumb: TrailCrumb}): ReactElement => {
    const {t} = useTranslation();
    if (leftCrumb.kind !== 'parent' || leftCrumb.children.length === 0) {
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

/**
 * ConsoleLayout — the COMPLETE User Center console: its sidebar, inset
 * header, routed pages and logout flow. Mounted directly by the route
 * table (see router.tsx); the console is fully self-contained and
 * lays out exactly like official sidebar-08.
 *
 * antd boundary: `ConfigProvider` scopes to the routed business pages
 * only — the console chrome above it (sidebar / inset header) is pure
 * shadcn. antd `<App>` lives on the business pages that call
 * `useApp()`, never here.
 */
const ConsoleLayout = (): ReactElement => {
    const [logoutOpen, setLogoutOpen] = useState(false);
    const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
    // `userLoading` gates the NavUser skeleton. Distinguishing "still
    // fetching" from "loaded but unauthenticated" matters: only the
    // first shows a skeleton; the second falls back to the static
    // `header.profile / header.console` text so the footer never
    // pulses forever when the user isn't signed in.
    const [userLoading, setUserLoading] = useState(true);
    // Runtime page-title override channel. Feature pages publish a
    // dynamic breadcrumb label via `usePageTitle(label)` (see
    // `_shared/pageTitle`) and get automatic cleanup on unmount;
    // ConsoleLayout owns the state so the trail can read it without
    // going through context.
    const [pageTitleOverride, setPageTitleOverride] = useState<string | null>(null);
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

    // Scroll-position tracking for InsetHeader's bottom seam: the line
    // shows only once the page actually scrolls (content sliding under
    // the frosted header) and hides again when the page rests at the
    // very top. The listener is passive (scroll never calls
    // preventDefault), and setState short-circuits on identical
    // booleans, so this is not a hot path.
    const scrollRef = useRef<HTMLDivElement>(null);
    const [scrolled, setScrolled] = useState(false);
    useEffect(() => {
        const el = scrollRef.current;
        if (!el) return undefined;
        const onScroll = () => setScrolled(el.scrollTop > 0);
        onScroll();
        el.addEventListener('scroll', onScroll, {passive: true});
        return () => el.removeEventListener('scroll', onScroll);
    }, []);

    return (
        <TooltipProvider>
            {/*
              Wrapper tint (`bg-sidebar`) fills the gutter behind the
              sidebar and around the floating content card. `h-svh`
              pins the console to exactly one viewport: long business
              pages scroll INSIDE the content card (the page container
              below is the sole scroll surface) instead of pushing the
              card's bottom edge past the screen.
            */}
            <SidebarProvider className="h-svh overflow-hidden bg-sidebar">
                <AppSidebar
                    menuItems={menuItems}
                    selectedKey={selectedKey}
                    openKey={openKey}
                    currentUser={currentUser}
                    userLoading={userLoading}
                    onLogout={() => setLogoutOpen(true)}
                />
                {/*
                  Floating card look for the content. `ml-0` in the
                  collapsed state is deliberate: an 8px left margin
                  would let the `bg-sidebar` tint show through as a
                  gutter between the rail and the card. `min-w-0`
                  is critical: SidebarInset is a flex child with
                  `flex-1 w-full`; without a min-width override its
                  intrinsic content (antd Tables with many columns)
                  prevents flex-shrink and the card grows past the
                  viewport. `min-h-0` + `overflow-hidden` complete the
                  in-card scroll model: the wrapper pins the console
                  to one viewport, the card clips to its rounded
                  outline, and the page container below scrolls.
                */}
                <SidebarInset className="min-h-0 min-w-0 overflow-hidden md:m-2 md:ml-0 md:rounded-xl md:shadow-sm">
                    {/*
                      This container is the page's sole scroll surface
                      (`min-h-0 overflow-y-auto`): long business pages
                      scroll inside the rounded card while InsetHeader
                      sticks to its top, frosted-glass style. The
                      header must live INSIDE this container for both
                      `sticky` and `backdrop-blur` to work — its
                      backdrop is the page content sliding underneath.

                      Feature pages own their own surface and don't
                      touch the breadcrumb — the layout derives it
                      from route `handle.title`. Pages that need a
                      dynamic label call `usePageTitle` (from
                      `_shared/pageTitle`) which publishes into
                      `PageTitleContext` below. Inset padding matches
                      the reference console (`p-4 pt-0`).

                      Deliberately a `div`, not `main`: SidebarInset
                      already renders the page's single `<main>`
                      landmark, nesting a second one would expose a
                      duplicate landmark to assistive tech.
                    */}
                    {/*
                      `overscroll-none` kills the macOS elastic
                      rubber-band on the console's main scroll
                      surface; the sidebar's own scroll surface is
                      covered in ConsoleLayout.css.
                    */}
                    <div ref={scrollRef} className="flex min-h-0 flex-1 flex-col overflow-y-auto overscroll-none">
                        <InsetHeader trail={trail} scrolled={scrolled}/>
                        <div className="flex flex-col gap-4 p-4 pt-0">
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
                                <PageTitleContext.Provider value={pageTitleCtx}>
                                    <Outlet/>
                                </PageTitleContext.Provider>
                            </ConfigProvider>
                        </div>
                    </div>
                </SidebarInset>
            </SidebarProvider>
            <LogoutConfirmModal
                open={logoutOpen}
                onCancel={() => setLogoutOpen(false)}
            />
        </TooltipProvider>
    );
};

export default ConsoleLayout;
