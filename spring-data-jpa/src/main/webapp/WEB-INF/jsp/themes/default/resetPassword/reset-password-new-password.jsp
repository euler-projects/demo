<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import = "org.eulerframework.web.module.authentication.enums.ResetPasswordType" %>
<!DOCTYPE html>
<html lang="en">

<head>

<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<title>${__SITENAME}</title>

</head>
<body>
    <div>
        <c:set var="sms" value="<%=ResetPasswordType.SMS %>"/> 
        <c:set var="email" value="<%=ResetPasswordType.EMAIL %>"/> 
        <form method="post" action="${__CONTEXT_PATH}/reset-password">
            <input type="hidden" value="${type}" name="type">
            <c:if test="${type eq email}">
                <input type="hidden" value="${token}" name="token">
            </c:if>
            <c:if test="${type eq sms}">
                <input type="hidden" value="${pin}" name="pin">
            </c:if>
            <div>
                <label class="" for="password">${e:i18n('_NEW_PASSWORD')}</label><input
                    id="password" type="password" name="password">
            </div>
            <div>
                <label for="confirm_password">${e:i18n('_CONFIRM_PASSWORD')}</label><input 
                    id="confirm_password" type="password" name="">

            </div>
            <div>
                <input type="submit" value="${e:i18n('_SUBMIT')}">
                <input type="button" value="${e:i18n('_CANCEL')}" onclick="history.go(-1);">
            </div>
        </form>
</body>
</html>