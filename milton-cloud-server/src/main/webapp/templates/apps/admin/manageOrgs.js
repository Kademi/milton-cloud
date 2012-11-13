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