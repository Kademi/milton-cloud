$(function() {
    var reportContainer = $("#annual");
    var reportRange = $("#reportRange");
    var rangeInputs = $('.dateRange input');
    if( rangeInputs.length > 0 ) {
        rangeInputs.daterangepicker({
            dateFormat : "dd/mm/yy",
            onChange : function() {
                runReport(reportRange.val(), reportContainer, window.location.pathname);
            }
        });         
        $('#reportRange').change(function() {
            log("change");
            runReport(reportRange.val(), reportContainer, window.location.pathname);
        });     
        runReport(reportRange.val(), reportContainer, window.location.pathname);
    }
});
function runReport(range, reportContainer, href) {
    var arr = range.split("-");
    log("range", range, arr)
    var data = {};
    if( arr.length > 0 ) {
        data.startDate = arr[0];
    }
    if( arr.length > 1) {
        data.finishDate = arr[1];
    }
    $.ajax({
        type: 'GET',
        url: href,
        dataType: "json",
        data: data,
        success: function(resp) {
            log("response", resp.data);
            showGraph(resp.data, reportContainer);
                
        }
    });                
}
function showGraph(graphData, reportContainer) {
    log("showGraph",reportContainer, graphData);    
    if( graphData ) {
        reportContainer.removeClass("nodata");
        reportContainer.html("");
        if( graphData.data.length > 0 ) {
            Morris.Line({
                element: reportContainer,
                data: graphData.data,
                xkey: graphData.xkey,
                ykeys: graphData.ykeys,
                labels: graphData.labels,
                hideHover: true,
                dateFormat: function (x) {
                    var dt = new Date(x).formatDDMMYYYY();
                    //var dt = new Date(x).toString();
                    //log("formatted date", x, dt, new Date(x).formatDDMMYYYY());
                    return dt;
                } // see common.js
            });
        } else {
            reportContainer.addClass("nodata");
            reportContainer.html("<p class='nodata'>No data</p>");
        }
    }
}
