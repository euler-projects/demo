<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    
    <%@ include file="/WEB-INF/jsp/admin/themes/default/common/css-global.jsp"%>
    <link rel="stylesheet" href="${__ASSETS_PATH}/css/admin/themes/default/demo/index/index.css">

    <title>${__ADMIN_DASHBOARD_BRAND_TEXT}</title>

</head>

<body class="easyui-layout">
    <div id="header-zone" data-options="region:'north',split:false,collapsible:false">
        <div id="header">
            <a class="site-brand" href="${__ADMIN_PATH}/">
                <span class="site-brand-icon"><img src="${__ADMIN_DASHBOARD_BRAND_ICON}"></span><span class="site-brand-text">${__ADMIN_DASHBOARD_BRAND_TEXT}</span>
            </a>
            <span id="user-info">
                <span>${__USERINFO.username}</span>&nbsp;&nbsp;<span><a href="${__CONTEXT_PATH}/signout">${e:i18n('_SIGN_OUT')}</a></span>
            </span>
        </div>
    </div>
    <div id="footer-zone" data-options="region:'south',split:false,collapsible:false">
        <div id="footer">           
            <span><a href="${__CONTEXT_PATH}/">${__SITENAME}<%-- &nbsp;${__PROJECT_VERSION} --%></a></span>
            <span>&copy;<fmt:formatDate value="${__NOW}" pattern="yyyy" />&nbsp;${__COPYRIGHT_HOLDER}</span>
            <%-- <span>Powered by <a href="https://eulerproject.io">Euler Framework ${__FRAMEWORK_VERSION}</a></span> --%>
        </div>
    </div>
    <div id="menu-zone" data-options="region:'west',split:false,collapsible:false">
        <div id="menu"> 
            <ul>
                <li><a href="javascript:void(0)" onclick="addTab('cmf/slide/slideManage', '图片管理')">图片管理</a></li>
                <li><a href="javascript:void(0)" onclick="addTab('cmf/slide/slideTypeManage', '图片类型管理')">图片类型管理</a></li>
                <li><a href="javascript:void(0)" onclick="addTab('cmf/post/postManage', '文章管理')">文章管理</a></li>
                <li><a href="javascript:void(0)" onclick="addTab('cmf/post/typeManage', '文章类型管理')">文章类型管理</a></li>
            </ul>
            <ul>
                <li><a href="javascript:void(0)" onclick="addTab('security/user', '账号管理')">账号管理</a></li>
                <%-- <c:if test="${__DEBUG_MODE}">
                <li><a href="javascript:void(0)" onclick="addTab('security/group', 'Groups')">用户组管理</a></li>
                <li><a href="javascript:void(0)" onclick="addTab('security/authority', 'Authorities')">权限管理</a></li>
                </c:if> --%>
                <li><a href="${__CONTEXT_PATH}/">返回首页</a></li>
            </ul>
        </div>
    </div>
    <div id="content-zone"  data-options="region:'center',split:false">
        <div id="main-content" class="easyui-tabs" data-options="collapsible:false" style="width:100%;height:100%;">
            <div id="welcome-tab" title="欢迎" data-options="closable:true">
                <h3 style="color:#0088CC/* #0099FF */;">${__ADMIN_DASHBOARD_BRAND_TEXT}</h3>
                <p></p>
            </div>
        </div>
    </div>
<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-global.jsp"%>
    
    <script>
        
        function addTab(url, title) {
            
            var html = '<iframe width="100%" height="100%" frameborder="no" border="0" marginwidth="0" marginheight="0" allowtransparency="yes" src="'+url+'"></iframe>';
            var exists = $('#main-content').tabs('exists', title);
            if(exists){
                $('#main-content').tabs('select', title);
                var tab = $('#main-content').tabs('getSelected');
                $('#main-content').tabs('update', {
                    tab: tab,
                    options: {
                        title: title,
                        content:html,
                    }
                });
                return;
            }
            
            $('#main-content').tabs('add',{
                title:title,
                content:html,
                closable:true
            });
            
        }
    </script>
</body>
</html>