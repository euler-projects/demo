<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">

<head>

<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">

<title>${__SITENAME}</title>

</head>
<body>
    <form action="${__CONTEXT_PATH}/signup" method="post">
        <p>
            <label for="username">Username</label> <input type="text"
                id="username" name="username" />
        </p>
        <p>
            <label for="password">Password</label> <input
                type="password" id="password" name="password" />
        </p>
        <button type="submit" class="btn">${e:i18n('_SIGN_UP')}</button>
    </form>
</body>
</html>