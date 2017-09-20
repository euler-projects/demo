<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">

<%-- <link href="${__ASSETS_PATH}/lib/thinkCmf/themes/flat/theme.min.css"
    rel="stylesheet">
<link href="${__ASSETS_PATH}/lib/thinkCmf/css/simplebootadmin.css" rel="stylesheet"> --%>
<link href="${__ASSETS_PATH}/lib/thinkCmf/js/artDialog/skins/default.css" rel="stylesheet" />
<%-- <link rel="stylesheet" href="${__ASSETS_PATH}/lib/font-awesome-4.7.0/css/font-awesome.min.css"> --%>
<%@ include file="/WEB-INF/jsp/admin/themes/default/common/css-global.jsp"%>
<%@ include file="/WEB-INF/jsp/admin/themes/default/common/css-dash.jsp"%>
<script>
    //全局变量
    var GV = {
        WEB_ROOT: "${__CONTEXT_PATH}",
        JS_ROOT: "${__ASSETS_PATH}",
        APP:'Euler'/*当前应用名*/
    };
</script>
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
            <select id="e-dg-tb-filter-usertype" class="easyui-combobox e-dg-tb-filter" data-filter-name="type" style="width:120px;" 
                data-options="
                    url:'${__ADMIN_PATH}/ajax/cmf/slide/findAllSlideTypes',
                    method:'get',
                    panelHeight:'auto',
                    panelMaxHeight:'200px',
                    valueField: 'type',
                    textField: 'name'">
            </select>
            <span class="e-dg-tb-label">适用语言</span>
            <select id="e-dg-tb-filter-usertype" class="easyui-combobox e-dg-tb-filter" data-filter-name="locale" style="width:120px;" 
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
                <div data-options="name:'title'">标题</div>
                <div data-options="name:'uri'">链接</div>
                <div data-options="name:'description'">描述</div>
                <div data-options="name:'content'">内容</div>
            </div>
        </div>
    </div>
    <table id="e-dg" class="easyui-datagrid"
        data-options="
            fit:true,
            url:'${__ADMIN_PATH}/ajax/cmf/slide/findSlideByPage',
            toolbar:'#e-dg-tb',
            fitColumns:true,
            rownumbers:true,
            remoteSort:false,
            pagination:true,
            singleSelect:false,
            onClickRow:onClickRow">
        <thead>
            <tr>
                <th data-options="field:'ck',checkbox:true"></th>
                <th data-options="field:'type',align:'center'">类型</th>
                <th data-options="field:'title',align:'center'">标题</th>
                <th data-options="field:'uri',align:'center',formatter:urlFormatter">链接</th>
                <th data-options="field:'locale',align:'center'">语言</th>
                <th data-options="field:'description',align:'center', width:'300px'">描述</th>
                <th data-options="field:'fileId',align:'center',formatter:viewImgFormatter">查看图片</th>
            </tr>
        </thead>
    </table>
</div>
<div id="e-ds-zone" data-options="region:'center'">
    <p style="padding-left:20px;"><a href="javascritp:void(0)" onClick="onAdd()">新建</a>或选择项目以编辑</p> 
    <%-- --%>
    <div id="e-ds-dlg-add-slide" class="easyui-dialog e-ds-dlg" title="${e:i18n('_ADMIN_DASH_DLG_ADD')}"
        data-options="
            closed:true,
            closable:true,
            draggable:false,
            resizable:false,
            modal:false,
            inline:true,
            border:false,
            onClose:onAddSlideDlgClose,
            maximized:true">
        <form id="e-ds-dlg-add-slide-fm" method="post">
            <input id="e-ds-dashInputId" type="hidden" name="id">
            <input id="e-ds-dashInputId" type="hidden" name="order">
            <table class="e-ds-dlg-fm-table">
                <tbody>
                    <tr>
                        <th style="width:50px;">分类</th>
                        <td style="width:285px;">
                            <select class="easyui-combobox e-ds-dlg-input" name="type" style="width: 150px"
                                id="e-ds-dlg-add-slide-fm_slidetype"
                                data-options="
                                    required:true,
                                    panelHeight:'auto',
                                    panelMaxHeight:'200px',
                                    editable:false,
                                    url:'${__ADMIN_PATH}/ajax/cmf/slide/findAllSlideTypes',
                                    method:'get',
                                    valueField: 'type',
                                    textField: 'name'">
                            </select>
                        <th style="width:50px;">语言</th>
                        <td style="width:285px;"><select class="easyui-combobox e-ds-dlg-input" name="locale" style="width: 150px"
                                id="e-ds-dlg-add-slide-fm_locale"
                                data-options="
                                    required:true,
                                    panelHeight:'auto',
                                    panelMaxHeight:'200px',
                                    editable:false,
                                    url:'${__ADMIN_PATH}/ajax/cmf/public/findAllSupportLanguagesKV',
                                    method:'get',
                                    valueField: 'key',
                                    textField: 'value'">
                            </select></td>
                        <td rowspan="5" style="vertical-align: top;">
                            <table class="e-ds-dlg-fm-table">
                                <tbody>
                                    <tr><th>上传图片</th></tr>
                                    <tr>
                                        <td style="text-align:center;padding:6px;border-bottom:0px;">
                                            <input type="hidden" name="fileId" id="thumb" value="">
                                            <a href="javascript:upload_one_image('图片上传', '#thumb')"><img src="${__ASSETS_PATH}/lib/euler-cmf/images/default-thumbnail.png" id="thumb-preview" width="135" style="cursor: hand"></a>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="text-align:center;padding:6px;;border-top:0px;">
                                            <a href="javascript:void(0)" onclick="$('#thumb-preview').attr('src','${__ASSETS_PATH}/lib/euler-cmf/images/default-thumbnail.png');$('#thumb').val('');return false;">删除图片</a>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <th>标题</th>
                        <td colspan="3"><input class="easyui-textbox e-ds-dlg-input" name="title" style="width:600px"
                            data-options="required:true"></td>
                    </tr>
                    <tr>
                        <th>链接</th>
                        <td colspan="3"><input class="easyui-textbox e-ds-dlg-input" name="uri" style="width:600px"
                            data-options=""></td>
                    </tr>
                    <tr>
                        <th>描述</th>
                        <td colspan="3"><input class="easyui-textbox e-ds-dlg-input" name="description" style="width:600px"
                            data-options=""></td>
                    </tr>
                    <tr>
                        <th>内容</th>
                        <td colspan="3"><input class="easyui-textbox e-ds-dlg-input" name="content" style="width:600px;height:100px;"
                            data-options="multiline:true"></td>
                    </tr>
                </tbody>
            </table>
        </form>
    </div>    
    <div data-dlg="#e-ds-dlg-add-slide" class="e-ds-dlg-btns">
        <a href="javascript:void(0)" class="easyui-linkbutton e-ds-dlg-btn" onclick="addSlide()"><i class="fa fa-save"></i>${e:i18n('_ADMIN_DASH_DLG_SAVE')}</a>
        <a href="javascript:void(0)" class="easyui-linkbutton e-ds-dlg-btn" onclick="$('#e-ds-dlg-add-slide').dialog('close')"><i class="fa fa-close"></i>${e:i18n('_ADMIN_DASH_DLG_CANCEL')}</a>
    </div>
    <%-- --%>
</div>


<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-global.jsp"%>
<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-dash.jsp"%>
<script src="${__ASSETS_PATH}/lib/thinkCmf/js/common.js"></script>
<script src="${__ASSETS_PATH}/lib/thinkCmf/js/wind.js"></script>
<script>

function onAdd() {
    $('#e-ds-dlg-add-slide').dialog('close');
    $('#e-ds-dlg-add-slide').dialog('open').dialog('setTitle', "${e:i18n('_ADMIN_DASH_DLG_ADD')}");
}

function addSlide() {
    if(!$('#e-ds-dlg-add-slide-fm').form('validate'))
        return;
    
    $('#fm-submit-mask').show();
    var data = $('#e-ds-dlg-add-slide-fm').serialize();
    
    $.ajax({
        url:'${__ADMIN_PATH}/ajax/cmf/slide/saveSlide',
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

function onAddSlideDlgClose() {
    clearAddSlideDlg();
}

function clearAddSlideDlg() {
    var slidetype = $('#e-ds-dlg-add-slide-fm_slidetype').combobox('getValue');
    $('#e-ds-dlg-add-slide-fm_slidetype').combobox('unselect', slidetype+"");
    
    var locale = $('#e-ds-dlg-add-slide-fm_locale').combobox('getValue');
    $('#e-ds-dlg-add-slide-fm_locale').combobox('unselect', locale+"");
    
    $('#thumb-preview').attr('src','${__ASSETS_PATH}/lib/euler-cmf/images/default-thumbnail.png');$('#thumb').val('');
    
    $('#e-ds-dlg-add-slide-fm').form('clear');    
}

function onClickRow(rowIndex, rowData) {
    $('#e-dg').datagrid('clearSelections');
    $('#e-dg').datagrid('selectRow', rowIndex);
    $('#e-ds-dlg-add-slide').dialog('close');
    var userId = getDashboardDataId();
    if(rowData.id == userId) {
        console.log(rowData.id + '的编辑区已打开,忽略此次点击');
        return;        
    }
    setDashboardDataId(rowData.id); 
    $('#e-ds-dlg-add-slide').dialog('open').dialog('setTitle', "${e:i18n('_ADMIN_DASH_DLG_EDIT')} - " + rowData.title);
    $('#e-ds-dlg-add-slide-fm').form('load', rowData);

    $('#thumb-preview').attr('src','${__FILE_DOWNLOAD_PATH}/'+rowData.fileId);
}

function onDelete() {
    var row = $('#e-dg').datagrid('getSelections');

    if(row == null || row.length < 1){
        euler.msg.error("请选择要删除的图片");
    } else if(row){
        euler.msg.confirm("确定删除选定图片?", function(r) {
            if(r) {
                var formData = new FormData();
                for(var i = 0; i < row.length; i++){
                    formData.append("slideIds", row[i].id);
                }
                $.ajax({
                    url:'${__ADMIN_PATH}/ajax/cmf/slide/deleteSlides',
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