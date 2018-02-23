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
    <form method="post" action="${__CONTEXT_PATH}/reset-password-email">
        <div class="row-1 col-4">
            <label for="email">${e:i18n('_EMAIL')}</label><input id="email" type="text" name="email">
        </div>
        <div>
            <input type="submit" value="${e:i18n('_SUBMIT')}">
            <input type="button" value="${e:i18n('_CANCEL')}" onclick="history.go(-1);">
        </div>
    </form>
</body>
</html>