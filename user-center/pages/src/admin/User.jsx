/* eslint-disable compat/compat */
import React, {useEffect, useMemo, useState, useCallback} from 'react';
import {Table, Button, Modal, Form, Input, Select, Checkbox, Spin} from 'antd';
import {useTranslation, Trans} from 'react-i18next';
import {Link, useSearchParams} from 'react-router';

import {ACTION_COLUMN_WIDTH, OverflowTags, RowActions, computeActionsColumnWidth} from './_shared/tableLayout';
import {apiPost, apiPut, apiDelete, apiGet} from './_shared/api';

const AUTHORITIES_COLUMN_WIDTH = 200;
const STATUS_COLUMN_WIDTH = 180;

const listUsers = async ({offset, limit}) => {
    const res = await fetch(`/admin/api/users?offset=${offset}&limit=${limit}`);
    return res.json();
};

const User = () => {
    const {t} = useTranslation();
    const [searchParams, setSearchParams] = useSearchParams();
    const initialPage = parseInt(searchParams.get('page') || '1', 10);
    const OP_TYPE_CREATE = 'create';
    const OP_TYPE_RESET_PW = 'reset-pw';
    const OP_TYPE_ENABLE = 'enable';
    const OP_TYPE_DISABLE = 'disable';
    const OP_TYPE_DELETE = 'delete';

    const formItemLayout = {
        labelCol: {xs: {span: 24}, sm: {span: 6}},
        wrapperCol: {xs: {span: 24}, sm: {span: 18}},
    };

    const authorityLabel = (authority) => {
        const key = `user.role.${authority.authority}`;
        const translated = t(key);
        return translated === key ? (authority.name ?? authority.authority) : translated;
    };

    const toggleMeasureLabel = useMemo(() => {
        const disable = t('user.disable');
        const enable = t('user.enable');
        return disable.length >= enable.length ? disable : enable;
    }, [t]);

    const actionColumnWidth = useMemo(
        () => computeActionsColumnWidth([
            t('user.detail'),
            toggleMeasureLabel,
            t('user.resetPassword'),
            t('user.delete'),
        ], ACTION_COLUMN_WIDTH),
        [t, toggleMeasureLabel]
    );

    // --- Async phone loading ---
    const [phoneMap, setPhoneMap] = useState({});

    const loadPhonesForUsers = useCallback((users) => {
        if (!users || users.length === 0) return;
        const updates = {};
        users.forEach(u => {
            if (!phoneMap[u.userId]) {
                updates[u.userId] = {loading: true, phones: []};
            }
        });
        if (Object.keys(updates).length > 0) {
            setPhoneMap(prev => ({...prev, ...updates}));
        }
        users.forEach(u => {
            apiGet(`/admin/api/users/${encodeURIComponent(u.userId)}/identities?identityType=phone`)
                .then(identities => {
                    const phones = (identities || []).map(i => i.phone).filter(Boolean);
                    setPhoneMap(prev => ({
                        ...prev,
                        [u.userId]: {loading: false, phones},
                    }));
                })
                .catch(() => {
                    setPhoneMap(prev => ({
                        ...prev,
                        [u.userId]: {loading: false, phones: []},
                    }));
                });
        });
    }, []);

    const renderPhone = (_, record) => {
        const entry = phoneMap[record.userId];
        if (!entry || entry.loading) {
            return <Spin size="small"/>;
        }
        if (entry.phones.length === 0) return '-';
        if (entry.phones.length === 1) return entry.phones[0];
        return <span>{entry.phones[0]} <span style={{color: '#999'}}>+{entry.phones.length - 1}</span></span>;
    };

    const columns = useMemo(() => [
        {
            // The column is captioned "User ID" (i18n key `user.column.userId`)
            // but intentionally sources the `username` field: username IS the
            // user-facing identifier in this system. The key/field mismatch is
            // deliberate, not a typo.
            title: t('user.column.userId'),
            dataIndex: 'username',
            sorter: true,
            width: 160,
            render: (text, record) => (
                <Link to={record.userId} state={{fromPage: tableParams.pagination.current}}>
                    <span style={{fontFamily: 'monospace'}}>{text}</span>
                </Link>
            ),
        },
        {
            title: t('user.column.phone'),
            key: 'phone',
            width: 160,
            render: renderPhone,
        },
        {
            title: t('user.column.authorities'),
            dataIndex: 'authorities',
            width: AUTHORITIES_COLUMN_WIDTH,
            render: (authorities) => {
                const items = (authorities ?? []).map((authority) => ({
                    key: authority.authority,
                    label: authorityLabel(authority),
                    color: (authority.authority === 'root' || authority.authority === 'admin') ? 'red' : undefined,
                }));
                return <OverflowTags items={items} columnWidth={AUTHORITIES_COLUMN_WIDTH}/>;
            },
        },
        {
            title: t('user.column.status'),
            dataIndex: 'enabled',
            width: STATUS_COLUMN_WIDTH,
            render: (_, user) => {
                const items = [];
                if (!user.enabled) {
                    items.push({key: 'disabled', label: t('user.status.disabled')});
                } else {
                    if (!user.accountNonExpired) items.push({key: 'account-expired', label: t('user.status.accountExpired'), color: 'red'});
                    if (!user.accountNonLocked) items.push({key: 'account-locked', label: t('user.status.accountLocked'), color: 'red'});
                    if (!user.credentialsNonExpired) items.push({key: 'credentials-expired', label: t('user.status.credentialsExpired'), color: 'red'});
                }
                if (items.length === 0) items.push({key: 'normal', label: t('user.status.normal'), color: 'green'});
                return <OverflowTags items={items} columnWidth={STATUS_COLUMN_WIDTH}/>;
            },
        },
        {
            title: t('user.column.action'),
            key: 'action',
            width: actionColumnWidth,
            fixed: 'right',
            render: (_, record) => {
                const toggleType = record.enabled ? OP_TYPE_DISABLE : OP_TYPE_ENABLE;
                const actions = [
                    {key: 'detail', label: t('user.detail'), onClick: () => {}},
                    {key: 'toggle', label: record.enabled ? t('user.disable') : t('user.enable'), onClick: () => showConfirmModal(record, toggleType)},
                    {key: 'reset', label: t('user.resetPassword'), onClick: () => showResetPasswordModal(record)},
                    {key: 'delete', label: t('user.delete'), danger: true, onClick: () => showConfirmModal(record, OP_TYPE_DELETE)},
                ];
                return (
                    <RowActions
                        actions={actions}
                        columnWidth={actionColumnWidth}
                        moreLabel={t('common.more')}
                        measureLabels={{toggle: toggleMeasureLabel}}
                    />
                );
            },
        },
    ], [t, toggleMeasureLabel, actionColumnWidth, phoneMap]);

    const prefixSelector = (
        <Form.Item name="prefix" noStyle>
            <Select style={{width: 70}}>
                <Select.Option value="86">+86</Select.Option>
            </Select>
        </Form.Item>
    );

    const showCreateModal = () => {
        setFormType(OP_TYPE_CREATE);
        setModalTitle(t('user.modal.create'));
        form.resetFields();
        setIsModalOpen(true);
    };
    const showResetPasswordModal = (user) => {
        setFormType(OP_TYPE_RESET_PW);
        setModalTitle(t('user.modal.resetPassword'));
        form.setFieldsValue({userId: user.userId, username: user.username});
        setIsModalOpen(true);
    };
    const showConfirmModal = (user, operateType) => {
        setFormType(operateType);
        setModalTitle(null);
        form.setFieldsValue({...user, authorities: null});
        setIsModalOpen(true);
    };
    const handleOk = () => form.submit();
    const handleCancel = () => {
        setIsModalOpen(false);
        form.resetFields();
    };

    const toUserRequestObject = (user) => ({
        userId: user.userId,
        username: user.username,
        password: user.password,
        email: user.email,
        phone: user.phone,
        authorities: user.authorities?.map(a => ({authority: a})),
        accountNonExpired: user.accountNonExpired ?? false,
        accountNonLocked: user.accountNonLocked ?? false,
        credentialsNonExpired: user.credentialsNonExpired ?? false,
        enabled: user.enabled ?? false,
    });

    const onFinish = (userData) => {
        if (formType === OP_TYPE_CREATE) {
            apiPost('/admin/api/users', toUserRequestObject(userData))
                .then(() => { setIsModalOpen(false); form.resetFields(); fetchData(); });
        } else if (formType === OP_TYPE_RESET_PW) {
            apiPut(`/admin/api/users/${encodeURIComponent(userData.userId)}/password`, {password: userData.password})
                .then(() => { setIsModalOpen(false); form.resetFields(); });
        } else if (formType === OP_TYPE_ENABLE || formType === OP_TYPE_DISABLE) {
            apiPut(`/admin/api/users/${encodeURIComponent(userData.userId)}`, toUserRequestObject({
                ...userData,
                enabled: formType === OP_TYPE_ENABLE,
            })).then(() => { setIsModalOpen(false); form.resetFields(); fetchData(); });
        } else if (formType === OP_TYPE_DELETE) {
            apiDelete(`/admin/api/users/${encodeURIComponent(userData.userId)}`)
                .then(() => { setIsModalOpen(false); form.resetFields(); fetchData(); });
        }
    };

    const [form] = Form.useForm();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [modalTitle, setModalTitle] = useState('');
    const [formType, setFormType] = useState(OP_TYPE_CREATE);
    const [data, setData] = useState();
    const [loading, setLoading] = useState(false);
    const [tableParams, setTableParams] = useState({
        pagination: {current: initialPage, pageSize: 10, total: 0},
    });

    const fetchData = () => {
        setLoading(true);
        const page = tableParams.pagination.current;
        const size = tableParams.pagination.pageSize;
        const offset = (page - 1) * size;
        const limit = size * 3;
        listUsers({offset, limit}).then(rows => {
            const total = rows.length < limit
                ? offset + rows.length
                : Math.max(tableParams.pagination.total, offset + rows.length);
            const pageData = rows.slice(0, size);
            setData(pageData);
            setLoading(false);
            setTableParams(prev => ({
                ...prev,
                pagination: {...prev.pagination, total},
            }));
            loadPhonesForUsers(pageData);
        });
    };

    useEffect(fetchData, [
        tableParams.pagination?.current,
        tableParams.pagination?.pageSize,
    ]);

    const handleTableChange = (pagination, filters, sorter) => {
        setSearchParams({page: String(pagination.current)}, {replace: true});
        setTableParams({pagination, filters, sortOrder: Array.isArray(sorter) ? undefined : sorter.order, sortField: Array.isArray(sorter) ? undefined : sorter.field});
        if (pagination.pageSize !== tableParams.pagination?.pageSize) setData([]);
    };

    const UserForm = ({operationType}) => {
        const isCreate = operationType === OP_TYPE_CREATE;
        const isResetPw = operationType === OP_TYPE_RESET_PW;
        const isEnableUser = operationType === OP_TYPE_ENABLE;
        const isDisableUser = operationType === OP_TYPE_DISABLE;
        const isDeleteUser = operationType === OP_TYPE_DELETE;
        const addHiddenUserIdInput = !isCreate;
        const showFullUserInputs = isCreate;
        const showPasswordInputs = isCreate || isResetPw;

        return (
            <Form {...formItemLayout} form={form} onFinish={onFinish} initialValues={{prefix: '86'}}>
                {isEnableUser && <Trans i18nKey="user.confirm.enable" values={{username: form.getFieldValue('username')}} components={{code: <code/>}}/>}
                {isDisableUser && <Trans i18nKey="user.confirm.disable" values={{username: form.getFieldValue('username')}} components={{code: <code/>}}/>}
                {isDeleteUser && <Trans i18nKey="user.confirm.delete" values={{username: form.getFieldValue('username')}} components={{code: <code/>}}/>}
                {addHiddenUserIdInput && <Form.Item name="userId" label="userId" hidden><Input disabled autoComplete="off"/></Form.Item>}
                {isResetPw && <Form.Item name="username" label="username" hidden><Input autoComplete="username" readOnly/></Form.Item>}
                {showFullUserInputs && (
                    <Form.Item name="username" label={t('user.form.username')} rules={[{required: true, message: t('user.form.required.username')}]}>
                        <Input/>
                    </Form.Item>
                )}
                {showFullUserInputs && (
                    <Form.Item name="phone" label={t('user.form.phone')}>
                        <Input addonBefore={prefixSelector} style={{width: '100%'}}/>
                    </Form.Item>
                )}
                {showPasswordInputs && (
                    <Form.Item name="password" label={t('user.form.password')} rules={[{required: true, message: t('user.form.required.password')}]} hasFeedback>
                        <Input.Password autoComplete="new-password"/>
                    </Form.Item>
                )}
                {showPasswordInputs && (
                    <Form.Item name="confirm" label={t('user.form.confirm')} dependencies={['password']} hasFeedback
                        rules={[
                            {required: true, message: t('user.form.required.confirm')},
                            ({getFieldValue}) => ({
                                validator(_, value) {
                                    if (!value || getFieldValue('password') === value) return Promise.resolve();
                                    return Promise.reject(new Error(t('user.form.passwordMismatch')));
                                },
                            }),
                        ]}>
                        <Input.Password autoComplete="new-password"/>
                    </Form.Item>
                )}
                {showFullUserInputs && (
                    <Form.Item name="authorities" label={t('user.form.authorities')} rules={[{required: true, message: t('user.form.required.authorities')}]} initialValue={['user']}>
                        <Select mode="multiple" allowClear style={{width: '100%'}} placeholder={t('user.form.authoritiesPlaceholder')}
                            options={[{label: t('user.role.user'), value: 'user'}, {label: t('user.role.admin'), value: 'admin'}]}/>
                    </Form.Item>
                )}
                {(isCreate || isEnableUser || isDisableUser) && <Form.Item name="enabled" valuePropName="checked" initialValue={true} hidden><Checkbox/></Form.Item>}
                {(isCreate || isEnableUser || isDisableUser) && <Form.Item name="credentialsNonExpired" valuePropName="checked" initialValue={true} hidden><Checkbox/></Form.Item>}
                {(isCreate || isEnableUser || isDisableUser) && <Form.Item name="accountNonExpired" valuePropName="checked" initialValue={true} hidden><Checkbox/></Form.Item>}
                {(isCreate || isEnableUser || isDisableUser) && <Form.Item name="accountNonLocked" valuePropName="checked" initialValue={true} hidden><Checkbox/></Form.Item>}
            </Form>
        );
    };

    return (
        <div>
            <div style={{marginBottom: '10px'}}>
                <Button type="primary" onClick={showCreateModal}>{t('user.create')}</Button>
            </div>
            <Table
                columns={columns}
                rowKey={(record) => record.userId}
                dataSource={data}
                pagination={tableParams.pagination}
                loading={loading}
                onChange={handleTableChange}
                scroll={{x: 'max-content'}}
                size="small"
            />
            <Modal title={modalTitle} open={isModalOpen} onOk={handleOk} onCancel={handleCancel} okText={t('common.ok')} cancelText={t('common.cancel')}>
                <UserForm operationType={formType}/>
            </Modal>
        </div>
    );
};
export default User;
