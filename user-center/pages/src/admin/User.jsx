/* eslint-disable compat/compat */
import React, {useEffect, useMemo, useState} from 'react';
import {Table, Button, Modal, Form, Input, Select, Checkbox} from 'antd';
import {useTranslation, Trans} from 'react-i18next';

import {ACTION_COLUMN_WIDTH, OverflowTags, RowActions} from './_shared/tableLayout';

// Page-local width budgets for tag-style columns; row-action column
// width is shared across admin pages via ACTION_COLUMN_WIDTH.
const AUTHORITIES_COLUMN_WIDTH = 200;
const STATUS_COLUMN_WIDTH = 180;

const toURLSearchParams = (record) => {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(record)) {
        params.append(key, value);
    }
    return params;
};
const getRandomuserParams = (params) => ({
    results: params.pagination?.pageSize,
    page: params.pagination?.current,
    ...params,
});

const listUsers = async ({offset, limit}) => {
    return await fetch(`/admin/api/user/list?offset=${offset}&limit=${limit}`)
        .then(res => res.json())
        .then(rows => {
            return rows;
        });
    //return await mock();
};

const toUserRequestObject = (user) => {
    return {
        userId: user.userId,
        username: user.username,
        password: user.password,
        email: user.email,
        phone: user.phone,
        authorities: user.authorities?.map(a => {
            return {authority: a};
        }),
        accountNonExpired: user.accountNonExpired ?? false,
        accountNonLocked: user.accountNonLocked ?? false,
        credentialsNonExpired: user.credentialsNonExpired ?? false,
        enabled: user.enabled ?? false,
    };
}

const createUser = async (user) => {
    console.log("Create user:", user);
    const requestData = toUserRequestObject(user);
    return await fetch("/_csrf", {
        headers: {
            "Accept": "application/json"
        }
    })
        .then(res => res.json())
        .then(csrf => {
            return fetch(`/admin/api/user`, {
                method: 'POST',
                body: JSON.stringify(requestData),
                headers: {
                    "Content-Type": "application/json",
                    [csrf.headerName]: csrf.token
                },
            })
                .then(res => res.json())
                .then(resp => {
                    return resp;
                });
        })
}

const updateUser = async (user) => {
    console.log("Update user:", user);
    const requestData = toUserRequestObject(user);
    return await fetch("/_csrf", {
        headers: {
            "Accept": "application/json"
        }
    })
        .then(res => res.json())
        .then(csrf => {
            return fetch(`/admin/api/user`, {
                method: 'PUT',
                body: JSON.stringify(requestData),
                headers: {
                    "Content-Type": "application/json",
                    [csrf.headerName]: csrf.token
                },
            })
                .then(res => res.json())
                .then(resp => {
                    return resp;
                });
        })
}

const resetPassword = async (user) => {
    let requestData = {
        userId: user.userId,
        password: user.password
    };
    const encodedData = new URLSearchParams(requestData).toString();
    return await fetch("/_csrf", {
        headers: {
            "Accept": "application/json"
        }
    })
        .then(res => res.json())
        .then(csrf => {
            return fetch(`/admin/api/user/reset-password`, {
                method: 'POST',
                body: encodedData,
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                    [csrf.headerName]: csrf.token
                },
            })
                .then(res => {
                    const contentLength = res.headers.get("Content-Length");
                    if (contentLength === "0") {
                        return res.text();
                    }
                    return res.json();
                })
                .then(resp => {
                    return resp;
                });
        })
}

const deleteUser = async (userId) => {
    return await fetch("/_csrf", {
        headers: {
            "Accept": "application/json"
        }
    })
        .then(res => res.json())
        .then(csrf => {
            return fetch(`/admin/api/user?userId=${userId}`, {
                method: 'DELETE',
                headers: {
                    [csrf.headerName]: csrf.token
                },
            })
                .then(res => {
                    const contentLength = res.headers.get("Content-Length");
                    if (contentLength === "0") {
                        return res.text();
                    }
                    return res.json();
                })
                .then(resp => {
                    return resp;
                });
        })
}

async function mock() {
    return [
        {
            userId: "213",
            username: "test",
            authorities: [
                {
                    "authority": "user",
                    "name": "普通用户",
                    "description": "普通用户"
                }
            ]
        },
        {
            userId: "214",
            username: "test2",
            enabled: true,
            authorities: [
                {
                    "authority": "user",
                    "name": "普通用户",
                    "description": "普通用户"
                },

                {
                    "authority": "root",
                    "name": "管理员",
                    "description": "管理员"
                }
            ]
        }];
}

const User = () => {
    const {t} = useTranslation();
    const OP_TYPE_CREATE = 'create';
    const OP_TYPE_UPDATE = 'update';
    const OP_TYPE_RESET_PW = 'reset-pw';
    const OP_TYPE_ENABLE = 'enable';
    const OP_TYPE_DISABLE = 'disable';
    const OP_TYPE_DELETE = 'delete';

    const formItemLayout = {
        labelCol: {
            xs: {
                span: 24,
            },
            sm: {
                span: 6,
            },
        },
        wrapperCol: {
            xs: {
                span: 24,
            },
            sm: {
                span: 18,
            },
        },
    };


    const authorityLabel = (authority) => {
        const key = `user.role.${authority.authority}`;
        const translated = t(key);
        return translated === key ? (authority.name ?? authority.authority) : translated;
    };

    // The toggle action's visible label changes per row (enable / disable);
    // pin the layout measurement to the longer of the two so the row does
    // not jitter as users flip their enabled state.
    const toggleMeasureLabel = useMemo(() => {
        const disable = t('user.disable');
        const enable = t('user.enable');
        return disable.length >= enable.length ? disable : enable;
    }, [t]);

    const columns = useMemo(() => [
        {
            title: t('user.column.username'),
            dataIndex: 'username',
            sorter: true,
            width: 160,
        },
        {
            title: t('user.column.email'),
            dataIndex: 'email',
            width: 220,
        },
        {
            title: t('user.column.phone'),
            dataIndex: 'phone',
            width: 140,
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
                    if (!user.accountNonExpired) {
                        items.push({key: 'account-expired', label: t('user.status.accountExpired'), color: 'red'});
                    }
                    if (!user.accountNonLocked) {
                        items.push({key: 'account-locked', label: t('user.status.accountLocked'), color: 'red'});
                    }
                    if (!user.credentialsNonExpired) {
                        items.push({key: 'credentials-expired', label: t('user.status.credentialsExpired'), color: 'red'});
                    }
                }
                if (items.length === 0) {
                    items.push({key: 'normal', label: t('user.status.normal'), color: 'green'});
                }
                return <OverflowTags items={items} columnWidth={STATUS_COLUMN_WIDTH}/>;
            },
        },
        {
            title: t('user.column.action'),
            key: 'action',
            width: ACTION_COLUMN_WIDTH,
            fixed: 'right',
            render: (_, record) => {
                const toggleType = record.enabled ? OP_TYPE_DISABLE : OP_TYPE_ENABLE;
                const actions = [
                    {key: 'detail', label: t('user.detail'), onClick: () => showEditModal(record)},
                    {key: 'toggle', label: record.enabled ? t('user.disable') : t('user.enable'), onClick: () => showConfirmModal(record, toggleType)},
                    {key: 'reset', label: t('user.resetPassword'), onClick: () => showResetPasswordModal(record)},
                    {key: 'delete', label: t('user.delete'), danger: true, onClick: () => showConfirmModal(record, OP_TYPE_DELETE)},
                ];
                return (
                    <RowActions
                        actions={actions}
                        columnWidth={ACTION_COLUMN_WIDTH}
                        moreLabel={t('common.more')}
                        measureLabels={{toggle: toggleMeasureLabel}}
                    />
                );
            },
        },
    ], [t, toggleMeasureLabel]);

    const prefixSelector = (
        <Form.Item name="prefix" noStyle>
            <Select
                style={{
                    width: 70,
                }}
            >
                <Select.Option value="86">+86</Select.Option>
            </Select>
        </Form.Item>
    );

    const showCreateModal = () => {
        setFormType(OP_TYPE_CREATE)
        setModalTitle(t('user.modal.create'))
        form.resetFields();
        setIsModalOpen(true);
    };
    const showEditModal = (user) => {
        console.log("load data for edit:", user);
        setFormType(OP_TYPE_UPDATE)
        setModalTitle(t('user.modal.detail'))
        form.setFieldsValue({
            ...user,
            authorities: user.authorities.map(a => a.authority),
        });
        setIsModalOpen(true);
    };
    const showResetPasswordModal = (user) => {
        console.log("load data for reset password:", user);
        setFormType(OP_TYPE_RESET_PW)
        setModalTitle(t('user.modal.resetPassword'))
        form.setFieldsValue({
            userId: user.userId
        });
        setIsModalOpen(true);
    };
    const showConfirmModal = (user, operateType) => {
        console.log("load data for confirm:", user);
        setFormType(operateType)
        setModalTitle(null)
        form.setFieldsValue({
            ...user,
            authorities: null, //暂时不需要这个字段 user.authorities.map(a => a.authority),
        });
        setIsModalOpen(true);
    };
    const handleOk = () => {
        form.submit();
    };
    const handleCancel = () => {
        setIsModalOpen(false);
    };
    const onFinish = (userData) => {
        console.log("form submit:", userData);
        if (formType === OP_TYPE_CREATE) {
            createUser(userData)
                .then(resp => {
                    console.log("create user resp:", resp);
                    setIsModalOpen(false);
                    form.resetFields();
                    fetchData();
                })
        } else if (formType === OP_TYPE_UPDATE) {
            updateUser(userData)
                .then(resp => {
                    console.log("update user resp:", resp);
                    setIsModalOpen(false);
                    form.resetFields();
                    fetchData();
                })
        } else if (formType === OP_TYPE_RESET_PW) {
            resetPassword(userData)
                .then(resp => {
                    console.log("reset password resp:", resp);
                    setIsModalOpen(false);
                    form.resetFields();
                    fetchData();
                })
        } else if (formType === OP_TYPE_ENABLE || formType === OP_TYPE_DISABLE) {
            updateUser({
                ...userData,
                enabled: formType === OP_TYPE_ENABLE
            }).then(resp => {
                console.log("update user status resp:", resp);
                setIsModalOpen(false);
                form.resetFields();
                fetchData();
            })
        } else if (formType === OP_TYPE_DELETE) {
            deleteUser(userData.userId)
                .then(resp => {
                    console.log("delete user resp:", resp);
                    setIsModalOpen(false);
                    form.resetFields();
                    fetchData();
                })
        }
    };

    const [form] = Form.useForm();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [modalTitle, setModalTitle] = useState('');
    const [formType, setFormType] = useState(OP_TYPE_CREATE);
    const [data, setData] = useState();
    const [loading, setLoading] = useState(false);
    const [tableParams, setTableParams] = useState({
        pagination: {
            current: 1,
            pageSize: 10,
            total: 0
        },
    });
    const params = toURLSearchParams(getRandomuserParams(tableParams));
    const fetchData = () => {
        setLoading(true);
        const page = tableParams.pagination.current;
        const size = tableParams.pagination.pageSize;
        const offset = (page - 1) * size;
        const limit = size * 3;
        listUsers({offset: offset, limit: limit})
            .then(rows => {
                const total = rows.length < limit ?
                    offset + rows.length :
                    Math.max(tableParams.pagination.total, offset + rows.length);
                setData(rows.slice(0, size));
                setLoading(false);
                setTableParams({
                    ...tableParams,
                    pagination: {
                        ...tableParams.pagination,
                        total: total,
                        // 200 is mock data, you should read it from server
                        // total: data.totalCount,
                    },
                });
            })
    };
    useEffect(fetchData, [
        tableParams.pagination?.current,
        tableParams.pagination?.pageSize,
        tableParams?.sortOrder,
        tableParams?.sortField,
        JSON.stringify(tableParams.filters),
    ]);
    const handleTableChange = (pagination, filters, sorter) => {
        setTableParams({
            pagination,
            filters,
            sortOrder: Array.isArray(sorter) ? undefined : sorter.order,
            sortField: Array.isArray(sorter) ? undefined : sorter.field,
        });

        // `dataSource` is useless since `pageSize` changed
        if (pagination.pageSize !== tableParams.pagination?.pageSize) {
            setData([]);
        }
    };

    const UserForm = ({operationType}) => {
        const isCreate = operationType === OP_TYPE_CREATE;
        const isUpdate = operationType === OP_TYPE_UPDATE;
        const isResetPw = operationType === OP_TYPE_RESET_PW;
        const isEnableUser = operationType === OP_TYPE_ENABLE;
        const isDisableUser = operationType === OP_TYPE_DISABLE;
        const isDeleteUser = operationType === OP_TYPE_DELETE;

        const addHiddenUserIdInput = !isCreate;
        const addHiddenStatusInputs = isCreate || isUpdate || isEnableUser || isDisableUser;
        const showFullUserInputs = isCreate || isUpdate;
        const disabledUnmodifiableInputs = !isCreate;
        const showPasswordInputs = isCreate || isResetPw;

        return (
            <Form
                {...formItemLayout}
                form={form}
                onFinish={onFinish}
                initialValues={{
                    prefix: '86',
                }}
            >
                {isEnableUser ? (
                    <Trans i18nKey="user.confirm.enable" values={{username: form.getFieldValue('username')}} components={{code: <code/>}}/>
                ) : null}
                {isDisableUser ? (
                    <Trans i18nKey="user.confirm.disable" values={{username: form.getFieldValue('username')}} components={{code: <code/>}}/>
                ) : null}
                {isDeleteUser ? (
                    <Trans i18nKey="user.confirm.delete" values={{username: form.getFieldValue('username')}} components={{code: <code/>}}/>
                ) : null}
                {addHiddenUserIdInput ? (
                    <Form.Item
                        name="userId"
                        label="userId"
                        hidden={true}
                    >
                        <Input disabled={disabledUnmodifiableInputs}/>
                    </Form.Item>
                ) : null}
                {showFullUserInputs ? (<Form.Item
                        name="username"
                        label={t('user.form.username')}
                        rules={[
                            {
                                required: isCreate,
                                message: t('user.form.required.username'),
                            },
                        ]}
                    >
                        <Input disabled={disabledUnmodifiableInputs}/>
                    </Form.Item>
                ) : null}
                {showFullUserInputs ? (<Form.Item
                        name="email"
                        label={t('user.form.email')}
                        rules={[
                            {
                                required: false,
                            },
                        ]}
                    >
                        <Input/>
                    </Form.Item>
                ) : null}
                {showFullUserInputs ? (<Form.Item
                        name="phone"
                        label={t('user.form.phone')}
                        rules={[
                            {
                                required: false,
                                message: t('user.form.required.phone'),
                            },
                        ]}
                    >
                        <Input
                            addonBefore={prefixSelector}
                            style={{
                                width: '100%',
                            }}
                        />
                    </Form.Item>
                ) : null}
                {showPasswordInputs ? (
                    <Form.Item
                        name="password"
                        label={t('user.form.password')}
                        rules={[
                            {
                                required: true,
                                message: t('user.form.required.password'),
                            },
                        ]}
                        hasFeedback
                    >
                        <Input.Password autoComplete="new-password"/>
                    </Form.Item>
                ) : null}
                {showPasswordInputs ? (
                    <Form.Item
                        name="confirm"
                        label={t('user.form.confirm')}
                        dependencies={['password']}
                        hasFeedback
                        rules={[
                            {
                                required: true,
                                message: t('user.form.required.confirm'),
                            },
                            ({getFieldValue}) => ({
                                validator(_, value) {
                                    if (!value || getFieldValue('password') === value) {
                                        return Promise.resolve();
                                    }
                                    return Promise.reject(new Error(t('user.form.passwordMismatch')));
                                },
                            }),
                        ]}
                    >
                        <Input.Password/>
                    </Form.Item>
                ) : null}
                {showFullUserInputs ? (
                    <Form.Item
                        name="authorities"
                        label={t('user.form.authorities')}
                        rules={[
                            {
                                required: true,
                                message: t('user.form.required.authorities'),
                            },
                        ]}
                        initialValue={['user']}
                    >
                        <Select
                            mode="multiple"
                            allowClear
                            style={{
                                width: '100%',
                            }}
                            placeholder={t('user.form.authoritiesPlaceholder')}
                            options={[{label: t('user.role.user'), value: "user"}, {label: t('user.role.admin'), value: "admin"}]}
                        />
                    </Form.Item>
                ) : null}
                {addHiddenStatusInputs ? (
                    <Form.Item
                        name="enabled"
                        label="账号已启用"
                        valuePropName="checked"
                        initialValue={true}
                        hidden={true}
                    >
                        <Checkbox/>
                    </Form.Item>
                ) : null}
                {addHiddenStatusInputs ? (
                    <Form.Item
                        name="credentialsNonExpired"
                        label="密码未过期"
                        valuePropName="checked"
                        initialValue={true}
                        hidden={true}
                    >
                        <Checkbox/>
                    </Form.Item>
                ) : null}
                {addHiddenStatusInputs ? (
                    <Form.Item
                        name="accountNonExpired"
                        label="账号未过期"
                        valuePropName="checked"
                        initialValue={true}
                        hidden={true}
                    >
                        <Checkbox/>
                    </Form.Item>
                ) : null}
                {addHiddenStatusInputs ? (
                    <Form.Item
                        name="accountNonLocked"
                        label="账号未锁定"
                        valuePropName="checked"
                        initialValue={true}
                        hidden={true}
                    >
                        <Checkbox/>
                    </Form.Item>
                ) : null}
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
            />
            <Modal title={modalTitle} open={isModalOpen} onOk={handleOk} onCancel={handleCancel} okText={t('common.ok')} cancelText={t('common.cancel')}>
                <UserForm operationType={formType}/>
            </Modal>
        </div>
    );
};
export default User;