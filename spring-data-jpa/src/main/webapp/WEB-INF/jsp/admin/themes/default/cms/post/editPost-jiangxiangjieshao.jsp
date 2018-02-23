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
<body>
<form id="e-ds-dlg-edit-post-fm" method="post">
<input id="e-ds-dashInputId" type="hidden" name="id" value="${post.id}">
<input id="e-ds-dashInputId" type="hidden" name="order" value="${post.order}">
<input id="e-ds-dashInputId" type="hidden" name="top" value="${post.top}">
<input id="e-ds-dashInputId" type="hidden" name="approved" value="${post.approved}">
<table class="e-ds-dlg-fm-table">
<tbody>
<tr>
    <th style="width:50px;">分类</th>
    <td style="width:320px;">
        <input id="e-ds-dashInputId" type="hidden" name="type" value="${post.type}">${post.type}</td>
    <th style="width:50px;">语言</th>
    <td style="width:320px;"><select class="easyui-combobox e-ds-dlg-input" name="locale" style="width: 150px"
            id="e-ds-dlg-add-slide-fm_locale"
            data-options="
                value: '${post.locale}',
                required:true,
                panelHeight:'auto',
                panelMaxHeight:'200px',
                editable:false,
                url:'${__ADMIN_PATH}/ajax/cms/public/findAllSupportLanguagesKV',
                method:'get',
                valueField: 'key',
                textField: 'value'">
        </select></td>
    <td rowspan="4" style="vertical-align: top;">
        <table class="e-ds-dlg-fm-table e-ds-dlg-fm-slide-table">
            <tbody>
                <tr><th>上传图片</th></tr>
                <tr>
                    <td style="border-bottom:0px;">
                        <input type="hidden" name="themePictureArchiedFileId" id="thumb" value="${post.themePictureArchiedFileId}">
                        <c:if test="${post.themePictureArchiedFileId == null}">
                            <a href="javascript:upload_one_image('图片上传', '#thumb')"><img src="${__ASSETS_PATH}/lib/euler-cmf/images/default-thumbnail.png" id="thumb-preview" width="135" style="cursor: hand"></a>
                        </c:if>
                        <c:if test="${post.themePictureArchiedFileId != null}">
                            <a href="javascript:upload_one_image('图片上传', '#thumb')"><img src="${__IMAGE_DOWNLOAD_PATH}/${post.themePictureArchiedFileId}" id="thumb-preview" width="135" style="cursor: hand"></a>
                        </c:if>
                    </td>
                </tr>
                <tr>
                    <td style="border-top:0px;">
                        <a href="javascript:void(0)" onclick="$('#thumb-preview').attr('src','${__ASSETS_PATH}/lib/euler-cmf/images/default-thumbnail.png');$('#thumb').val('');return false;">删除图片</a>
                    </td>
                </tr>
                <tr><th>发布时间</th></tr>
                <tr><td class="align-center">
                    <input class="easyui-datetimebox e-ds-dlg-input" name="updateDate" style="width:200px"
                            data-options="required:true,editable:false"
                            value='<fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss" value="${post.updateDate}"/>' >
                </td></tr>
            </tbody>
        </table>
    </td>
</tr>
<tr><th>作者/团队名称</th><td colspan="3">
<input class="easyui-textbox e-ds-dlg-input" name="title" style="width:700px"
                            data-options="required:true" value="${post.title}">
</td></tr>
<tr><th>摘要</th><td colspan="3">
<input class="easyui-textbox e-ds-dlg-input" name="excerpt" style="width:700px;height:90px"
                            data-options="required:true,multiline:true" value="${post.excerpt}"></td></tr>
<tr><th>正文</th><td colspan="3">
<script style="height:300px" type="text/plain" id="content" name="content">${post.content}</script>
</td></tr>
</tbody>
</table>
</form>



<script type="text/javascript">
    //编辑器路径定义
    var editorURL = GV.WEB_ROOT;
</script>
<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-global.jsp"%>
<%@ include file="/WEB-INF/jsp/admin/themes/default/common/js-dash.jsp"%>
<script src="${__ASSETS_PATH}/lib/thinkCmf/js/common.js"></script>
<script src="${__ASSETS_PATH}/lib/thinkCmf/js/wind.js"></script>
<script type="text/javascript" src="${__ASSETS_PATH}/lib/thinkCmf/js/ueditor/ueditor.config.js"></script>
<script type="text/javascript" src="${__ASSETS_PATH}/lib/thinkCmf/js/ueditor/ueditor.all.min.js"></script>

<script>
$(function() {
    //编辑器
    editorcontent = new baidu.editor.ui.Editor();
    editorcontent.render('content');   
    try {
        editorcontent.sync();
    } catch (err) {
    }
});

function onEulerIframeDlgConfirm(callback) {
    if(!$('#e-ds-dlg-edit-post-fm').form('validate'))
        return;
    
    var data = $('#e-ds-dlg-edit-post-fm').serializeJson();
    
    data.updateDate = new Date(data.updateDate).getTime();
    
    $.ajax({
        url:'${__ADMIN_PATH}/ajax/cms/post/savePost',
        type:'POST',
        async:true,
        data: data,
        error:function(XMLHttpRequest, textStatus, errorThrown) {
            $('#fm-submit-mask').hide();   
            euler.msg.response.error(XMLHttpRequest);
        },
        success:function(data, textStatus) {
            $('#fm-submit-mask').hide();
            callback(data);
        }
    });
}
</script>
</body>
</html>