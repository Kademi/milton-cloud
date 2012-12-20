$(function() {
    var reportContainer = $("#annual");
    var itemsContainer = $("#items");
    var reportRange = $("#reportRange");
    var rangeInputs = $('.dateRange input');
    if( rangeInputs.length > 0 ) {
        rangeInputs.daterangepicker({
            dateFormat : "dd/mm/yy",
            onChange : function() {
                runReport(reportRange.val(), reportContainer, itemsContainer, window.location.pathname);
            }
        });         
        $('#reportRange').change(function() {
            log("change");
            runReport(reportRange.val(), reportContainer, itemsContainer, window.location.pathname);
        });     
        runReport(reportRange.val(), reportContainer, itemsContainer, window.location.pathname);
    }
    
});
function runReport(range, reportContainer, itemsContainer, href) {
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
            showGraph(resp.data, reportContainer, itemsContainer);
                
        }
    });                
}
function showGraph(graphData, reportContainer, itemsContainer) {
    log("showGraph",reportContainer, graphData);    
    if( graphData ) {
        reportContainer.removeClass("nodata");
        reportContainer.html("");
        if( itemsContainer ) {
            itemsContainer.html("");
        }
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
            if( itemsContainer ) {
                if( graphData.itemFields ) {
                    var table = $("<table><thead><tr></tr></thead><tbody><tr></tr></tbody></table>");
                    var trHeader = table.find("thead tr");
                    $.each(graphData.itemFields, function(i, f) {
                        var td = $("<th>");
                        td.text(f);
                        trHeader.append(td);                    
                    });

                    if( graphData.items) {
                        var tbody = table.find("tbody");
                        $.each(graphData.items, function(i, item) {
                            var tr = $("<tr>");
                            log("item", item);
                            $.each(graphData.itemFields, function(i, f) {
                                log("field", f);
                                var td = $("<td>");
                                td.text(item[f]);
                                tr.append(td);                    
                            });
                            tbody.append(tr);
                        });
                    }
                    itemsContainer.append(table);
                } else {
                    reportContainer.addClass("nodata");
                    reportContainer.html("<p class='nodata'>No data</p>");
                }
            }
        }
    }
}
