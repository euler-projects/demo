$(function(){
//    $(".search-table input,.search-table select").on('keyup',function(event){
//        if(event.keyCode == "13"){
//            $(".search-btn-table #search-btn").click();
//        }
//    });
    
    initDgTb();
    initDashZone();
    
    function initDgTb() {
        $('.e-dg-tb-filter').combobox ({onChange: onSearch});
        console.log("DG-TB init completed");
    }
    
    function initDashZone() {
        var dlgBtnDivs = $('.e-ds-dlg-btns');
        for(var i=0; i<dlgBtnDivs.length; i++) {
            var dlgBtnDiv = dlgBtnDivs[i];
            dlgBtnDiv.id = "e-ds-dlg-btns" + i;
            $($(dlgBtnDiv).data('dlg')).dialog({buttons:'#'+dlgBtnDiv.id});
        }        
        console.log("DS-DLG init completed");
    }
});

function onSearch() {
    if($('#e-dg-tb-searcher').length != 0) {
        var name = $('#e-dg-tb-searcher').searchbox('getName');
        var value = $('#e-dg-tb-searcher').searchbox('getValue');
        
        var data = {};
        if(name == '_all') {        
            var options = $('#e-dg-tb-searcher').searchbox('menu').children("div.menu-item");
            
            for(var i=0; i<options.length; i++) {
                var dataOptions = $(options[i]).data('options');        
                var property = dataOptions.substring(6, dataOptions.length - 1);
                var mode = $(options[i]).data('mode');    
    
                if(property == '_all')
                    continue;
                
                data['query.' + property] = value;
                data['mode.' + property] = typeof(mode) == 'undefined' ? 'anywhere' : mode;
            }        
        } else {
            data['query.' + name] = value;
            data['mode.' + name] = 'anywhere';
        }

        data['useOr']=true;
    }
    
    var filterList = $('#e-dg-tb').find('.e-dg-tb-filter');
    
    for(var i=0; i< filterList.length; i++) {
        var filter = $(filterList[i]);
        var name = filter.data("filter-name");
        data['filter.' + name] = filter.combobox('getValue');
    }
    
    $('#e-dg').datagrid('reload', data);
    
    $(".e-ds-dlg").dialog('close');
}

function getDashboardDataId() {
    return $('#e-ds-dashInputId').val();
}

function setDashboardDataId(value) {
    $('#e-ds-dashInputId').val(value);
}
