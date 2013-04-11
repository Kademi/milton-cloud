jQuery(document).ready(function() {
    initFiles();
    $("#myUploaded").mupload({
        url: window.location.pathname,
        buttonText: "Upload a file",
        oncomplete: function(data, name, href) {
            // reload the file list
            log("uploaded ok, now reload file list")
            reloadFileList();
        }
    });
    $(".newFolder").click(function(e) {
        var parentHref = window.location.pathname;
        showCreateFolder(parentHref, "New folder", "Please enter a name for the new folder", function() {
            reloadFileList();
        });
    });
    $(".importFromUrl").click(function() {
        showImportFromUrl();
    });
});

function reloadFileList() {
    $.get(window.location.pathname, "", function(resp) {
        log("got file list", resp);
        var html = $(resp);
        $("#fileList").replaceWith(html.find("#fileList"));
        initPseudoClasses();
        initFiles();
    });

}

function initFiles() {
    log("initFiles");
    $('a.image').each(function(i, n) {
        var href = $(n).attr("href");
        $(n).attr("href", href + "/alt-640-360.png");
    });
    $('a.image').lightBox({
        imageLoading: '/static/images/lightbox-ico-loading.gif',
        imageBtnClose: '/static/images/lightbox-btn-close.gif',
        imageBtnPrev: '/static/images/lightbox-btn-prev.gif',
        imageBtnNext: '/static/images/lightbox-btn-next.gif',
        imageBlank: '/static/images/lightbox-blank.gif',
        containerResizeSpeed: 350
    });
    jQuery("abbr.timeago").timeago();
    jQuery("table.bar .file a").each(function(index, node) {
        tag = $(node);
        var href = tag.attr("href");
        var icon = findIconByExt(href);
        $("img", tag).attr("src", icon);
    });
    $("#fileList tbody").on("mouseenter", "tr", function(e) {
        var target = $(e.target);
        showFileTools(target.closest("tr"));
    });
    $("#fileList tbody").on("mouseleave", "tr", function(e) {
        var target = $(e.target);
        hideFileTools(target.closest("tr"));
    });
    $("#fileList tbody").on("click", "a.delete", function(e) {
        e.stopPropagation();
        e.preventDefault();
        var target = $(e.target);
        log("click target", target);
        target = target.closest("tr").find("> td a");
        var href = target.attr("href");
        log("click delete. href", href);
        var name = getFileName(href);
        var tr = target.closest("tr");
        confirmDelete(href, name, function() {
            log("deleted", tr);
            tr.remove();
            alert("Deleted " + name);
        });
    });
    $("#fileList tbody").on("click", "a.rename", function(e) {
        e.stopPropagation();
        e.preventDefault();
        var target = $(e.target);
        var href = target.attr("href");
        promptRename(href, function(resp) {
            window.location.reload();
        });
    });
}

function showFileTools(tr) {
    var td = tr.find("td:last-child");
    log("showFileTools", td);
    var href = tr.find("a.hidden").first().attr("href"); // a.hidden does not get changed by lightbox
    var toolsDiv = td.find("div.tools");
    if (toolsDiv != null) {
        toolsDiv = $("<div class='tools'><a class='delete'>Delete</a><a class='rename'>Rename</a><a target='_blank' class='download'>Download</a></div>")
        toolsDiv.find("a").attr("href", href);
        td.append(toolsDiv);
    } else {
        toolsDiv.show();
    }
}

function hideFileTools(tr) {
    tr.find(".tools").hide();
}

function showImportFromUrl() {
    var url = prompt("Please enter a url to import files from");
    if (url) {
        $.ajax({
            type: 'POST',
            url: window.location.pathname,
            dataType: "json",
            data: {
                importFromUrl: url
            },
            success: function(data) {
                log("response", data);
                if (!data.status) {
                    alert("Failed to import");
                    return;
                } else {
                    alert("Importing has finished");
                    window.location.reload();
                }
            },
            error: function(resp) {
                log("error", resp);
                alert("err");
            }
        });
    }
}