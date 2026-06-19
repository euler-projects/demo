import React, {useMemo, useState} from 'react';
import {Layout, Menu, Typography, Avatar, Dropdown, theme as antdTheme, ConfigProvider} from 'antd';
import {
    UserOutlined,
    TeamOutlined,
    SafetyCertificateOutlined,
    KeyOutlined,
    ApiOutlined,
    AppstoreOutlined,
    SettingOutlined,
    DashboardOutlined,
    LogoutOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    GlobalOutlined,
} from '@ant-design/icons';
import {Link, Outlet, useLocation} from 'react-router';
import {useTranslation} from 'react-i18next';

import {SUPPORTED_LOCALES, getAntdLocale, setLocale} from '../i18n';

const {Header, Sider, Content} = Layout;

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
 * Abstract console mark: a 2x2 stack of rounded tiles with
 * alternating opacity, evoking a dashboard / cluster surface.
 */
const ConsoleMark = ({size = 16}) => (
    <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
    >
        <rect x="2" y="2" width="9" height="9" rx="2" fill="currentColor" opacity="0.95"/>
        <rect x="13" y="2" width="9" height="9" rx="2" fill="currentColor" opacity="0.55"/>
        <rect x="2" y="13" width="9" height="9" rx="2" fill="currentColor" opacity="0.55"/>
        <rect x="13" y="13" width="9" height="9" rx="2" fill="currentColor" opacity="0.95"/>
    </svg>
);

const buildMenuItems = (t) => [
    {
        key: '/dashboard',
        icon: <DashboardOutlined/>,
        label: <Link to="/dashboard">{t('nav.dashboard')}</Link>,
    },
    {
        key: 'identity',
        icon: <TeamOutlined/>,
        label: t('nav.iam'),
        children: [
            {
                key: '/user',
                icon: <UserOutlined/>,
                label: <Link to="/user">{t('nav.user')}</Link>,
            },
            {
                key: '/role',
                icon: <SafetyCertificateOutlined/>,
                label: <Link to="/role">{t('nav.role')}</Link>,
            },
        ],
    },
    {
        key: 'oauth2',
        icon: <AppstoreOutlined/>,
        label: t('nav.oauth2'),
        children: [
            {
                key: '/oauth2/client',
                icon: <ApiOutlined/>,
                label: <Link to="/oauth2/client">{t('nav.oauth2_client')}</Link>,
            },
            {
                key: '/oauth2/jwk',
                icon: <KeyOutlined/>,
                label: <Link to="/oauth2/jwk">{t('nav.oauth2_jwk')}</Link>,
            },
        ],
    },
    {
        key: '/settings',
        icon: <SettingOutlined/>,
        label: <Link to="/settings">{t('nav.settings')}</Link>,
    },
];

/**
 * Resolve currently selected menu key and its parent submenu key
 * based on the current location pathname.
 */
const resolveSelection = (pathname, items) => {
    const flatten = (list, parentKey) => {
        const acc = [];
        for (const item of list) {
            if (item.children) {
                acc.push(...flatten(item.children, item.key));
            } else {
                acc.push({key: item.key, parentKey});
            }
        }
        return acc;
    };
    const leaves = flatten(items, null);
    const matched = leaves
        .filter((it) => pathname === it.key || pathname.startsWith(it.key + '/'))
        .sort((a, b) => b.key.length - a.key.length)[0];
    if (!matched) {
        return {selectedKeys: [], openKeys: []};
    }
    return {
        selectedKeys: [matched.key],
        openKeys: matched.parentKey ? [matched.parentKey] : [],
    };
};

const AdminLayout = () => {
    const [collapsed, setCollapsed] = useState(false);
    const location = useLocation();
    const {t, i18n} = useTranslation();
    const menuItems = useMemo(() => buildMenuItems(t), [t, i18n.language]);
    const {selectedKeys, openKeys} = useMemo(
        () => resolveSelection(location.pathname, menuItems),
        [location.pathname, menuItems]
    );
    const {token} = antdTheme.useToken();

    const userMenu = {
        items: [
            {key: 'profile', icon: <UserOutlined/>, label: t('header.profile')},
            {type: 'divider'},
            {key: 'logout', icon: <LogoutOutlined/>, label: t('header.logout')},
        ],
    };

    const localeMenu = {
        items: SUPPORTED_LOCALES.map((tag) => ({
            key: tag,
            label: t(`language.${tag}`),
        })),
        selectedKeys: [i18n.language],
        onClick: ({key}) => setLocale(key),
    };

    return (
        <ConfigProvider
            locale={getAntdLocale(i18n.language)}
            theme={{
                algorithm: antdTheme.defaultAlgorithm,
                token: {
                    colorPrimary: '#1677ff',
                    borderRadius: 6,
                },
                components: {
                    Layout: {
                        headerBg: '#ffffff',
                        siderBg: '#ffffff',
                        bodyBg: '#f5f7fa',
                        headerHeight: 56,
                    },
                    Menu: {
                        itemBg: '#ffffff',
                        subMenuItemBg: '#ffffff',
                        itemSelectedBg: '#e6f4ff',
                        itemSelectedColor: '#1677ff',
                        itemHoverBg: '#f0f5ff',
                    },
                },
            }}
        >
            <Layout style={{minHeight: '100vh'}}>
                <Sider
                    theme="light"
                    width={232}
                    collapsible
                    collapsed={collapsed}
                    trigger={null}
                    style={{
                        borderRight: `1px solid ${token.colorBorderSecondary}`,
                        boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)',
                    }}
                >
                    <div
                        style={{
                            height: 56,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: collapsed ? 'center' : 'flex-start',
                            padding: collapsed ? 0 : '0 20px',
                            borderBottom: `1px solid ${token.colorBorderSecondary}`,
                            gap: 8,
                        }}
                    >
                        <div
                            style={{
                                width: 28,
                                height: 28,
                                borderRadius: 6,
                                background: 'linear-gradient(135deg, #1677ff 0%, #69b1ff 100%)',
                                color: '#fff',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                            }}
                        >
                            <ConsoleMark size={16}/>
                        </div>
                        {!collapsed && (
                            <Typography.Text strong style={{fontSize: 16, whiteSpace: 'nowrap'}}>
                                {SITE_NAME}
                            </Typography.Text>
                        )}
                    </div>
                    <Menu
                        mode="inline"
                        theme="light"
                        items={menuItems}
                        selectedKeys={selectedKeys}
                        defaultOpenKeys={openKeys}
                        style={{borderInlineEnd: 'none', paddingTop: 8}}
                    />
                </Sider>
                <Layout>
                    <Header
                        style={{
                            padding: '0 16px 0 8px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            borderBottom: `1px solid ${token.colorBorderSecondary}`,
                            boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)',
                        }}
                    >
                        <div style={{display: 'flex', alignItems: 'center', gap: 8}}>
                            <span
                                onClick={() => setCollapsed(!collapsed)}
                                style={{
                                    fontSize: 18,
                                    cursor: 'pointer',
                                    padding: '0 12px',
                                    color: token.colorTextSecondary,
                                }}
                            >
                                {collapsed ? <MenuUnfoldOutlined/> : <MenuFoldOutlined/>}
                            </span>
                            <Typography.Text type="secondary" style={{fontSize: 13}}>
                                {t('header.console')}
                            </Typography.Text>
                        </div>
                        <div style={{display: 'flex', alignItems: 'center', gap: 4}}>
                            <Dropdown menu={localeMenu} placement="bottomRight" trigger={['click']}>
                                <div
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 6,
                                        cursor: 'pointer',
                                        padding: '0 10px',
                                        color: token.colorTextSecondary,
                                    }}
                                    title={t('header.language')}
                                >
                                    <GlobalOutlined style={{fontSize: 16}}/>
                                    <Typography.Text style={{fontSize: 13}}>
                                        {t(`language.${i18n.language}`)}
                                    </Typography.Text>
                                </div>
                            </Dropdown>
                            <Dropdown menu={userMenu} placement="bottomRight" trigger={['click']}>
                                <div
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 8,
                                        cursor: 'pointer',
                                        padding: '0 8px',
                                    }}
                                >
                                    <Avatar size="small" icon={<UserOutlined/>}/>
                                    <Typography.Text>admin</Typography.Text>
                                </div>
                            </Dropdown>
                        </div>
                    </Header>
                    <Content
                        style={{
                            margin: 16,
                            padding: 20,
                            background: '#ffffff',
                            borderRadius: 8,
                            border: `1px solid ${token.colorBorderSecondary}`,
                            minHeight: 'calc(100vh - 56px - 32px)',
                        }}
                    >
                        <Outlet/>
                    </Content>
                </Layout>
            </Layout>
        </ConfigProvider>
    );
};

export default AdminLayout;
