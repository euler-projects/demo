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
    <div class="row-1 col-4">
        <label for="mobile">${e:i18n('_MOBILE')}</label><input
            id="mobile" type="text" name="mobile">
    </div>
    <form method="get"
        action="${__CONTEXT_PATH}/reset-password?type=SMS">
        <input type="hidden" value="SMS" name="type">
        <div>
            <label for="pin">${e:i18n('_SMS_PIN')}</label><input
                id="pin" type="text" name="pin">
        </div>
        <div>
            <input type="submit" value="${e:i18n('_SUBMIT')}">
        </div>
    </form>
</body>
</html>