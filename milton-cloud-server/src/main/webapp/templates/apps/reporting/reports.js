$(function() {
    var reportContainer = $("#annual");
    var itemsContainer = $("#items");
    var reportRange = $("#reportRange");
    var rangeInputs = $('.dateRange input');
    if (rangeInputs.length > 0) {
        rangeInputs.daterangepicker({
            dateFormat: "dd/mm/yy",
            onChange: function(e) {
                log("onChange", this, e);
                runReport(reportRange.val(), reportContainer, itemsContainer, window.location.pathname);
                updateDatedLinks();
            }
        });
        $('#reportRange').change(function() {
            log("change");
            runReport(reportRange.val(), reportContainer, itemsContainer, window.location.pathname);
            updateDatedLinks();
        });
        runReport(reportRange.val(), reportContainer, itemsContainer, window.location.pathname);
        updateDatedLinks();
    }
});

function updateDatedLinks() {
    var reportRange = $("#reportRange");
    $("a.dated").each(function(i, n) {
        var target = $(n);
        var href = target.attr("href");
        log("href", href);
        var pos = href.indexOf("?");
        if (pos > 0) {
            href = href.substring(0, pos);
        }
        log("href2", href);
        href += "?" + reportRange.val();
        target.attr("href", href);
    });

}

function runReport(range, reportContainer, itemsContainer, href) {
    log("runReport");
    $(".pageMessage").hide(100);
    var arr = range.split("-");
    log("range", range, arr)
    var data = {};
    if (arr.length > 0) {
        data.startDate = arr[0];
    }
    if (arr.length > 1) {
        data.finishDate = arr[1];
    }
    $.ajax({
        type: 'GET',
        url: href,
        dataType: "json",
        data: data,
        success: function(resp) {
            log("response", resp.data);
            if( resp.data !== null && resp.data.data.length === 0 ) {
                $(".pageMessage").html("No data was found for the given criteria").show(300);
            } else {
                showGraph(resp.data, reportContainer, itemsContainer);
            }

        }
    });
}
function showGraph(graphData, reportContainer, itemsContainer) {
    log("showGraph", reportContainer, graphData);
    if (graphData) {
        reportContainer.removeClass("nodata");
        reportContainer.html("");
        if (itemsContainer) {
            itemsContainer.html("");
        }
        if (graphData.data.length > 0) {
            if (graphData.graphType == "Line") {
                showLine(reportContainer, graphData);
            } else if (graphData.graphType == "Bar") {
                showBar(reportContainer, graphData);
            }
            if (itemsContainer) {
                if (graphData.itemFields) {
                    var table = $("<table><thead><tr></tr></thead><tbody><tr></tr></tbody></table>");
                    var trHeader = table.find("thead tr");
                    $.each(graphData.itemFields, function(i, f) {
                        var td = $("<th>");
                        td.text(f);
                        trHeader.append(td);
                    });

                    if (graphData.items) {
                        var tbody = table.find("tbody");
                        $.each(graphData.items, function(i, item) {
                            var tr = $("<tr>");
//                            log("item", item);
                            $.each(graphData.itemFields, function(i, f) {
//                                log("field", f);
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

function showLine(reportContainer, graphData) {
    Morris.Line({
        element: reportContainer,
        data: graphData.data,
        xkey: graphData.xkey,
        ykeys: graphData.ykeys,
        labels: graphData.labels,
        hideHover: true,
        dateFormat: function(x) {
            var dt = new Date(x).formatDDMMYYYY();
            //var dt = new Date(x).toString();
            //log("formatted date", x, dt, new Date(x).formatDDMMYYYY());
            return dt;
        } // see common.js
    });
}

function showBar(reportContainer, graphData) {
    Morris.Bar({
        element: reportContainer,
        data: graphData.data,
        xkey: graphData.xkey,
        ykeys: graphData.ykeys,
        labels: graphData.labels
    });

}