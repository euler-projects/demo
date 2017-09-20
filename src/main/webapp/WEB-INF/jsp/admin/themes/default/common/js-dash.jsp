   
    <div id="fm-submit-mask" class="window-mask">
        <div class="mask-wrap" >
            <img class="loading-img" src="${__ASSETS_PATH}/lib/easyui/themes/bootstrap/images/loading.gif">
        </div>
    </div>
    
    <script src="${__ASSETS_PATH}/lib/euler/js/dict.js"></script>
    <script src="${__ASSETS_PATH}/lib/euler/js/util.js"></script>
    <script src="${__ASSETS_PATH}/lib/euler/js/for_easyui/util.js"></script>
    <script src="${__ASSETS_PATH}/lib/euler/js/for_easyui/formatter.js"></script>
    
    <div id="e-iframe-dlg" class="easyui-dialog" style="width:90%; max-width:800px; height:400px"
        data-options="
            closed:true,
            iconCls:'icon-search',
            resizable:false,
            modal:true,
            constrain:true,
            onClose:onEulerIframeDlgClose,
            buttons:'#e-iframe-dlg-btns'">
        <iframe id="e-iframe-dlg-content" 
            name="e-iframe-dlg-content" 
            width="100%" height="100%" frameborder="no" border="0" marginwidth="0" marginheight="0" allowtransparency="yes"></iframe>  
        <div id="e-iframe-dlg-btns">
            <a href="javascript:void(0)" class="easyui-linkbutton" onclick="onEulerIframeDlgConfirm()"><i class="fa fa-check"></i>${e:i18n('_ADMIN_DASH_DLG_OK')}</a>
            <a href="javascript:void(0)" class="easyui-linkbutton" onclick="$('#e-iframe-dlg').dialog('close')"><i class="fa fa-close"></i>${e:i18n('_ADMIN_DASH_DLG_CANCEL')}</a>
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

    function imgFormatter(value, row, index) {
        return '<img alt="No Image" style="margin-top:5px;" src="${__CONTEXT_PATH}/upload/'+value+'"  width="200px">'
    }
    

    function yesOrNoFormatter(value, row, index){
        if(typeof(value) == 'undefined')
            return '-';
        if(value === true) {
            return "${e:i18n('_ADMIN_DASH_YES')}";            
        } else {
            return "${e:i18n('_ADMIN_DASH_NO')}";            
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
        return '<a class="operate-link" href="' + value + '" target="_blank">' + value + '</a>'
    }
    
    function viewImgFormatter(value, row, index){
        if(typeof(value) == 'undefined')
            return '-';
        return '<a class="operate-link" href="${__IMAGE_DOWNLOAD_PATH}/' + value + '" target="_blank">查看图片</a>'
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
            
            dialog: function(url, params, title, callback) {
                eulerIframeDlgCallBackFunction = callback;
                $('#e-iframe-dlg').dialog('open').dialog('setTitle', title);
                $('#e-iframe-dlg-content').attr('src', url + '?' + params);
            }
    }
    
    </script>