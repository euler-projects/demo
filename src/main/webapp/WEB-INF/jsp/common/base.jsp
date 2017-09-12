<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="e" uri="http://eulerframework.net/tags" %>
<jsp:useBean id="__NOW" class="java.util.Date"></jsp:useBean>   

<%-- <c:if test="${__DEBUG_MODE}">
<style>
#__sys-info{
    position: fixed;
    bottom: 0;
    right: 0;
    padding-right:2px;
    height: 20px;
    line-height: 20px;
    font-size: 0.6em;
    font-family: Arial,'Microsoft YaHei','微软雅黑',sans-serif;
    color: #d1d1d1;
    z-index: 999999999;
    filter: opacity(0.5);
}

#__sys-info a,
#__sys-info a:visited {
    color: #d1d1d1;
    text-decoration: none;
}

#__sys-info a:hover {
    color: #d1d1d1;
    text-decoration: underline;
}

</style>
<div id="__sys-info">
<span>Ver. ${__PROJECT_VERSION}</span>
<span style="font-weight:bold; font-style:italic;">${__PROJECT_MODE}</span>
<span>Bulid.${__PROJECT_BUILDTIME}</span>
<span><a href="https://eulerproject.io">Euler Framework ${__FRAMEWORK_VERSION}</a></span>
</div>
<script>
if (self != top) {
    document.getElementById('__sys-info').style.display= 'none';
}
</script>
</c:if> --%>
