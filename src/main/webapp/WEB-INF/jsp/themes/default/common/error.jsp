<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>${__SITENAME}</title>
</head>
<body>
<div class="title error">ERROR</div>
<div class="info">${e:i18n('_ERROR_MESSAGE')}:&nbsp;${__error_description}</div>
<div class="info">${e:i18n('_ERROR_CODE')}:&nbsp;${__error} (${__code})</div>
<div class="target-list">
    <span><a href="${__CONTEXT_PATH}/">${e:i18n('_GO_HOME')}</a></span>
</div>
</body>
</html>