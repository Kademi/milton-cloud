$(function() {
    if( privs) {
        log("check privs", privs, $.inArray("WRITE", privs));
        if( $.inArray("WRITE", privs) < 0 ) {
            return;
        }
        var formContainer = $(".contentForm");
        if( formContainer.length == 0 ) {
            return;
        }        
        initInlineEdit(formContainer);
    }
});

function initInlineEdit(formContainer) {    
    log("initInlineEdit", formContainer, privs);
    var adminToolbar = $("<div class='adminToolbar'>");
    var btnEdit = $("<button class='edit'>Edit page</button>");
    btnEdit.click(function() {
        edifyPage(".contentForm");
    });
    var btnNew = $("<button class='new'>New Page</button>");
    btnNew.click(function() {
        var href = window.location.href;
        href = getFolderPath(href);
        href = href + "/autoname.new?template=learner/modulePage"; // TODO: make configurable somewhoe
        window.location.href = href;
    });
    adminToolbar.append(btnEdit).append(btnNew);
    $("body").append(adminToolbar);
        
    $("link").each(function(i ,n){
        var link = $(n);
        if( link.attr("rel") == "stylesheet" && link.attr("media") == "screen" ) {
            var href = link.attr("href");
            log("push theme css file", href);
            themeCssFiles.push(href);
        }
    });
    if( window.location.href.endsWith(".new")) {
        edifyPage(".contentForm");
    }    
}

function edifyPage(selector) {
    log("edifyPage", container);
    $("body").removeClass("edifyIsViewMode");
    $("body").addClass("edifyIsEditMode");
            
    var container = $(selector);
    container.animate({
        opacity: 0
    }, 500);
    
    log("do ajax get...");
    $.ajax({
        type: 'GET',
        url: window.location.pathname,
        success: function(resp) {
            log("got page content");
            ajaxLoadingOff();                
            var page = $(resp);                
            var newContentForm = page.find(".contentForm");
            log("got data", newContentForm);
            $(".contentForm").replaceWith(newContentForm);
            
            // now we've loaded the content we must re-select the container
            container = $(selector);            
            
            initHtmlEditors(container.find(".htmleditor"));
        
            $(".inputTextEditor").each(function(i, n) {
                var $n = $(n);
                var s = $n.text();
                $n.replaceWith("<input name='" + $n.attr("id") + "' type='text' value='" + s + "' />");
            });        
            container.wrap("<form id='edifyForm' action='" + window.location + "' method='POST'></form>");
            var form = $("#edifyForm");
            form.append("<input type='hidden' name='body' value='' />");
            var buttons = $("<div class='buttons'></div>");
            form.prepend(buttons);
            form.prepend("<div class='pageMessage'>.</div>");
            var title = $("<input type='text' name='title' id='title' title='Enter the page title here' class='required' />");
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
                            
            form.submit(function(e) {
                e.preventDefault();
                log("inlineedit: edifyPage: submit page");
                submitEdifiedForm(function(resp) {
                    if( resp.nextHref ) {
                        window.location.href = resp.nextHref;
                    } else {
                        window.location.reload();
                    }
                });
            });
            log("now show again");
            container.animate({
                opacity: 1
            },500);            
        },
        error: function(resp) {
            ajaxLoadingOff();
            alert("There was an error loading the page content into the editor. Please refresh the page and try again");
        }
            
    });    

}



function submitEdifiedForm(callback) {
    var form = $("#edifyForm");
    log("submit form", form);
    for( var key in CKEDITOR.instances) {
        var editor = CKEDITOR.instances[key];
        var content = editor.getData();
        var inp = $("input[name=" + key + "], textarea[name=" + key + "]");
        if( inp.length > 0 ) {
            inp.val(content);
        } else {
            inp = $("<input type='hidden' name='" + key + "/>");
            form.append(inp);
            inp.val(content);
        }
        log("copied html editor val to:", inp, "for", key);
    }
        
    resetValidation(form);
    if( !checkRequiredFields(form)) {
        return false;
    }        
        
    var data = form.serialize();
    log("serialied", data);

    try {
        //$("#edifyForm input[name=body]").attr("value", CKEDITOR.instances["editor1"].getData() );
        $.ajax({
            type: 'POST',
            url: $("#edifyForm").attr("action"),
            data: data,
            dataType: "json",
            success: function(resp) {
                ajaxLoadingOff();
                log("common.js: edify: save success", resp, window.location.path);
                if( callback ) {
                    log("call callback", callback);
                    callback(resp);
                } else {
                    log("no callback");
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
    return false;    
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