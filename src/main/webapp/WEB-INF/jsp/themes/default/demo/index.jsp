<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">

<head>

<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link href="${__ASSETS_PATH}/lib/bootstrap/css/bootstrap.min.css"
    rel="stylesheet">

<title>${__SITENAME}</title>

</head>
<body>
Hello World!<br>
<code>
/<br>
|--<a href="${__CONTEXT_PATH}/signup">signup</a> 注册<br>
|--<a href="${__CONTEXT_PATH}/signup?t=basic">signup?t=basic</a> 带有基本用户信息的注册页<br>
|--<a href="${__CONTEXT_PATH}/signin">signin</a> 登录<br>
|--<a href="${__CONTEXT_PATH}/signout">signout</a> 注销<br>
|--<a href="${__CONTEXT_PATH}/reset-password">reset-password</a> 重置密码<br>
|--settings 设置相关<br>
|  |--account 账号相关<br>
|  |  |--<a href="${__CONTEXT_PATH}/settings/account/change-password">change-password</a> 修改密码<br>
|--error-{errorCode} 错误页面<br>
|<br>
|--<a href="${__CONTEXT_PATH}/admin/">admin/</a> 后台管理<br>
</code>
    <script src="${__ASSETS_PATH}/lib/jquery/jquery-1.12.4.min.js"></script>
    <script src="${__ASSETS_PATH}/lib/bootstrap/js/bootstrap.min.js"></script>
</body>
</html>