<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">

<head>

<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link href="${__ASSETS_PATH}/lib/bootstrap/css/bootstrap.min.css" rel="stylesheet">
<link href="${__ASSETS_PATH}/css/themes/default/demo/single-form.css" rel="stylesheet">

<title>${__SITENAME}</title>

</head>

<body>
    <div class="wrapper">
        <div class="wrapper-inner">
            <div class="title-wrapper">
                <span class="">${__SITENAME}</span>
            </div>
            <div class="form-wrapper">
                <form action="${__CONTEXT_PATH}/signin" class="full-line-form" method="post">
                    <div class="form-group">
                        <input type="text" name="username" class="form-control" id="username" placeholder="${e:i18n('_USERNAME')}">
                    </div>
                    <div class="form-group">
                        <input type="password" name="password" class="form-control" id="password" placeholder="${e:i18n('_PASSWORD')}">
                    </div>
                    <input type="hidden" name="${_csrf.parameterName}"
                        value="${_csrf.token}" />
                    <div class="form-group">
                        <button type="submit" class="btn btn-success">${e:i18n('_SIGN_IN')}</button>
                    </div>
                </form>
            </div>
            <c:if test="${param.error != null}">
                <div class="info-wrapper">
                    <div class="failure-info">Invalid username or password.</div>
                </div>
            </c:if>
        </div>
        <footer
            class="navbar navbar-inverse navbar-fixed-bottom footer-wrapper">
            <span><a href="${__CONTEXT_PATH}/">${__SITENAME}<%-- &nbsp;${__PROJECT_VERSION} --%></a></span>
            <span>&copy;<fmt:formatDate value="${__NOW}" pattern="yyyy" />&nbsp;${__COPYRIGHT_HOLDER}</span>
        </footer>
    </div>

    <script src="${__ASSETS_PATH}/lib/jquery/jquery-1.12.4.min.js"></script>
    <script src="${__ASSETS_PATH}/lib/bootstrap/js/bootstrap.min.js"></script>
</body>
</html>