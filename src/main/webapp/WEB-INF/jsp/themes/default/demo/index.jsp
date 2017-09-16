<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>${__SITENAME}</title>
</head>
<body>
Hello World!<br>
<code>
/<br>
|--signup 注册<br>
|--signin 登录<br>
|--<a href="${__CONTEXT_PATH}/signout">signout</a> 注销<br>
|--<a href="${__CONTEXT_PATH}/reset-password">reset-password</a> 重置密码<br>
|--settings 设置相关<br>
|  |--account 账号相关<br>
|  |  |--<a href="${__CONTEXT_PATH}/settings/account/change-password">change-password</a> 修改密码<br>
|--error-{errorCode} 错误页面<br>
|<br>
|--<a href="${__CONTEXT_PATH}/admin/">admin/</a> 后台管理<br>
</code>
</body>
</html>