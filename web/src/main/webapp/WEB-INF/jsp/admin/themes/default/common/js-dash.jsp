<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <div id="fm-submit-mask" class="window-mask">
        <div class="mask-wrap" >
            <img class="loading-img" src="${__ASSETS_PATH}/lib/easyui/themes/bootstrap/images/loading.gif">
        </div>
    </div>
    
    <script src="${__ASSETS_PATH}/lib/euler/js/dict.js"></script>
    <script src="${__ASSETS_PATH}/lib/euler/js/util.js"></script>
    <script src="${__ASSETS_PATH}/lib/euler/js/for_easyui/util.js"></script>
    
    <div id="e-iframe-dlg" class="easyui-dialog" style="overflow: hidden"
        data-options="
            closed:true,
            resizable:false,
            modal:true,
            constrain:true,
            onClose:onEulerIframeDlgClose,
            buttons:'#e-iframe-dlg-btns'">
        <iframe id="e-iframe-dlg-content" 
            name="e-iframe-dlg-content" 
            width="100%" height="100%" frameborder="no" border="0" marginwidth="0" marginheight="0" allowtransparency="yes"></iframe>  
        <div id="e-iframe-dlg-btns">
            <a href="javascript:void(0)" class="easyui-linkbutton" onclick="onEulerIframeDlgConfirm()"><i class="fa fa-check"></i>${e:i18n('_ADMIN_DASHBOARD_DLG_CONFIRM')}</a>
            <a href="javascript:void(0)" class="easyui-linkbutton" onclick="$('#e-iframe-dlg').dialog('close')"><i class="fa fa-close"></i>${e:i18n('_ADMIN_DASHBOARD_DLG_CANCEL')}</a>
        </div>
    </div>
    
    <script>
    
    var eulerIframeDlgCallBackFunction;
    
    function onEulerIframeDlgConfirm() {
        window.frames['e-iframe-dlg-content'].onEulerIframeDlgConfirm(function (data){
            eulerIframeDlgCallBackFunction(data);
            $('#e-iframe-dlg-content').attr('src', '');
            $('#e-iframe-dlg').dialog('close');            
        });
    }
    
    function onEulerIframeDlgClose() {
        
    }
    
    </script>
    
    <script> 
    function unixDatetimeFormatter(value, row, index) {
        return new Date(value).Format('yyyy-MM-dd hh:mm:ss');
    }
    
    function yesOrNoFormatter(value, row, index){
        if(typeof(value) == 'undefined')
            return '-';
        if(value === true) {
            return "${e:i18n('_ADMIN_DASHBOARD_YES')}";            
        } else {
            return "${e:i18n('_ADMIN_DASHBOARD_NO')}";            
        }
    }
    
    function yesOrNoWithColorFormatter(value, row, index){
        if(typeof(value) == 'undefined')
            return '-';
        if(value === true) {
            return "<i style='color:#48D4AE;font-style: normal;'>${e:i18n('_ADMIN_DASHBOARD_YES')}</i>";            
        } else {
            return "<i style='color:#D8504D;'>${e:i18n('_ADMIN_DASHBOARD_NO')}</i>";            
        }
    }
    
    function orderEditorFormatter(value, row, index){
        if(typeof(value) == 'undefined')
            return '<input class="order-editor" id="' + row.id + '">';
        return '<input class="order-editor" id="' + row.id + '" value="' + value + '">';
    }
    
    function urlFormatter(value, row, index){
        if(typeof(value) == 'undefined')
            return '-';
        return '<a class="operate-link" href="' + value + '" target="_blank">' + value + '</a>';
    }
    
    function viewImgFormatter(value, row, index){
        if(typeof(value) == 'undefined')
            return '-';
        return '<a class="operate-link" href="${__IMAGE_DOWNLOAD_PATH}/' + value + '" target="_blank">查看图片</a>';
    }
    
    function floatWindowFormatter(value, row, index){
        if(typeof(value) == 'undefined')
            return '-';
        return '<span title="' + value + '">' + value + '</span>';
    }
    
    var euler = {
            table: {
                loadData: function(table, data) {
                    var td = $(table).find('.data-td');

                    for(var i=0;i<td.length;i++){
                        var field = $(td[i]).data("field");
                        
                        if(typeof(field) == 'undefined' || field == '')
                            continue;
                        
                        var r =data[field];
                        var value = r;
                        
                        var formatter = $(td[i]).data("formatter");
                        if(typeof(formatter) != 'undefined' && formatter != '') {
                            var func = eval(formatter);
                            r =func(r, data);
                        }

                        if($(td[i]).hasClass('editable')) {
                            if(typeof(r) == 'undefined')
                                r = '';
                            $($(td[i]).children('.td-input')[0]).val(r);                      
                        } else {
                            if(typeof(r) == 'undefined')
                                r = '-';                        
                            td[i].innerHTML = r+'<input type="hidden" name="'+field+'" value="'+value+'">';                        
                        }                    
                    }
                }
            },
            
            msg: {
                confirm: function(msg, callback) {
                    $.messager.confirm("${e:i18n('_ADMIN_ALERT')}", msg, callback);
                },
                alert: function(msg) {
                    $.messager.alert("${e:i18n('_ADMIN_ALERT')}", msg);
                },
                error: function(msg) {
                    $.messager.alert("${e:i18n('_ADMIN_ERROR')}", "<div style='color: #D8504D;font-size: 1.5em;margin-bottom: 5px;'>ERROR</div><div style='margin-bottom: 5px;'>" + msg + "</div>");
                },
                response: {
                    error: function(XMLHttpRequest) {
                        var response = JSON.parse(XMLHttpRequest.responseText);
                        var msg = "<div style='color: #D8504D;font-size: 1.5em;margin-bottom: 5px;'>ERROR</div><div style='margin-bottom: 5px;'>${e:i18n('_ADMIN_ERROR_CODE')}:&nbsp;" + response.error + ' (' + response.error_code + ')'  + "</div><div>${e:i18n('_ADMIN_ERROR_DETAILS')}:&nbsp;" + response.error_description + "</div>";
                        $.messager.alert("${e:i18n('_ADMIN_ERROR')}", msg);
                    }
                }
            },
            
            dialog: function(url, params, title, callback, options) {
                eulerIframeDlgCallBackFunction = callback;
                if(typeof(options) == 'object') {
                    $('#e-iframe-dlg').dialog(options);
                }
                $('#e-iframe-dlg').dialog('open').dialog('setTitle', title);
                $('#e-iframe-dlg').window('center');
                $('#e-iframe-dlg-content').attr('src', url + '?' + params);
            }
    }
    
    </script>