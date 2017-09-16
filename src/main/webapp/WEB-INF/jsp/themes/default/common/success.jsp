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
<div class="title success">SUCCESS</div>
<div class="info">${e:i18n(__message)}</div>
<div class="target-list">
<c:forEach items="${__targets}" var="target">
    <span><a href="${target.href}">${e:i18n(target.name)}</a></span>
</c:forEach>
</div>
</body>
</html>