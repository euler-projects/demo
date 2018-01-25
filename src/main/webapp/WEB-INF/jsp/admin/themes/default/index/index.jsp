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
                <span>${__USER_INFO.username}</span>
                <%-- <span><a href="${__CONTEXT_PATH}/settings/account/change-password">${e:i18n('_CHANGE_PASSWORD')}</a></span> --%>
                <span><a href="${__CONTEXT_PATH}/signout">${e:i18n('_SIGN_OUT')}</a></span>
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
                <li><a href="javascript:void(0)" onclick="addTab('cms/slide/slideManage', '${e:i18n('_ADMIN_SLIDE_MANAGE')}')">${e:i18n('_ADMIN_SLIDE_MANAGE')}</a></li>
                <security:authorize access="hasAnyAuthority('ROOT') ">
                <li><a href="javascript:void(0)" onclick="addTab('cms/slide/slideTypeManage', '${e:i18n('_ADMIN_SLIDE_TYPE_MANAGE')}')">${e:i18n('_ADMIN_SLIDE_TYPE_MANAGE')}</a></li>
                </security:authorize>
                <li><a href="javascript:void(0)" onclick="addTab('cms/post/postManage', '${e:i18n('_ADMIN_POST_MANAGE')}')">${e:i18n('_ADMIN_POST_MANAGE')}</a></li>
                <security:authorize access="hasAnyAuthority('ROOT') ">
                <li><a href="javascript:void(0)" onclick="addTab('cms/post/postTypeManage', '${e:i18n('_ADMIN_POST_TYPE_MANAGE')}')">${e:i18n('_ADMIN_POST_TYPE_MANAGE')}</a></li>
                </security:authorize>
            </ul>
            <ul>
                <li><a href="javascript:void(0)" onclick="addTab('authentication/user/userManage', '${e:i18n('_ADMIN_USER_MANAGE')}')">${e:i18n('_ADMIN_USER_MANAGE')}</a></li>
                <c:if test="${__DEBUG_MODE}">
                <li><a href="javascript:void(0)" onclick="addTab('security/group/groupManage', '${e:i18n('_ADMIN_GROUP_MANAGE')}')">${e:i18n('_ADMIN_GROUP_MANAGE')}</a></li>
                <li><a href="javascript:void(0)" onclick="addTab('security/authority/authorityManage', '${e:i18n('_ADMIN_AUTHORITY_MANAGE')}')">${e:i18n('_ADMIN_AUTHORITY_MANAGE')}</a></li>
                </c:if>
                <li><a href="${__CONTEXT_PATH}/">${e:i18n('_ADMIN_RETURN_HOME')}</a></li>
            </ul>
        </div>
    </div>
    <div id="content-zone"  data-options="region:'center',split:false">
        <div id="main-content" class="easyui-tabs" data-options="collapsible:false" style="width:100%;height:100%;">
            <div id="welcome-tab" title="${e:i18n('_ADMIN_WELCOME')}" data-options="closable:true">
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