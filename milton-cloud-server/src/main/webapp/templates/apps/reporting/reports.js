$(function() {
    $('.dateRange input').daterangepicker({
        dateFormat : "dd/mm/yy",
        onChange : function() {
            runReport();
        }
    });         
    $('#reportRange').change(function() {
        log("change");
        runReport();
    });     
    runReport();
});
function runReport() {
    var range = $("#reportRange").val();
    var arr = range.split("-");
    log("range", range, arr)
    var start = "";
    if( arr.length > 0 ) {
        start = arr[0];
    }
    var finish = "";
    if( arr.length > 1) {
        finish = arr[1];
    }
    $.ajax({
        type: 'GET',
        url: window.location.pathname,
        dataType: "json",
        data: {
            startDate: start,
            finishDate : finish
        },
        success: function(resp) {
            log("response", resp.data);
            showGraph(resp.data);
                
        }
    });                
}
function showGraph(data) {
    $("#annual").html("");
    if( data ) {
        Morris.Line({
            element: 'annual',
            data: data,
            xkey: 'date',
            ykeys: ['value'],
            labels: ['Series A']
        });
    }
}
