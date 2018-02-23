<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">

<head>

<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<%@ include file="/WEB-INF/jsp/themes/default/common/css-global.jsp"%>
<%@ include file="/WEB-INF/jsp/themes/default/common/css-infoPage.jsp"%>

<title>${__SITENAME}</title>

</head>
<body>
<div class="title error">404</div>
<div class="target-list">
    <span><a href="${__CONTEXT_PATH}/">${e:i18n('_GO_HOME')}</a></span>
</div>
</body>
</html>