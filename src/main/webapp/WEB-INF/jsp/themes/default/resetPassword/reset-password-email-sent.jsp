<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">

<head>

<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<%@ include file="/WEB-INF/jsp/themes/default/common/css-global.jsp"%>
<%@ include file="/WEB-INF/jsp/themes/default/common/css-infoPage.jsp"%>
    <link rel="stylesheet" href="${__ASSETS_PATH}/css/themes/default/g.css">
<title>${__SITENAME}</title>

</head>
<body>
<%@ include file="/WEB-INF/jsp/themes/default/common/header.jsp"%>
<div class="wra">
<div class="title success">SUCCESS</div>
<div class="info">Password reset email has been sent if the Email address is correct.</div>
<div class="targets">
    <span class="target"><a href="${__CONTEXT_PATH}/">${e:i18n('_GO_HOME')}</a></span>
</div>
</div>
</body>
</html>