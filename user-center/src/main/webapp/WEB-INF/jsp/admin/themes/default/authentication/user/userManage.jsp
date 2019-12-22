<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>

<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    
    <%@ include file="/WEB-INF/jsp/admin/themes/default/common/css-global.jsp"%>
    <%@ include file="/WEB-INF/jsp/admin/themes/default/common/css-dash.jsp"%>

    <title></title>

</head>

<body class="easyui-layout">
<div id="e-dg-zone" data-options="region:'north',split:true" style="background:#eee;">
    <div id="e-dg-tb">
        <div class="btns">
            <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" onclick="onSearch()"><i class="fa fa-refresh"></i>${e:i18n('_ADMIN_DASHBOARD_REFRESHE')}</a>
            <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" onclick="onAdd()"><i class="fa fa-plus"></i>${e:i18n('_ADMIN_DASHBOARD_ADD')}</a>
            <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" onclick="onDelete()"><i class="fa fa-remove"></i>${e:i18n('_ADMIN_DASHBOARD_DEL')}</a>
        </div>
        <div class="filter">
            <%-- <span class="e-dg-tb-label">${e:i18n('_ADMIN_DASHBOARD_AUTHORITY_GROUP')}</span>
            <select id="e-dg-tb-filter-usertype" class="easyui-combobox e-dg-tb-filter" data-filter-name="groupId" style="width:80px;" 
                data-options="
                    panelHeight:'auto',
                    panelMaxHeight:'200px',
                    valueField: 'id',
                    textField: 'name'">
            </select> --%>
            <span class="e-dg-tb-label">${e:i18n('_ADMIN_DASHBOARD_USER_STATUS')}</span>
            <select id="e-dg-tb-filter-userstatus" class="easyui-combobox e-dg-tb-filter" data-filter-name="enabled" style="width:80px;" data-options="panelHeight:'auto',panelMaxHeight:'200px'">
                <option value="">${e:i18n('_ADMIN_DASHBOARD_FILTER_ALL')}</option>
                <option value="true">${e:i18n('_ADMIN_DASHBOARD_USER_STATUS_ACTIVED')}</option>
                <option value="false">${e:i18n('_ADMIN_DASHBOARD_USER_STATUS_BLOCKED')}</option>
            </select>
        </div>
        <div class="searcher">
            <input id="e-dg-tb-searcher" class="easyui-searchbox" style="width:280px"
            data-options="searcher:onSearch ,prompt:'${e:i18n('_ADMIN_DASHBOARD_SEARCHER_USERMANAGE_PROMPT')}',menu:'#ss-menu'"></input>
            <div id="ss-menu" style="width:30px">
                <div data-options="name:'_all'">${e:i18n('_ADMIN_DASHBOARD_SEARCHER_ALL')}</div>
                <div data-options="name:'username'">${e:i18n('_ADMIN_DASHBOARD_USER_USERNAME')}</div>
                <div data-options="name:'mobile'">${e:i18n('_ADMIN_DASHBOARD_USER_MOBILE')}</div>
                <div data-options="name:'email'">${e:i18n('_ADMIN_DASHBOARD_USER_EMAIL')}</div>
            </div>
        </div>
    </div>
    <%-- 数据表格控件,务必不要加class="easyui-datagrid",避免提前初始化 --%>
    <table id="e-dg" data-options="
            fit:true,
            url:'${__ADMIN_PATH}/ajax/authentication/user/findUserByPage',
            toolbar:'#e-dg-tb',
            fitColumns:true,
            rownumbers:true,
            remoteSort:false,
            pagination:true,
            singleSelect:true,
            onClickRow:onClickRow,
            onRowContextMenu:onRowContextMenu">
        <thead>
            <tr>
                <th data-options="field:'userId',hidden:true">ID</th>
                <th data-options="field:'username',align:'center', width:'200px'">${e:i18n('_ADMIN_DASHBOARD_USER_USERNAME')}</th>
                <th data-options="field:'email',align:'center', width:'200px'">${e:i18n('_ADMIN_DASHBOARD_USER_EMAIL')}</th>
                <th data-options="field:'mobile',align:'center', width:'200px'">${e:i18n('_ADMIN_DASHBOARD_USER_MOBILE')}</th>
                <th data-options="field:'enabled',align:'center', width:'80px',formatter:userStatusFormatter">${e:i18n('_ADMIN_DASHBOARD_USER_STATUS')}</th>
            </tr>
        </thead>
    </table>
</div>
<div id="e-ds-zone" data-options="region:'center'">
    <p style="padding-left:20px;"><a href="javascript:void(0)" onClick="onAdd()">${e:i18n('_ADMIN_DASHBOARD_ADD')}</a>&nbsp;${e:i18n('_ADMIN_DASHBOARD_OR')}&nbsp;${e:i18n('_ADMIN_DASHBOARD_SELECT_TO_EDIT')}</p> 
    <div id="e-ds-dlg-add-user" class="easyui-dialog e-ds-dlg" title="${e:i18n('_ADMIN_DASHBOARD_ADD')}"
        data-options="
            closed:true,
            closable:true,
            draggable:false,
            resizable:false,
            modal:false,
            inline:true,
            border:false,
            onClose:onAddUserDlgClose,
            maximized:true">
        <form id="e-ds-dlg-add-user-fm" method="post">
            <input id="e-ds-dashInputId" type="hidden" name="userId">
            <div class="e-ds-dlg-line">
                <input class="easyui-textbox e-ds-dlg-input" name="username"
                data-options="required:true,label:'${e:i18n('_ADMIN_DASHBOARD_USER_USERNAME')}',labelAlign:'right'">
            </div>
            <div class="e-ds-dlg-line">
                <input class="easyui-textbox e-ds-dlg-input" name="email"
                data-options="label:'${e:i18n('_ADMIN_DASHBOARD_USER_EMAIL')}',labelAlign:'right'">
            </div>
            <div class="e-ds-dlg-line">
                <input class="easyui-textbox e-ds-dlg-input" name="mobile"
                data-options="label:'${e:i18n('_ADMIN_DASHBOARD_USER_MOBILE')}',labelAlign:'right'">
            </div>
            <div class="e-ds-dlg-line" id="add-user-fm-password-line">
                <input class="easyui-textbox e-ds-dlg-input" name="password" id="add-user-fm-password"
                data-options="required:true,type:'password',label:'${e:i18n('_ADMIN_DASHBOARD_USER_PASSWORD')}',labelAlign:'right'">
            </div>
            <div class="e-ds-dlg-line">
                <select class="easyui-combobox e-ds-dlg-input" name="enabled" 
                data-options="
                    required:true,
                    label:'${e:i18n('_ADMIN_DASHBOARD_USER_STATUS')}',
                    labelAlign:'right',
                    panelHeight:'auto',
                    panelMaxHeight:'200px',
                    editable:false">
                    <option value="true">${e:i18n('_ADMIN_DASHBOARD_USER_STATUS_ACTIVED')}</option>
                    <option value="false">${e:i18n('_ADMIN_DASHBOARD_USER_STATUS_BLOCKED')}</option>
                </select>
            </div>
        </form>
    </div>    
    <div data-dlg="#e-ds-dlg-add-user" class="e-ds-dlg-btns">
        <a href="javascript:void(0)" class="easyui-linkbutton e-ds-dlg-btn" onclick="userManage.addUser()"><i class="fa fa-save"></i>${e:i18n('_ADMIN_DASHBOARD_DLG_SAVE')}</a>
        <a href="javascript:void(0)" class="easyui-linkbutton e-ds-dlg-btn" onclick="$('#e-ds-dlg-add-user').dialog('close')"><i class="fa fa-close"></i>${e:i18n('_ADMIN_DASHBOARD_DLG_CANCEL')}</a>
    </div>
</div>

<div id="mm" class="easyui-menu" style="">
    <div id="mm-reset-password" onclick="openResetPasswordDlg()">${e:i18n('_ADMIN_DASHBOARD_USER_MENU_RESET_PASSWORD')}</div>
    <div id="mm-authority" 
    onclick="euler.dialog(
                '${__ADMIN_PATH}/authentication/user/userAuthorityManage', 
                'userId='+$('#e-dg').datagrid('getSelections')[0].userId, 
                '${e:i18n('_ADMIN_DASHBOARD_USER_AUTHORITY')}',
                userManage.userAuthorityManageCallback,
                {width:'800px', height:'600px'}
            )">${e:i18n('_ADMIN_DASHBOARD_USER_MENU_USER_AUTHORITY')}</div>
    <div id="mm-active" onclick="userManage.activeUser()">${e:i18n('_ADMIN_DASHBOARD_USER_MENU_ACTIVE_USER')}</div>
    <div id="mm-block" onclick="userManage.blockUser()">${e:i18n('_ADMIN_DASHBOARD_USER_MENU_BLOCK_USER')}</div>
</div>
<div id="dlg-reset-password" class="easyui-dialog e-ds-dlg" title="${e:i18n('_ADMIN_DASHBOARD_USER_RESET_PASSWORD')}" style="width:400px;"
    data-options="
    resizable:true,
    modal:true,
    onClose:onResetPasswordDlgClose,
    closed:true">
    <form id="dlg-reset-password-fm" method="post">
        <input id="reset-password-fm-userId" type="hidden" name="userId">
        <div class="e-ds-dlg-line" id="add-user-fm-password-line">
            <input class="easyui-textbox e-ds-dlg-input" name="newPassword" id="reset-password-fm-new-password"
            data-options="required:true,type:'password',label:'${e:i18n('_ADMIN_DASHBOARD_USER_NEW_PASSWORD')}',labelAlign:'right',labelWidth:'120px'">
        </div>
        <div class="e-ds-dlg-line" id="add-user-fm-password-line">
            <input class="easyui-textbox e-ds-dlg-input" id="reset-password-fm-confirm-password"
            data-options="required:true,type:'password',label:'${e:i18n('_ADMIN_DASHBOARD_USER_CONFIRM_PASSWORD')}',labelAlign:'right',labelWidth:'120px'">
        </div>
    </form>
</div>
<div data-dlg="#dlg-reset-password" class="e-ds-dlg-btns">
    <a href="javascript:void(0)" class="easyui-linkbutton e-ds-dlg-btn" onclick="userManage.resetPassword()"><i class="fa fa-check"></i>${e:i18n('_ADMIN_DASHBOARD_DLG_CONFIRM')}</a>
    <a href="javascript:void(0)" class="easyui-linkbutton e-ds-dlg-btn" onclick="$('#dlg-reset-password').dialog('close')"><i class="fa fa-close"></i>${e:i18n('_ADMIN_DASHBOARD_DLG_CANCEL')}</a>
</div>
<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-global.jsp"%>
<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-dash.jsp"%>

<script>
$(function(){
});

function onRowContextMenu(e, rowIndex, rowData) {
    e.preventDefault();
    //$(".e-ds-dlg").dialog('close');
    $(this).datagrid("clearSelections"); //取消所有选中项
    $(this).datagrid("selectRow", rowIndex); //根据索引选中该行
    $('#mm').menu('show', {
        left: e.pageX,
        top: e.pageY
    });
    if(rowData.enabled === true) {
        $('#mm').menu('disableItem', $('#mm-active'));
        $('#mm').menu('enableItem', $('#mm-block'));
    } else {
        $('#mm').menu('enableItem', $('#mm-active'));
        $('#mm').menu('disableItem', $('#mm-block'));
    }
}

function clearAddUserDlg() {
    $('#e-ds-dlg-add-user-fm').form('clear');
    $('#add-user-fm-password-line').show();  
}

function onAddUserDlgClose() {
    clearAddUserDlg();   
}

function onAdd() {
    $('#e-ds-dlg-add-user').dialog('close');
    $('#e-ds-dlg-add-user').dialog('open');
}

function onClickRow(rowIndex, rowData) {
    $('#e-dg').datagrid('clearSelections');
    $('#e-dg').datagrid('selectRow', rowIndex);
    var userId = getDashboardDataId();
    if(rowData.userId == userId) {
        console.log(rowData.userId + '的编辑区已打开,忽略此次点击');
        return;        
    }
    $('#e-ds-dlg-add-user').dialog('close');
    $('#e-ds-dlg-add-user').dialog('open').dialog('setTitle', "${e:i18n('_ADMIN_DASHBOARD_DLG_EDIT')} - " + rowData.username);
    $('#e-ds-dlg-add-user-fm').form('load', rowData);
    $('#add-user-fm-password-line').hide();
    $('#add-user-fm-password').textbox('setValue', 'password');
}

function openResetPasswordDlg() {
    var row = $('#e-dg').datagrid('getSelections');
    $('#dlg-reset-password').dialog('close');
    $('#dlg-reset-password').dialog('open').dialog('setTitle', "${e:i18n('_ADMIN_DASHBOARD_USER_RESET_PASSWORD')} - " + row[0].username);;
    $('#reset-password-fm-userId').val(row[0].userId);
}

function clearResetPasswordDlg() {
    $('#dlg-reset-password-fm').form('clear');
}

function onResetPasswordDlgClose() {
    clearResetPasswordDlg();
}

function userStatusFormatter(value, row, index){
    if(typeof(value) == 'undefined')
        return '-';
    if(value == true) {
        return "<span style='color:#48D4AE;'>${e:i18n('_ADMIN_DASHBOARD_USER_STATUS_ACTIVED')}</span>";        
    } else {
        return "<span style='color:#D8504D;'>${e:i18n('_ADMIN_DASHBOARD_USER_STATUS_BLOCKED')}</span>";         
    }
}

var userManage = {
    addUser: function() {
        if(!$('#e-ds-dlg-add-user-fm').form('validate'))
            return;
        
        $('#fm-submit-mask').show();
        var data = $('#e-ds-dlg-add-user-fm').serialize();
        
        $.ajax({
            url:'${__ADMIN_PATH}/ajax/authentication/user/saveOrUpdateUser',
            type:'POST',
            async:true,
            data: data,
            error:function(XMLHttpRequest, textStatus, errorThrown) {
                $('#fm-submit-mask').hide();   
                euler.msg.response.error(XMLHttpRequest);
            },
            success:function(data, textStatus) {
                $('#fm-submit-mask').hide();
                onSearch();
            }
        });
    },
    
    resetPassword: function() {
        if(!$('#dlg-reset-password-fm').form('validate'))
            return;
        
        var newPassword = $('#reset-password-fm-new-password').textbox('getValue');
        var confirmPassword = $('#reset-password-fm-confirm-password').textbox('getValue');
        
        if(newPassword != confirmPassword) {
            euler.msg.error("${e:i18n('_ADMIN_DASHBOARD_USER_CONFIRM_PASSWORD_FAIL')}");
            return;
        }
        
        $('#fm-submit-mask').show();
        var data = $('#dlg-reset-password-fm').serialize();
        
        $.ajax({
            url:'${__ADMIN_PATH}/ajax/authentication/user/resetPassword',
            type:'POST',
            async:true,
            data: data,
            error:function(XMLHttpRequest, textStatus, errorThrown) {
                $('#fm-submit-mask').hide();   
                euler.msg.response.error(XMLHttpRequest);
            },
            success:function(data, textStatus) {
                $('#fm-submit-mask').hide();
                onSearch();
            }
        });
    },
    
    activeUser: function() {
        var row = $('#e-dg').datagrid('getSelections');
        
        $('#fm-submit-mask').show();
        
        $.ajax({
            url:'${__ADMIN_PATH}/ajax/authentication/user/activeUser',
            type:'POST',
            async:true,
            data: {userId: row[0].userId},
            error:function(XMLHttpRequest, textStatus, errorThrown) {
                $('#fm-submit-mask').hide();   
                euler.msg.response.error(XMLHttpRequest);
            },
            success:function(data, textStatus) {
                $('#fm-submit-mask').hide();
                onSearch();
            }
        });
    },
    
    blockUser: function() {
        var row = $('#e-dg').datagrid('getSelections');
        
        $('#fm-submit-mask').show();
        
        $.ajax({
            url:'${__ADMIN_PATH}/ajax/authentication/user/blockUser',
            type:'POST',
            async:true,
            data: {userId: row[0].userId},
            error:function(XMLHttpRequest, textStatus, errorThrown) {
                $('#fm-submit-mask').hide();   
                euler.msg.response.error(XMLHttpRequest);
            },
            success:function(data, textStatus) {
                $('#fm-submit-mask').hide();
                onSearch();
            }
        });
    },

    userAuthorityManageCallback: function(data) {
        console.log(data);
        return true;
    }
};
</script>
</body>

</html>