/**
 *
 *  jquery.history.js
 *
 * Config:
 * pageUrl - url of the resource to show history for. For link tags will use the href by default
 * showPreview - whether or not to show a preview link in the history table
 * 
 */

(function($) {
    $.fn.history = function(options) {
        var btn = this;
        var config = $.extend({
            'pageUrl': null,
            'showPreview': true,
            'afterRevertFn': function() {
                window.location.reload();
            },
            'getPageUrl': function(target) {
                if (target) {
                    var href = target.attr("href");
                    if (href && href.length > 0 && !href.equals("#")) {
                        return href;
                    }
                }
                if (this.pageUrl !== null) {
                    return this.pageUrl;
                } else {
                    return window.location.pathname;
                }
            }
        }, options);
        btn.click(function(e) {
            log("show history");
            e.preventDefault();
            showHistory(btn, config);
        });

    };
})(jQuery);


function showHistory(btn, config) {
    var modal = $("div.history");
    if (modal.length == 0) {
        modal = $("<div class='history' style='display: none'><a title='Close' href='#' class='Close'><span class='Hidden' style='display: none;'>Close</span></a><h3>History</h3><div class='historyList'></div></div>");
        $("body").append(modal);
        modal.find(".historyList").append("<table><tbody></tbody></table>");
        modal.find("a.Close").click(function(e) {
            e.preventDefault();
            modal.hide(300);
        });
    }
    var tbody = modal.find("tbody");
    loadHistory(tbody, config);
    var p = btn.offset();
    modal.css("position", "fixed");
    modal.css("top", p.top + btn.height() + "px");
    var left = p.left;
    var windowWidth = $(window).width();
    var modalWidth = modal.width();
    if (modalWidth < 350) {
        modalWidth = 350;
    }
    if (left + modalWidth > windowWidth) {
        left = windowWidth - modalWidth;
    }
    modal.css("left", left + "px");
    modal.css("z-index", "10000");
    modal.show(300);
}

function loadHistory(tbody, config) {
    log("loadHistory", config);
    try {
        var href = suffixSlash(config.getPageUrl()) + ".history";

        $.ajax({
            type: 'GET',
            url: href,
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
    } catch (e) {
        log("exception", e);
    }

}


function buildHistoryTable(data, tbody, config) {
    tbody.html("");
    if (data.length === 0) {
        var tr = $("<tr class='version'><td colspan='3'>No history information is available</td></tr>");
        tbody.append(tr);
    } else {
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
            if (config.showPreview) {
                log("show preview", config.getPageUrl());
                tr.append("<td><a target='_blank' href='" + config.getPageUrl() + ".preview?version=" + n.hash + "'>Preview</td>");
            }
            tbody.append(tr);
            btnRevert.click(function(e) {
                e.preventDefault();
                confirmRevert(n.hash, tbody, config);
            });
        });
    }
    tbody.find("abbr.timeago").timeago();
}

function confirmRevert(hash, tbody, config) {
    //alert("Rollback to: " + hash);
    if (confirm("Are you sure you want to revert?")) {
        revert(hash, tbody, config);
    }

}

function revert(hash, tbody, config) {
    try {
        $.ajax({
            type: 'POST',
            url: config.getPageUrl() + "/.history",
            data: {
                revertHash: hash
            },
            dataType: "json",
            success: function(resp) {
                ajaxLoadingOff();
                log("got history after revert", resp);
                if (tbody) {
                    buildHistoryTable(resp.data, tbody, config);
                }
                if (config.afterRevertFn) {
                    config.afterRevertFn(resp.data, config);
                }
            },
            error: function(resp) {
                ajaxLoadingOff();
                log("error response", resp);
                alert("Sorry, there was an error reverting to the previous version. Please check your internet connection");
            }
        });
    } catch (e) {
        log("exception", e);
        alert("Sorry, there was an exception reverting to the previous version. Please check your internet connection");
    }
}