<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>

<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">

    <%@ include file="/WEB-INF/jsp/admin/themes/default/common/css-global.jsp"%>
    <%@ include file="/WEB-INF/jsp/admin/themes/default/common/css-dash.jsp"%>

    <title></title>

    <style>
        .add-btn {
            color: #444!important;
        }
    </style>
</head>

<body class="easyui-layout">
    <div data-options="region:'west',collapsible:false" style="width:48%;">
        <ul id="dl-authority-available" class="authority-list" title="${e:i18n('_ADMIN_DASHBOARD_USER_AUTH_AVAILABLE')}">
            <li value="AL">Alabama</li>
            <li value="AK">Alaska</li>
            <li value="AZ">Arizona</li>
            <li value="AR">Arkansas</li>
            <li value="CA">California</li>
            <li value="CO">Colorado</li>
        </ul>
    </div>
    <div data-options="region:'center'" style="width:4%;height:100%;">
        <div style="position: relative; height:180px; top:50%; margin-top:-90px; text-align: center;  line-height: 40px;">
            <div style="position: relative; height: 80px; top: 0">
                <a id="rm-selected" title="${e:i18n('_ADMIN_DASHBOARD_USER_AUTH_RM_SELECTED')}" href="#" class="easyui-linkbutton add-btn">&lt;</a>
                <a id="rm-all" title="${e:i18n('_ADMIN_DASHBOARD_USER_AUTH_RM_ALL')}" href="#" class="easyui-linkbutton add-btn">&lt;&lt;</a>
            </div>
            <div style="position: relative; height: 80px; top: 20px">
                <a id="add-selected" title="${e:i18n('_ADMIN_DASHBOARD_USER_AUTH_ADD_SELECTED')}" href="#" class="easyui-linkbutton add-btn">&gt;</a>
                <a id="add-all" title="${e:i18n('_ADMIN_DASHBOARD_USER_AUTH_ADD_ALL')}" href="#" class="easyui-linkbutton add-btn">&gt;&gt;</a>
            </div>
        </div>
    </div>
    <div data-options="region:'east',collapsible:false" style="width:48%;">
        <ul id= "dl-authority-active" class="authority-list" title="${e:i18n('_ADMIN_DASHBOARD_USER_AUTH_ACTIVE')}">
            <li value="AL">Alabama</li>
            <li value="AK">Alaska</li>
            <li value="AZ">Arizona</li>
            <li value="AR">Arkansas</li>
            <li value="CA">California</li>
            <li value="CO">Colorado</li>
        </ul>
    </div>

<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-global.jsp"%>
<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-dash.jsp"%>
<script>
    $('.add-btn').linkbutton({
        height: '25px',
        width: '25px'
    });

    $('#dl-authority-available').datalist({
        url: '${__ADMIN_PATH}/ajax/authentication/group/findGroupByPage?pageSize=9999999&pageIndex=0',
        height: '100%',
        width: '100%',
        checkbox: true,
        singleSelect: false,
        valueField: 'code',
        textField: 'name'
    });

    $('#dl-authority-active').datalist({
        url: '${__ADMIN_PATH}/ajax/authentication/group/findUserGroupByPage?pageSize=9999999&pageIndex=0&query.userId=' + euler.getUrlParam('userId'),
        height: '100%',
        width: '100%',
        checkbox: true,
        singleSelect: false,
        valueField: 'code',
        textField: 'name'
    });

    function onEulerIframeDlgConfirm(callback) {
        var result = {};
        result.userId = euler.getUrlParam('userId');
        var selectedGroupCodes = [];
        var data = $('#dl-authority-active').datalist('getData');
        if(data != null && data.rows != null) {
            for(var i = 0; i < data.rows.length; i++) {
                selectedGroupCodes[i] = data.rows[i].code;
            }
        }
        result.selectedGroupCodes = selectedGroupCodes;
        callback(result);
    }
</script>
</body>

</html>