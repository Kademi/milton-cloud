$(function() {
    $("body").on("click", ".DeleteOrg", function(e, n) {
        e.preventDefault();
        var node = $(e.target);
        var href = node.attr("href");
        var name = getFileName(href);
        confirmDelete(href, name, function() {
            window.location.reload();
        });
    });
    $("body").on("click", ".editOrg", function(e, n) {
        e.preventDefault();
        var node = $(e.target);
        var href = node.attr("href");
        showEditOrg(href);
    });
    $("#editOrgModal form").forms({
        callback: function(resp) {
            log("done", resp);
            alert("Saved ok. Please refresh to see changes");
            $.tinybox.close();
        }
    });
    initSearchOrgs();
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
            log("oncomplete:", data.result, name, href);
            if (data.result.status) {
                $(".results .numUpdated").text(data.result.data.numUpdated);
                $(".results .numUnmatched").text(data.result.data.unmatched.length);
                showUnmatched(data.result.data.unmatched);
                $(".results").show();
                alert("Upload completed. Please review any unmatched organisations below, or refresh the page to see the updated list of organisations");
            } else {
                alert("There was a problem uploading the organisations: " + data.result.messages);
            }
        }
    });
    var uploadForm = $("#doUploadCsv form");
    $("#allowInserts").click(function(e) {
        log("click", e.target);
        if ($(e.target).is(":checked")) {
            uploadForm.attr("action", "orgs.csv?insertMode=true");
        } else {
            uploadForm.attr("action", "orgs.csv");
        }
    });
    $("a.Add.org").click(function() {
        showEditOrg(null);
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
    $("#orgQuery").keyup(function() {
        typewatch(function() {
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
        },
        error: function(resp) {
            alert("err");
        }
    });
}

function showEditOrg(orgHref) {
    var modal = $("#editOrgModal");
    $.tinybox.show(modal, {
        overlayClose: false,
        opacity: 0
    });
    if (orgHref) {
        modal.find("form").attr("action", orgHref);
    } else {
        modal.find("form").attr("action", window.location.pathname + "?newOrg");
    }
    modal.find("input").val("");
    modal.find("select").val("");
    log("select", modal.find("select").val());
    resetValidation(modal);
    if (orgHref) {
        $.ajax({
            type: 'GET',
            url: orgHref,
            dataType: "json",
            success: function(resp) {
                log("success", resp);
                for (var key in resp.data) {
                    var val = resp.data[key];
                    modal.find("[name='" + key + "']").val(val);
                }
            },
            error: function(resp) {
                alert("err");
            }
        });
    }
}