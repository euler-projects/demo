<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">

<%@ include file="/WEB-INF/jsp/admin/themes/default/common/css-global.jsp"%>
<%@ include file="/WEB-INF/jsp/admin/themes/default/common/css-dash.jsp"%>
<title>${__ADMIN_DASHBOARD_BRAND_TEXT}</title>
</head>
<body class="easyui-layout">
<div id="e-dg-zone" data-options="region:'north',split:true" style="background:#eee;">
    <div id="e-dg-tb">
        <div class="btns">
            <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" onclick="onSearch()"><i class="fa fa-refresh"></i>刷新</a>
            <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" onclick="onAdd()"><i class="fa fa-plus"></i>新建</a>
            <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" onclick="onDelete()"><i class="fa fa-remove"></i>删除</a>
            <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" onclick="onSort()"><i class="fa fa-sort"></i>排序</a>
        </div>
        <div class="filter">
            <span class="e-dg-tb-label">图片类型</span>
            <select id="e-dg-tb-filter-usertype" class="easyui-combobox e-dg-tb-filter" data-filter-name="groupId" style="width:120px;" 
                data-options="
                    url:'${__ADMIN_PATH}/ajax/cmf/slide/findAllSlideTypes',
                    method:'get',
                    panelHeight:'auto',
                    panelMaxHeight:'200px',
                    valueField: 'type',
                    textField: 'name'">
            </select>
            <span class="e-dg-tb-label">适用语言</span>
            <select id="e-dg-tb-filter-usertype" class="easyui-combobox e-dg-tb-filter" data-filter-name="groupId" style="width:120px;" 
                data-options="
                    url:'${__ADMIN_PATH}/ajax/cmf/public/findAllSupportLanguagesKV?addAll=true',
                    method:'get',
                    panelHeight:'auto',
                    panelMaxHeight:'200px',
                    valueField: 'key',
                    textField: 'value'">
            </select>
        </div>
        <div class="searcher">
            <input id="e-dg-tb-searcher" class="easyui-searchbox" style="width:280px"
            data-options="searcher:onSearch ,prompt:'搜索',menu:'#ss-menu'"></input>
            <div id="ss-menu" style="width:30px">
                <div data-options="name:'_all'">全部</div>
                <div data-options="name:'type'">类型标识</div>
                <div data-options="name:'name'">类型名称</div>
                <div data-options="name:'description'">类型描述</div>
            </div>
        </div>
    </div>
    <table id="e-dg" class="easyui-datagrid"
        data-options="
            fit:true,
            url:'${__ADMIN_PATH}/ajax/cmf/slide/findSlideTypeByPage',
            toolbar:'#e-dg-tb',
            fitColumns:true,
            rownumbers:true,
            remoteSort:false,
            pagination:true,
            singleSelect:true,
            onClickRow:onClickRow">
        <thead>
            <tr>
                <th data-options="field:'type',align:'center'">类型标识</th>
                <th data-options="field:'name',align:'center'">类型名称</th>
                <th data-options="field:'description',align:'center', width:'600px'">类型描述</th>
                <th data-options="field:'enabled',align:'center',formatter:yesOrNoFormatter">已生效</th>
            </tr>
        </thead>
    </table>
</div>
<div id="e-ds-zone" data-options="region:'center'">
    <p style="padding-left:20px;"><a href="javascritp:void(0)" onClick="onAdd()">新建</a>或选择项目以编辑</p> 
    <%-- --%>
    <div id="e-ds-dlg-add-slide-type" class="easyui-dialog e-ds-dlg" title="${e:i18n('_ADMIN_DASH_DLG_ADD')}"
        data-options="
            closed:true,
            closable:true,
            draggable:false,
            resizable:false,
            modal:false,
            inline:true,
            border:false,
            onClose:onAddSlideTypeDlgClose,
            maximized:true">
        <form id="e-ds-dlg-add-slide-type-fm" method="post">
            <input id="e-ds-dashInputId" type="hidden">
            <table class="e-ds-dlg-fm-table">
                <tbody>
                    <tr>
                        <th style="width:50px;">分类</th>
                        <td style="width:620px;"><select class="easyui-combobox e-ds-dlg-input" name="enabled" id="e-ds-dlg-add-slide-type-fm_enabled"
                            data-options="
                                required:true,
                                panelHeight:'auto',
                                panelMaxHeight:'200px',
                                editable:false,
                                url:'${__ADMIN_PATH}/ajax/cmf/slide/findAllSlideTypes',
                                method:'get',
                                valueField: 'type',
                                textField: 'name'">
                            </select></td>
                        <td rowspan="5" style="vertical-align: top;">
                            <table class="e-ds-dlg-fm-table">
                                <tbody>
                                    <tr><th>上传图片</th></tr>
                                    <tr>
                                        <td style="text-align:center;padding:6px;border-bottom:0px;">
                                            <a href="javascript:void(0)"><img src="${__ASSETS_PATH}/lib/euler-cmf/images/default-thumbnail.png" id="thumb-preview" width="135" style="cursor: hand"></a>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="text-align:center;padding:6px;;border-top:0px;">
                                            <a href="javascript:void(0)">删除图片</a>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <th>标题</th>
                        <td><input class="easyui-textbox e-ds-dlg-input" name="type" style="width:600px"
                            data-options="required:true"></td>
                    </tr>
                    <tr>
                        <th>链接</th>
                        <td><input class="easyui-textbox e-ds-dlg-input" name="name" style="width:600px"
                            data-options="required:true"></td>
                    </tr>
                    <tr>
                        <th>描述</th>
                        <td><input class="easyui-textbox e-ds-dlg-input" name="name" style="width:600px"
                            data-options="required:true"></td>
                    </tr>
                    <tr>
                        <th>内容</th>
                        <td><input class="easyui-textbox e-ds-dlg-input" name="name" style="width:600px;height:200px;"
                            data-options="required:true,multiline:true"></td>
                    </tr>
                </tbody>
            </table>
            <%-- <input id="e-ds-dashInputId" type="hidden">
            <div class="e-ds-dlg-line">
                <select class="easyui-combobox e-ds-dlg-input" name="enabled" id="e-ds-dlg-add-slide-type-fm_enabled"
                data-options="
                    required:true,
                    label:'分类',
                    labelAlign:'right',
                    panelHeight:'auto',
                    panelMaxHeight:'200px',
                    editable:false,
                    url:'${__ADMIN_PATH}/ajax/cmf/slide/findAllSlideTypes',
                    method:'get',
                    valueField: 'type',
                    textField: 'name'">
                </select>
            </div>
            <div class="e-ds-dlg-line">
                <input class="easyui-textbox e-ds-dlg-input" name="type" style="width:600px"
                data-options="required:true,label:'标题',labelAlign:'right'">
            </div>
            <div class="e-ds-dlg-line">
                <input class="easyui-textbox e-ds-dlg-input" name="name" style="width:600px"
                data-options="required:true,label:'链接',labelAlign:'right'">
            </div>
            <div class="e-ds-dlg-line">
                <input class="easyui-textbox e-ds-dlg-input" name="description" style="width:600px"
                data-options="label:'描述',labelAlign:'right'">
            </div>
            <div class="e-ds-dlg-line">
                <input class="easyui-textbox e-ds-dlg-input" name="description" style="width:600px;height:240px"
                data-options="multiline:true,label:'内容',labelAlign:'right'">
            </div>
            <div class="e-ds-dlg-line">
                <select class="easyui-combobox e-ds-dlg-input" name="enabled" id="e-ds-dlg-add-slide-type-fm_enabled"
                data-options="
                    required:true,
                    label:'是否生效',
                    labelAlign:'right',
                    panelHeight:'auto',
                    panelMaxHeight:'200px',
                    editable:false">
                    <option value="true">是</option>
                    <option value="false">否</option>
                </select>
            </div> --%>
        </form>
    </div>    
    <div data-dlg="#e-ds-dlg-add-slide-type" class="e-ds-dlg-btns">
        <a href="javascript:void(0)" class="easyui-linkbutton e-ds-dlg-btn" onclick="addSlideType()"><i class="fa fa-save"></i>${e:i18n('_ADMIN_DASH_DLG_SAVE')}</a>
        <a href="javascript:void(0)" class="easyui-linkbutton e-ds-dlg-btn" onclick="$('#e-ds-dlg-add-slide-type').dialog('close')"><i class="fa fa-close"></i>${e:i18n('_ADMIN_DASH_DLG_CANCEL')}</a>
    </div>
    <%-- --%>
</div>


<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-global.jsp"%>
<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-dash.jsp"%>
<script>

function onAdd() {
    //$('#e-ds-dlg-add-slide-type').dialog('close');
    $('#e-ds-dlg-add-slide-type').dialog('open').dialog('setTitle', "${e:i18n('_ADMIN_DASH_DLG_ADD')}");
}

function addSlideType() {
    if(!$('#e-ds-dlg-add-slide-type-fm').form('validate'))
        return;
    
    $('#fm-submit-mask').show();
    var data = $('#e-ds-dlg-add-slide-type-fm').serialize();
    
    $.ajax({
        url:'${__ADMIN_PATH}/ajax/cmf/slide/saveSlideType',
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
}

function onAddSlideTypeDlgClose() {
    clearAddSlideTypeDlg();
}

function clearAddSlideTypeDlg() {
    var enabled = $('#e-ds-dlg-add-slide-type-fm_enabled').combobox('getValue');
    $('#e-ds-dlg-add-slide-type-fm_enabled').combobox('unselect', enabled+"");
    $('#e-ds-dlg-add-slide-type-fm').form('clear');    
}

function onClickRow(rowIndex, rowData) {
    $('#e-ds-dlg-add-slide-type').dialog('close');
    var userId = getDashboardDataId();
    if(rowData.id == userId) {
        console.log(rowData.id + '的编辑区已打开,忽略此次点击');
        return;        
    }
    setDashboardDataId(rowData.id); 
    $('#e-ds-dlg-add-slide-type').dialog('open').dialog('setTitle', "${e:i18n('_ADMIN_DASH_DLG_EDIT')} - " + rowData.name);
    $('#e-ds-dlg-add-slide-type-fm').form('load', rowData);
    
    $('#e-ds-dlg-add-slide-type-fm_enabled').combobox('clear');           
    $('#e-ds-dlg-add-slide-type-fm_enabled').combobox('select', rowData.enabled+"");
}

function onDelete() {
    var row = $('#e-dg').datagrid('getSelections');

    if(row == null || row.length < 1){
        euler.msg.error("请选择要删除的图片类型");
    } else if(row){
        euler.msg.confirm("确定删除选定图片类型?", function(r) {
            if(r) {
                var formData = new FormData();
                for(var i = 0; i < row.length; i++){
                    formData.append("slideTypes", row[i].id);
                }
                $.ajax({
                    url:'${__ADMIN_PATH}/ajax/cmf/slide/deleteSlideTypes',
                    type:'POST',
                    async:true,
                    contentType: false,
                    cache: false,
                    processData: false,
                    data: formData,
                    error:function(XMLHttpRequest, textStatus, errorThrown) {
                        euler.msg.response.error(XMLHttpRequest);
                    },
                    success:function(data, textStatus) {
                        onSearch();
                    }
                });
            }
        });
    }  
}

</script>
</body>
</html>