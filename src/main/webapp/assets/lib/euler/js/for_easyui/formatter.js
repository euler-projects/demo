function unixDatetimeFormatter(value, row, index) {
    return new Date(value).Format('yyyy-MM-dd hh:mm:ss');
}

function urlFormatter(value, row, index) {
    if(value == null || value == "")
        return '-';
    return '<a href="'+value+'" target="_Blank">'+value+'</a>';
}
