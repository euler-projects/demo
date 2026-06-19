/* eslint-disable compat/compat */
import React, {useEffect, useMemo, useState} from 'react';
import {Table, Button, message} from 'antd';
import {useTranslation} from 'react-i18next';

import {ACTION_COLUMN_WIDTH, OverflowTags, RowActions} from './_shared/tableLayout';

// Page-local width budgets for tag-style columns.
const AUTH_METHOD_COLUMN_WIDTH = 200;
const GRANT_TYPES_COLUMN_WIDTH = 220;
const SCOPES_COLUMN_WIDTH = 200;

/**
 * Convert a backend payload that may be either an array or a Set/Map
 * projection into a plain array. The Java domain models behind
 * /admin/api/oauth2/client expose Set<String>, which Jackson serializes
 * as a JSON array, but defensively normalize anyway.
 */
function toArray(value) {
    if (value == null) return [];
    if (Array.isArray(value)) return value;
    if (typeof value === 'object') {
        if (typeof value[Symbol.iterator] === 'function') return Array.from(value);
        return Object.values(value);
    }
    return [value];
}

const listClients = async ({offset, limit}) => {
    return await fetch(`/admin/api/oauth2/client?offset=${offset}&limit=${limit}`)
        .then((res) => res.json())
        .then((rows) => rows);
};

const OAuth2Client = () => {
    const {t} = useTranslation();
    const [messageApi, contextHolder] = message.useMessage();

    const [data, setData] = useState();
    const [loading, setLoading] = useState(false);
    const [tableParams, setTableParams] = useState({
        pagination: {current: 1, pageSize: 10, total: 0},
    });

    const fetchData = () => {
        setLoading(true);
        const page = tableParams.pagination.current;
        const size = tableParams.pagination.pageSize;
        const offset = (page - 1) * size;
        const limit = size * 3;
        listClients({offset, limit})
            .then((rows) => {
                const total = rows.length < limit
                    ? offset + rows.length
                    : Math.max(tableParams.pagination.total, offset + rows.length);
                setData(rows.slice(0, size));
                setLoading(false);
                setTableParams({
                    ...tableParams,
                    pagination: {...tableParams.pagination, total},
                });
            })
            .catch(() => setLoading(false));
    };

    useEffect(fetchData, [
        tableParams.pagination?.current,
        tableParams.pagination?.pageSize,
    ]);

    const handleTableChange = (pagination) => {
        setTableParams({pagination});
        if (pagination.pageSize !== tableParams.pagination?.pageSize) {
            setData([]);
        }
    };

    // Placeholder handler for actions that are not wired to the backend
    // yet. Surfaces the intended action through a toast so the affordance
    // remains discoverable during the UI-only phase.
    const stubAction = (key, record) => {
        const actionLabel = t(`oauth2.client.${key}`);
        const subject = record?.clientName || record?.clientId || record?.registrationId || '';
        messageApi.info(t('oauth2.client.stub', {action: actionLabel, name: subject}));
    };

    const columns = useMemo(() => [
        {
            title: t('oauth2.client.column.clientId'),
            dataIndex: 'clientId',
            width: 220,
            ellipsis: true,
        },
        {
            title: t('oauth2.client.column.clientName'),
            dataIndex: 'clientName',
            width: 200,
            ellipsis: true,
        },
        {
            title: t('oauth2.client.column.authMethod'),
            dataIndex: 'tokenEndpointAuthMethod',
            width: AUTH_METHOD_COLUMN_WIDTH,
            render: (method) => {
                if (!method) return null;
                return <OverflowTags items={[{key: method, label: method}]} columnWidth={AUTH_METHOD_COLUMN_WIDTH}/>;
            },
        },
        {
            title: t('oauth2.client.column.grantTypes'),
            dataIndex: 'authorizationGrantTypes',
            width: GRANT_TYPES_COLUMN_WIDTH,
            render: (grantTypes) => {
                const items = toArray(grantTypes).map((g) => ({key: g, label: g}));
                return <OverflowTags items={items} columnWidth={GRANT_TYPES_COLUMN_WIDTH}/>;
            },
        },
        {
            title: t('oauth2.client.column.scopes'),
            dataIndex: 'scopes',
            width: SCOPES_COLUMN_WIDTH,
            render: (scopes) => {
                const items = toArray(scopes).map((s) => ({key: s, label: s, color: 'blue'}));
                return <OverflowTags items={items} columnWidth={SCOPES_COLUMN_WIDTH}/>;
            },
        },
        {
            title: t('oauth2.client.column.action'),
            key: 'action',
            width: ACTION_COLUMN_WIDTH,
            fixed: 'right',
            render: (_, record) => {
                const actions = [
                    {key: 'detail', label: t('oauth2.client.detail'), onClick: () => stubAction('detail', record)},
                    {key: 'rotateSecret', label: t('oauth2.client.rotateSecret'), onClick: () => stubAction('rotateSecret', record)},
                    {key: 'delete', label: t('oauth2.client.delete'), danger: true, onClick: () => stubAction('delete', record)},
                ];
                return (
                    <RowActions
                        actions={actions}
                        columnWidth={ACTION_COLUMN_WIDTH}
                        moreLabel={t('common.more')}
                    />
                );
            },
        },
    ], [t]);

    return (
        <div>
            {contextHolder}
            <div style={{marginBottom: '10px'}}>
                <Button type="primary" onClick={() => stubAction('create')}>{t('oauth2.client.create')}</Button>
            </div>
            <Table
                columns={columns}
                rowKey={(record) => record.registrationId ?? record.clientId}
                dataSource={data}
                pagination={tableParams.pagination}
                loading={loading}
                onChange={handleTableChange}
                scroll={{x: 'max-content'}}
            />
        </div>
    );
};

export default OAuth2Client;
