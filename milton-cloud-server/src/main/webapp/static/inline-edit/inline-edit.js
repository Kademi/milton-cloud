$(function() {
    var adminToolbar = $("<div class='adminToolbar'>");
    var btnEdit = $("<button class='edit'>Edit page</button>");
    btnEdit.click(function() {
        edifyPage($('.contentForm'), ['/templates/themes/3dn/theme.css']);
    });
    var btnNew = $("<button class='new'>New Page</button>");
    adminToolbar.append(btnEdit).append(btnNew);
    $("body").append(adminToolbar);
});

function edifyPage(container, cssFiles, callback) {
    log("edifyPage", container, callback);
    $("body").removeClass("edifyIsViewMode");
    $("body").addClass("edifyIsEditMode");
        
    if( !callback ) {
        callback = function(resp) {
            if( resp.nextHref) {
            //window.location = resp.nextHref;
            } else {
            //window.location = window.location.pathname;
            }            
        };
    }
    
    container.animate({
        opacity: 0
    }, 200, function() {
        initHtmlEditors(container.find(".htmleditor"), cssFiles);
    
        $(".inputTextEditor").each(function(i, n) {
            var $n = $(n);
            var s = $n.text();
            $n.replaceWith("<input name='" + $n.attr("id") + "' type='text' value='" + s + "' />");
        });        
        container.wrap("<form id='edifyForm' action='" + window.location + "' method='POST'></form>");
        $("#edifyForm").append("<input type='hidden' name='body' value='' />");
        var buttons = $("<div class='buttons'></div>");
        $("#edifyForm").prepend(buttons);
        var title = $("<input type='text' name='title' id='title' title='Enter the page title here' />");
        title.val(document.title);
        buttons.append(title);
        buttons.append("<button title='Save edits to the page' class='save' type='submit'>Save</button>");
        buttons.append("<button title='View page history' class='history' type='button'>History</button>");
        var btnCancel = $("<button title='Return to view mode without saving. Any changes will be lost' class='cancel' type='button'>Cancel</button>");
        btnCancel.click(function() {
            window.location.reload()
        });
        buttons.append(btnCancel);
        buttons.on("click", "button.history", function(){
            showHistory();
        });
                        
        $("#edifyForm").submit(function(e) {
            e.preventDefault();
            log("inlineedit: edifyPage: submit page");
            submitEdifiedForm(function() {
                window.location.reload();
            });
        });
        log("done hide, now show again");
        container.animate({
            opacity: 1
        },500);
    });

}

function showHistory() {
    var modal = $(".Modal.history");
    if( modal.length == 0 ) {
        modal = $("<div class='Modal history'><header><h3>History</h3><a title='Close' href='#' class='Close'><span class='Hidden'>Close</span></a></header><div class='ModalContent'></div></div>");
        $("body").append(modal);
        modal.find(".ModalContent").append("<table><tbody></tbody></table>");
        modal.find("a.Close").click(function(e) {
            e.preventDefault();
            $.tinybox.close();                    
        });
    }        
    var tbody = modal.find("tbody");
    loadHistory(tbody);
    log("show modal", modal);
    $.tinybox.show(modal, {
        overlayClose: false,
        opacity: 0
    });   
}

function loadHistory(tbody) {
    try {
        $.ajax({
            type: 'GET',
            url: window.location.pathname + "/.history",
            dataType: "json",
            success: function(resp) {
                ajaxLoadingOff();
                log("got history", resp);
                buildHistoryTable(resp.data, tbody);                
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

function buildHistoryTable(data, tbody) {
    tbody.html("");
    $.each(data, function(i, n) {
        var date = new Date(n.modDate);
        var formattedDate = date.toISOString();
        var tr = $("<tr class='version'>");
        var tdFirst = $("<td>");
        tr.append(tdFirst);
        var btnRevert = $("<button class='history'>Rollback</button>");
        tdFirst.append(btnRevert);        
        tr.append("<td>" + n.description + "</td>");
        tr.append("<td><abbr class='timeago' title='" + formattedDate + "'>" + formattedDate + "</abbr></td>");
        tr.append("<td>" + n.user.name + "</td>");
        tr.append("<td><a target='_blank' href='" + window.location.pathname + ".preview?version=" + n.hash + "'>Preview</td>");
        tbody.append(tr);        
        btnRevert.click(function() {
            alert("Rollback to: " + n.hash);
            revert(n.hash, tbody);
        });
    });
    tbody.find("abbr.timeago").timeago();
}

function revert(hash, tbody) {
    try {
        $.ajax({
            type: 'POST',
            url: window.location.pathname + ".history",
            data: {
                revertHash: hash
            },
            dataType: "json",
            success: function(resp) {
                ajaxLoadingOff();
                log("got history after revert", resp);
                buildHistoryTable(resp.data, tbody);                
                reloadContent();
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

function reloadContent() {
    window.location.reload(); // TODO: use ajax to load content into current window (like pjax)
}