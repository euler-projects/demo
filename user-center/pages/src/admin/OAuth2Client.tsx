import {useEffect, useMemo, useState} from 'react';
import {App as AntdApp, Button} from 'antd';
import type {TablePaginationConfig, TableProps} from 'antd';
import {useTranslation} from 'react-i18next';

import {DataTable} from '@/components/DataTable';
import {
    ACTION_COLUMN_WIDTH,
    CopyableCell,
    MiddleEllipsisText,
    OverflowTags,
    RowActions,
    computeActionsColumnWidth,
} from './_shared/tableLayout';
import {extractApiError} from './_shared/api';

// Page-local width budgets for tag-style columns. OverflowTags packs its
// tags against these, so the columns must stay fixed-width.
const AUTH_METHOD_COLUMN_WIDTH = 200;
const GRANT_TYPES_COLUMN_WIDTH = 220;
const SCOPES_COLUMN_WIDTH = 200;

/** One row of the OAuth2 client list (`/admin/api/oauth2/clients`). */
interface OAuth2ClientRow {
    registrationId?: string;
    clientId: string;
    clientName?: string;
    tokenEndpointAuthMethod?: string;
    authorizationGrantTypes?: unknown;
    scopes?: unknown;
}

interface TableParams {
    pagination: TablePaginationConfig;
}

/**
 * Convert a backend payload that may be either an array or a Set/Map
 * projection into a plain array. The Java domain models behind
 * /admin/api/oauth2/clients expose Set<String>, which Jackson serializes
 * as a JSON array, but defensively normalize anyway.
 */
function toArray(value: unknown): string[] {
    if (value == null) return [];
    if (Array.isArray(value)) return value as string[];
    if (typeof value === 'object') {
        if (typeof (value as Iterable<unknown>)[Symbol.iterator] === 'function') {
            return Array.from(value as Iterable<string>);
        }
        return Object.values(value as Record<string, string>);
    }
    return [value as string];
}

const listClients = async ({offset, limit}: {offset: number; limit: number}): Promise<OAuth2ClientRow[]> => {
    return await fetch(`/admin/api/oauth2/clients?offset=${offset}&limit=${limit}`)
        .then((res) => res.json() as Promise<OAuth2ClientRow[]>)
        .then((rows) => rows);
};

const OAuth2Client = () => {
    const {t} = useTranslation();
    // `<App>` context comes from AntdAppRoute (see router.tsx), so the
    // page needs no message holder of its own.
    const {message} = AntdApp.useApp();

    const [data, setData] = useState<OAuth2ClientRow[]>();
    const [loading, setLoading] = useState(false);
    const [tableParams, setTableParams] = useState<TableParams>({
        pagination: {current: 1, pageSize: 10, total: 0},
    });

    const fetchData = () => {
        setLoading(true);
        const page = tableParams.pagination.current ?? 1;
        const size = tableParams.pagination.pageSize ?? 10;
        const offset = (page - 1) * size;
        const limit = size * 3;
        listClients({offset, limit})
            .then((rows) => {
                const total = rows.length < limit
                    ? offset + rows.length
                    : Math.max(tableParams.pagination.total ?? 0, offset + rows.length);
                setData(rows.slice(0, size));
                setLoading(false);
                setTableParams({
                    ...tableParams,
                    pagination: {...tableParams.pagination, total},
                });
            })
            .catch((err) => {
                setLoading(false);
                message.error(extractApiError(err));
            });
    };

    useEffect(fetchData, [
        tableParams.pagination?.current,
        tableParams.pagination?.pageSize,
    ]);

    const handleTableChange = (pagination: TablePaginationConfig) => {
        setTableParams({pagination});
        if (pagination.pageSize !== tableParams.pagination?.pageSize) {
            setData([]);
        }
    };

    // Placeholder handler for actions that are not wired to the backend
    // yet. Surfaces the intended action through a toast so the affordance
    // remains discoverable during the UI-only phase.
    const stubAction = (key: string, record?: OAuth2ClientRow) => {
        const actionLabel = t(`oauth2.client.${key}`);
        const subject = record?.clientName || record?.clientId || record?.registrationId || '';
        message.info(t('oauth2.client.stub', {action: actionLabel, name: subject}));
    };

    const actionColumnWidth = useMemo(
        () => computeActionsColumnWidth([
            t('oauth2.client.detail'),
            t('oauth2.client.rotateSecret'),
            t('oauth2.client.delete'),
        ], ACTION_COLUMN_WIDTH),
        [t]
    );

    const columns = useMemo<TableProps<OAuth2ClientRow>['columns']>(() => [
        {
            title: t('oauth2.client.column.clientId'),
            dataIndex: 'clientId',
            width: 260,
            render: (clientId: string) => (
                <CopyableCell
                    mono
                    value={clientId}
                    display={(v) => <MiddleEllipsisText text={v} mono/>}
                />
            ),
        },
        {
            title: t('oauth2.client.column.clientName'),
            // No width: the single flexible column (floor via DataTable's
            // flexFloor) absorbing leftover space on wide screens.
            dataIndex: 'clientName',
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
            width: actionColumnWidth,
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
                        columnWidth={actionColumnWidth}
                        moreLabel={t('common.more')}
                    />
                );
            },
        },
    ], [t, actionColumnWidth]);

    return (
        <DataTable<OAuth2ClientRow>
            leading={
                <Button type="primary" onClick={() => stubAction('create')}>
                    {t('oauth2.client.create')}
                </Button>
            }
            onRefresh={() => fetchData()}
            rowKey={(record) => record.registrationId ?? record.clientId}
            dataSource={data}
            columns={columns}
            pagination={tableParams.pagination}
            loading={loading}
            onChange={handleTableChange}
            flexFloor={200}
        />
    );
};

export default OAuth2Client;
