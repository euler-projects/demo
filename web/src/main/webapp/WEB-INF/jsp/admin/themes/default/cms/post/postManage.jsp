<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">

<link href="${__ASSETS_PATH}/lib/thinkCmf/js/artDialog/skins/default.css" rel="stylesheet" />
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
<div id="e-dg-tb">
    <div class="btns">
        <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" onclick="onSearch()"><i class="fa fa-refresh"></i>${e:i18n('_ADMIN_DASHBOARD_REFRESHE')}</a>
        <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" onclick="onAdd()"><i class="fa fa-plus"></i>${e:i18n('_ADMIN_DASHBOARD_ADD')}</a>
        <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" onclick="onDelete()"><i class="fa fa-remove"></i>${e:i18n('_ADMIN_DASHBOARD_DEL')}</a>
        <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" onclick="onSort()"><i class="fa fa-sort"></i>${e:i18n('_ADMIN_DASHBOARD_SORT')}</a>
    </div>
    <div class="filter">
        <span class="e-dg-tb-label">内容类型</span>
        <select id="e-dg-tb-filter-usertype" class="easyui-combobox e-dg-tb-filter" data-filter-name="type" style="width:80px;" 
            data-options="
                url:'${__ADMIN_PATH}/ajax/cms/post/findAllPostTypes',
                method:'get',
                panelHeight:'auto',
                panelMaxHeight:'200px',
                panelWidth:'auto',
                panelMaxWidth:'200px',
                valueField: 'type',
                textField: 'name'">
        </select>
        <span class="e-dg-tb-label">适用语言</span>
        <select id="e-dg-tb-filter-usertype" class="easyui-combobox e-dg-tb-filter" data-filter-name="locale" style="width:70px;" 
            data-options="
                url:'${__ADMIN_PATH}/ajax/cms/public/findAllSupportLanguagesKV?addAll=true',
                method:'get',
                panelHeight:'auto',
                panelMaxHeight:'200px',
                valueField: 'key',
                textField: 'value'">
        </select>
        <span class="e-dg-tb-label">是否置顶</span>
        <select id="e-dg-tb-filter-userstatus" class="easyui-combobox e-dg-tb-filter" data-filter-name="top" style="width:60px;" data-options="panelHeight:'auto',panelMaxHeight:'200px'">
            <option value="">全部</option>
            <option value="true">${e:i18n('_ADMIN_DASHBOARD_YES')}</option>
            <option value="false">${e:i18n('_ADMIN_DASHBOARD_NO')}</option>
        </select>
        <span class="e-dg-tb-label">是否已审核</span>
        <select id="e-dg-tb-filter-userstatus" class="easyui-combobox e-dg-tb-filter" data-filter-name="approved" style="width:60px;" data-options="panelHeight:'auto',panelMaxHeight:'200px'">
            <option value="">全部</option>
            <option value="true">${e:i18n('_ADMIN_DASHBOARD_YES')}</option>
            <option value="false">${e:i18n('_ADMIN_DASHBOARD_NO')}</option>
        </select>
    </div>
    <div class="searcher">
        <input id="e-dg-tb-searcher" class="easyui-searchbox" style="width:280px"
        data-options="searcher:onSearch ,prompt:'搜索',menu:'#ss-menu'"></input>
        <div id="ss-menu" style="width:30px">
            <div data-options="name:'_all'">全部</div>
            <div data-options="name:'title'">标题</div>
            <div data-options="name:'excerpt'">摘要</div>
        </div>
    </div>
</div>
<table id="e-dg" class="easyui-datagrid"
    data-options="
        fit:true,
        url:'${__ADMIN_PATH}/ajax/cms/post/findPostByPage',
        toolbar:'#e-dg-tb',
        fitColumns:true,
        rownumbers:true,
        remoteSort:false,
        pagination:true,
        singleSelect:false,
        pageSize:30,
        pageList:[10,20,30,40,50,100,200],
        onDblClickRow:onDblClickRow">
    <thead>
        <tr>
            <th data-options="field:'ck',checkbox:true"></th>
            <th data-options="field:'order',align:'center',formatter:orderEditorFormatter">顺序</th>
            <th data-options="field:'type',align:'center'">类型</th>
            <th data-options="field:'title',width:'300px',fixed:true,align:'left',formatter:postTitleFormatter">标题</th>
            <th data-options="field:'excerpt',align:'left', width:'200px',formatter:floatWindowFormatter">摘要</th>
            <th data-options="field:'authorUsername',align:'center',width:'60px',fixed:true">作者</th>
            <th data-options="field:'locale',align:'center',width:'60px',fixed:true">语言</th>
            <th data-options="field:'updateDate',align:'center',width:'120px',fixed:true,formatter:unixDatetimeFormatter">发布时间</th>
            <th data-options="field:'top',align:'center',width:'40px',fixed:true,formatter:yesOrNoFormatter">置顶</th>
            <th data-options="field:'approved',align:'center',width:'40px',fixed:true,formatter:yesOrNoWithColorFormatter">审核</th>
            <th data-options="field:'themePictureArchiedFileId',align:'center',width:'160px',fixed:true,formatter:postOperFormatter">操作</th>
        </tr>
    </thead>
</table>


<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-global.jsp"%>
<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-dash.jsp"%>
<script>
function onAdd() {
    euler.dialog("addPost", "", "新建文章", function(data){
        console.log(data);
        onSearch();        
    }, {
        border: false,
        maximized: true
    });
}
function onDblClickRow(rowIndex, rowData) {
    $('#e-dg').datagrid('clearSelections');
    $('#e-dg').datagrid('selectRow', rowIndex);
    editPost(rowData.id, rowData.type);
}

function editPost(postId, postType) {
    euler.dialog("editPost", "id=" + postId + "&type=" + postType, "编辑文章", function(data){
        console.log(data);
        onSearch();        
    }, {
        border: false,
        maximized: true
    });    
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
                    formData.append("postIds", row[i].id);
                }
                $.ajax({
                    url:'${__ADMIN_PATH}/ajax/cms/post/deletePosts',
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

function onSort() {
    var orderEditor = $('.order-editor');
    if(typeof(orderEditor) == 'undefined' || orderEditor.length <= 0) {
        return;
    }
    var formData = new FormData();
    for(var i = 0; i < orderEditor.length; i++) {
        var order = orderEditor[i];
        formData.append("postIds", order.id);
        formData.append("postOrders", order.value);
    }
    $.ajax({
        url:'${__ADMIN_PATH}/ajax/cms/post/sortPosts',
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

function postTitleFormatter(value, row, index) {
    var html = '<span title="' + value + '"><a class="operate-link" href="${__CONTEXT_PATH}/cms/post/' + row.id + '" target="_blank">' + value + '</a></span>';
    return html;
}

function postOperFormatter(value, row, index) {
    var html = '<a class="operate-link" href="javascript:editPost(\'' + row.id + '\', \'' + row.type + '\')">编辑</a>';
    
    if(row.top === true) {
        html += '&nbsp;&nbsp;<a class="operate-link" href="javascript:postManage.untop(\'' + row.id + '\')">取消置顶</a>';
    } else {
        html += '&nbsp;&nbsp;<a class="operate-link" href="javascript:postManage.top(\'' + row.id + '\')">置顶</a>';        
    }
    
    if(row.approved === true) {
        html += '&nbsp;&nbsp;<a class="operate-link" href="javascript:postManage.unapprove(\'' + row.id + '\')">取消审核</a>'; 
    } else {       
        html += '&nbsp;&nbsp;<a class="operate-link" href="javascript:postManage.approve(\'' + row.id + '\')">审核通过</a>';
    }
    
    return html;
}

var postManage = {
        top: function(postId){
            var formData = new FormData();
            formData.append("postIds", postId);
            $.ajax({
                url:'${__ADMIN_PATH}/ajax/cms/post/topPosts',
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
        },
        untop: function (postId) {
            var formData = new FormData();
            formData.append("postIds", postId);
            $.ajax({
                url:'${__ADMIN_PATH}/ajax/cms/post/untopPosts',
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
            
        },
        approve: function(postId) {
            var formData = new FormData();
            formData.append("postIds", postId);
            $.ajax({
                url:'${__ADMIN_PATH}/ajax/cms/post/approvePosts',
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
            
        },
        unapprove: function(postId) {
            var formData = new FormData();
            formData.append("postIds", postId);
            $.ajax({
                url:'${__ADMIN_PATH}/ajax/cms/post/unapprovePosts',
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
}




</script>
</body>
</html>