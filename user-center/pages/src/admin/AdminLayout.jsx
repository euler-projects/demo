import React, {useMemo, useState} from 'react';
import {Layout, Menu, Typography, Avatar, Dropdown, theme as antdTheme, ConfigProvider} from 'antd';
import {
    UserOutlined,
    TeamOutlined,
    SafetyCertificateOutlined,
    KeyOutlined,
    AppstoreOutlined,
    SettingOutlined,
    DashboardOutlined,
    LogoutOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
} from '@ant-design/icons';
import {Link, Outlet, useLocation} from 'react-router';

const {Header, Sider, Content} = Layout;

const menuItems = [
    {
        key: '/admin/dashboard',
        icon: <DashboardOutlined/>,
        label: <Link to="/admin/dashboard">仪表盘</Link>,
    },
    {
        key: 'identity',
        icon: <TeamOutlined/>,
        label: '身份与访问',
        children: [
            {
                key: '/admin/user',
                icon: <UserOutlined/>,
                label: <Link to="/admin/user">用户管理</Link>,
            },
            {
                key: '/admin/role',
                icon: <SafetyCertificateOutlined/>,
                label: <Link to="/admin/role">角色与权限</Link>,
            },
        ],
    },
    {
        key: 'oauth2',
        icon: <AppstoreOutlined/>,
        label: 'OAuth2',
        children: [
            {
                key: '/admin/oauth2/client',
                label: <Link to="/admin/oauth2/client">客户端</Link>,
            },
            {
                key: '/admin/oauth2/jwk',
                icon: <KeyOutlined/>,
                label: <Link to="/admin/oauth2/jwk">JWK 密钥</Link>,
            },
        ],
    },
    {
        key: '/admin/settings',
        icon: <SettingOutlined/>,
        label: <Link to="/admin/settings">系统设置</Link>,
    },
];

/**
 * Resolve currently selected menu key and its parent submenu key
 * based on the current location pathname.
 */
const resolveSelection = (pathname) => {
    const flatten = (items, parentKey) => {
        const acc = [];
        for (const item of items) {
            if (item.children) {
                acc.push(...flatten(item.children, item.key));
            } else {
                acc.push({key: item.key, parentKey});
            }
        }
        return acc;
    };
    const leaves = flatten(menuItems, null);
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
    const {selectedKeys, openKeys} = useMemo(
        () => resolveSelection(location.pathname),
        [location.pathname]
    );
    const {token} = antdTheme.useToken();

    const userMenu = {
        items: [
            {key: 'profile', icon: <UserOutlined/>, label: '个人中心'},
            {type: 'divider'},
            {key: 'logout', icon: <LogoutOutlined/>, label: '退出登录'},
        ],
    };

    return (
        <ConfigProvider
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
                                fontWeight: 700,
                                fontSize: 14,
                            }}
                        >
                            UC
                        </div>
                        {!collapsed && (
                            <Typography.Text strong style={{fontSize: 16, whiteSpace: 'nowrap'}}>
                                User Center
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
                                Admin Console
                            </Typography.Text>
                        </div>
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
