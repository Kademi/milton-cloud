/**
 *
 *  jquery.history.js
 *
 * Config:
 * pageUrl - url of the resource to show history for.
 * 
 */

(function( $ ) {
    $.fn.history = function(options) {
        var btn = this;
        var config = $.extend( {
            'pageUrl' : window.location,
            'showPreview': true,
            'afterRevertFn': function() {
                window.location.reload();
            }
        }, options);  
        btn.click(function(e) {
            e.preventDefault();
            showHistory(btn, config);
        });

    };


    function showHistory(btn, config) {
        var modal = $("div.history");
        if( modal.length == 0 ) {
            modal = $("<div class='history' style='display: none'><a title='Close' href='#' class='Close'><span class='Hidden'>Close</span></a><h3>History</h3><div class='historyList'></div></div>");
            $("body").append(modal);
            modal.find(".historyList").append("<table><tbody></tbody></table>");
            modal.find("a.Close").click(function(e) {
                e.preventDefault();
                modal.hide(300);
            });
        }        
        var tbody = modal.find("tbody");
        loadHistory(tbody, config);
        log("show modal", modal);
        var p = btn.offset();
        modal.css("position", "fixed");
        modal.css("top", p.top + btn.height() + "px");
        modal.css("left", p.left + "px");
        modal.css("z-index", "10000");
        modal.show(300);
    }

    function loadHistory(tbody, config) {
        log("loadHistory", config.pageUrl);
        try {
            $.ajax({
                type: 'GET',
                url: config.pageUrl + "/.history",
                dataType: "json",
                success: function(resp) {
                    ajaxLoadingOff();
                    log("got history", resp);
                    buildHistoryTable(resp.data, tbody, config);                
                },
                error: function(resp) {
                    ajaxLoadingOff();
                    alert("err");
                }
            });     
        } catch(e) {
            log("exception", e);
        }
    
    }

})( jQuery );


function buildHistoryTable(data, tbody, config) {
    tbody.html("");
    $.each(data, function(i, n) {
        var date = new Date(n.modDate);
        var formattedDate = date.toISOString();
        var tr = $("<tr class='version'>");
        var tdFirst = $("<td>");
        tr.append(tdFirst);
        var btnRevert = $("<button class='history' title='Restore this version'>Rollback</button>");
        tdFirst.append(btnRevert);        
        tr.append("<td>" + n.description + "</td>");
        tr.append("<td><abbr class='timeago' title='" + formattedDate + "'>" + formattedDate + "</abbr></td>");
        tr.append("<td>" + n.user.name + "</td>");
        if( config.showPreview ) {
            tr.append("<td><a target='_blank' href='" + config.pageUrl + ".preview?version=" + n.hash + "'>Preview</td>");
        }
        tbody.append(tr);        
        btnRevert.click(function(e) {
            e.preventDefault();
            confirmRevert(n.hash, tbody, config);
        });
    });
    tbody.find("abbr.timeago").timeago();
}

function confirmRevert(hash, tbody, config) {
    //alert("Rollback to: " + hash);
    if( confirm("Are you sure you want to revert?")) {
        revert(hash, tbody, config);
    }
    
}

function revert(hash, tbody, config) {
    try {
        $.ajax({
            type: 'POST',
            url: config.pageUrl + "/.history",
            data: {
                revertHash: hash
            },
            dataType: "json",
            success: function(resp) {
                ajaxLoadingOff();
                log("got history after revert", resp);
                if( tbody ) {
                    buildHistoryTable(resp.data, tbody, config);
                }
                if( config.afterRevertFn ) {
                    config.afterRevertFn(resp.data, config);
                }
            },
            error: function(resp) {
                ajaxLoadingOff();
                alert("err");
            }
        });     
    } catch(e) {
        log("exception", e);
    }
}