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
<div class="info">${e:i18n(__message)}</div>
<div class="info">${e:i18n('_PAGE_WILL_REDIRECT')}&nbsp;<span id="wait">${__wait}</span>&nbsp;S</div>
<div class="target-list">
    <span><a href="${__target.href}" id="target">${e:i18n('_REDIRECT_NOW')}</a></span>
    <span><a href="${__CONTEXT_PATH}/">${e:i18n('_GO_HOME')}</a></span>    
</div>

<script type="text/javascript">
(function(){
var wait = document.getElementById('wait'),href = document.getElementById('target').href;
var interval = setInterval(function(){
    var time = --wait.innerHTML;
    if(time <= 0) {
        location.href = href;
        clearInterval(interval);
    };
}, 1000);
})();
</script>
</body>
</html>