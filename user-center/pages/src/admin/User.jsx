/* eslint-disable compat/compat */
import React, {useEffect, useState} from 'react';
import {Table, Space, Button, Modal, Tag, Form, Input, Select, Checkbox} from 'antd';

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
    return await fetch(`/admin/user/list?offset=${offset}&limit=${limit}`)
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
            return fetch(`/admin/user`, {
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
            return fetch(`/admin/user`, {
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
            return fetch(`/admin/user/reset-password`, {
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
            return fetch(`/admin/user?userId=${userId}`, {
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


    const columns = [
        {
            title: 'Username',
            dataIndex: 'username',
            sorter: true,
            width: '20%',
        },
        {
            title: 'Email',
            dataIndex: 'email',
        },
        {
            title: 'Phone',
            dataIndex: 'phone',
        },
        {
            title: 'Authorities',
            dataIndex: 'authorities',
            width: '15%',
            render: (authorities) => (<>{authorities.map(authority => {
                if (authority.authority === 'root' || authority.authority === 'admin') {
                    return <Tag key={authority.authority} color={'red'}>{authority.name}</Tag>;
                } else {
                    return <Tag key={authority.authority}>{authority.name}</Tag>;
                }
            })}</>)
        },
        {
            title: 'Status',
            dataIndex: 'enabled',
            width: '10%',
            render: (_, user) => {
                let tags = [];
                if (!user.enabled) {
                    tags.push(
                        <Tag key={'tag-disabled'}>
                            账号已禁用
                        </Tag>
                    )
                } else {
                    if (!user.accountNonExpired) {
                        tags.push(
                            <Tag key={'tag-account-expired'} color={'red'}>
                                账号已过期
                            </Tag>
                        )
                    }
                    if (!user.accountNonLocked) {
                        tags.push(
                            <Tag key={'tag-account-locked'} color={'red'}>
                                账号已锁定
                            </Tag>
                        )
                    }
                    if (!user.credentialsNonExpired) {
                        tags.push(
                            <Tag key={'tag-credentials-expired'} color={'red'}>
                                密码已过期
                            </Tag>
                        )
                    }
                }
                if (tags.length === 0) {
                    tags.push(
                        <Tag key={'tag-normal'} color={'green'}>
                            正常
                        </Tag>
                    );
                }

                return <>{tags}</>;
            },
        },
        {
            title: 'Action',
            key: 'action',
            width: '20%',
            render: (_, record) => (
                <Space size="middle">
                    <a onClick={() => showEditModal(record)}>详情</a>
                    <a onClick={() => showResetPasswordModal(record)}>重置密码</a>
                    {disableOrEnable(record)}
                    <a onClick={() => showConfirmModal(record, OP_TYPE_DELETE)}>删除</a>
                </Space>
            ),
        },
    ];

    const disableOrEnable = (user) => {
        if (user.enabled) {
            return <a onClick={() => showConfirmModal(user, OP_TYPE_DISABLE)}>禁用</a>
        } else {
            return <a onClick={() => showConfirmModal(user, OP_TYPE_ENABLE)}>启用</a>
        }
    }

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
        setModalTitle('创建新用户')
        form.resetFields();
        setIsModalOpen(true);
    };
    const showEditModal = (user) => {
        console.log("load data for edit:", user);
        setFormType(OP_TYPE_UPDATE)
        setModalTitle('用户详情')
        form.setFieldsValue({
            ...user,
            authorities: user.authorities.map(a => a.authority),
        });
        setIsModalOpen(true);
    };
    const showResetPasswordModal = (user) => {
        console.log("load data for reset password:", user);
        setFormType(OP_TYPE_RESET_PW)
        setModalTitle('重置密码')
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
    const [modalTitle, setModalTitle] = useState("新增用户");
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
                    <>确认启用用户 <code>{form.getFieldValue('username')}</code> ?</>
                ) : null}
                {isDisableUser ? (
                    <>确认禁用用户 <code>{form.getFieldValue('username')}</code> ?</>
                ) : null}
                {isDeleteUser ? (
                    <>确认删除用户 <code>{form.getFieldValue('username')}</code> ?</>
                ) : null}
                {addHiddenUserIdInput ? (
                    <Form.Item
                        name="userId"
                        label="用户ID"
                        hidden={true}
                    >
                        <Input disabled={disabledUnmodifiableInputs}/>
                    </Form.Item>
                ) : null}
                {showFullUserInputs ? (<Form.Item
                        name="username"
                        label="用户名"
                        rules={[
                            {
                                required: isCreate,
                            },
                        ]}
                    >
                        <Input disabled={disabledUnmodifiableInputs}/>
                    </Form.Item>
                ) : null}
                {showFullUserInputs ? (<Form.Item
                        name="email"
                        label="邮箱"
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
                        label="手机号"
                        rules={[
                            {
                                required: false,
                                message: 'Please input your phone number!',
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
                        label="密码"
                        rules={[
                            {
                                required: true,
                                message: 'Please input your password!',
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
                        label="确认密码"
                        dependencies={['password']}
                        hasFeedback
                        rules={[
                            {
                                required: true,
                                message: 'Please confirm your password!',
                            },
                            ({getFieldValue}) => ({
                                validator(_, value) {
                                    if (!value || getFieldValue('password') === value) {
                                        return Promise.resolve();
                                    }
                                    return Promise.reject(new Error('The new password that you entered do not match!'));
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
                        label="权限"
                        rules={[
                            {
                                required: true,
                                message: '请至少选择一个权限项',
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
                            placeholder="请选择用户权限"
                            //onChange={handleChange}
                            options={[{label: "普通用户", value: "user"}, {label: "管理员", value: "admin"}]}
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
                <Button type="primary" onClick={showCreateModal}>新增用户</Button>
            </div>
            <Table
                columns={columns}
                rowKey={(record) => record.userId}
                dataSource={data}
                pagination={tableParams.pagination}
                loading={loading}
                onChange={handleTableChange}
            />
            <Modal title={modalTitle} open={isModalOpen} onOk={handleOk} onCancel={handleCancel}>
                <UserForm operationType={formType}/>
            </Modal>
        </div>
    );
};
export default User;