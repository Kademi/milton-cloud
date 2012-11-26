$(function() {
    $("form.addOrg").forms({
        callback: function(resp) {
            log("done", resp);
            window.location.reload();
        }
    });                
    $("body").on("click", ".DeleteOrg", function(e, n) {
        e.preventDefault();
        var node = $(e.target);
        var href = node.attr("href");
        var name = getFileName(href);
        confirmDelete(href, name, function() {
            window.location.reload();
        });                    
    });
    initSearchOrgs();
    initControl();
    $(".showUploadCsvModal").click(function() {
        $.tinybox.show($("#modalUploadCsv"), {
            overlayClose: false,
            opacity: 0
        });                       
    });                
                
    $("#doUploadCsv").mupload({
        buttonText: "Upload spreadsheet",
        url: "orgs.csv",
        useJsonPut: false,
        oncomplete: function(data, name, href) {
            log("oncomplete:", data.result.data, name, href);
            $(".results .numUpdated").text(data.result.data.numUpdated);
            $(".results .numUnmatched").text(data.result.data.unmatched.length);
            showUnmatched(data.result.data.unmatched);
            $(".results").show();
            alert("Upload completed. Please review any unmatched organisations below, or refresh the page to see the updated list of organisations");
        }
    });    
    var uploadForm = $("#doUploadCsv form");
    $("#allowInserts").click(function(e) {
        log("click", e.target);
        if( $(e.target).is(":checked")) {
            uploadForm.attr("action", "orgs.csv?insertMode=true");
        } else {
            uploadForm.attr("action", "orgs.csv");
        }        
    });
});
function showUnmatched(unmatched) {
    var unmatchedTable = $(".results table");
    var tbody = unmatchedTable.find("tbody");
    tbody.html("");
    $.each(unmatched, function(i, row) {
        log("unmatched", row);
        var tr = $("<tr>");
        $.each(row, function(ii, field) {
            tr.append("<td>" + field + "</td>");
        });        
        tbody.append(tr);
    });
    unmatchedTable.show();
}     


function initSearchOrgs() {
    $("#orgQuery").keyup(function () {
        typewatch(function () {
            doSearch();
        }, 500);
    });    
}

function doSearch() {
    var newUrl = window.location.pathname + "?q=" + $("#orgQuery").attr("value");    
    $.ajax({
        type: 'GET',
        url: newUrl,
        success: function(data) {
            log("success", data);
            window.history.pushState("", document.title, newUrl);
            var $fragment = $(data).find("#searchResults");
            log("replace", $("#searchResults"));
            log("frag", $fragment); 
            $("#searchResults").replaceWith($fragment);
            initControl();            
        },
        error: function(resp) {
            alert("err");
        }
    });      
}

function initControl() {
    $("div.Info a.ShowDialog").click(function(e) {
        var _this = $(this);
        var dialog = _this.parent().find("div.Dialog");
        dialog.toggle(100);
        e.preventDefault();
    });
}