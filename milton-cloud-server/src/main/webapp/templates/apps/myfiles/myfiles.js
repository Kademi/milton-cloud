jQuery(document).ready(function(){
    initFiles();
    $("#myUploaded").mupload({
        url: window.location.pathname,
        buttonText: "Upload a file",
        oncomplete: function(data, name, href) {
            // reload the file list
            log("uploaded ok, now reload file list")
            $.get(window.location.pathname, "", function(resp) {
                log("got file list", resp);
                var html = $(resp);
                $("#fileList").replaceWith(html.find("#fileList"));
                initPseudoClasses();
                initFiles();
            });
        }
    });
});            
function initFiles() {
    log("initFiles");
    $('a.image').each(function(i, n) {
        var href = $(n).attr("href");
        $(n).attr("href", href + "/alt-640-360.png");
    });
    $('a.image').lightBox(
    {
        imageLoading: '/static/images/lightbox-ico-loading.gif',
        imageBtnClose: '/static/images/lightbox-btn-close.gif',
        imageBtnPrev: '/static/images/lightbox-btn-prev.gif',
        imageBtnNext: '/static/images/lightbox-btn-next.gif',
        imageBlank: '/static/images/lightbox-blank.gif',
        containerResizeSpeed: 350                    
    }                
    );
    jQuery("abbr.timeago").timeago();
    jQuery("table.bar .file a").each(function(index, node) {                    
        tag = $(node);
        var href = tag.attr("href");
        var icon = findIconByExt(href);
        $("img", tag).attr("src", icon);
    });
    $("#fileList tbody").on("mouseenter", "tr", function(e) {
        var target = $(e.target);
        addFileTools(target.closest("tr"));
    });
    $("#fileList tbody").on("mouseleave", "tr", function(e) {
        var target = $(e.target);
        removeFileTools(target.closest("tr"));
    });    
    $("#fileList tbody").on("click", "a.delete", function(e) {
        e.stopPropagation();
        e.preventDefault();
        var target = $(e.target);
        var href = target.attr("href");
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

function addFileTools(tr) {
    var td = tr.find("td:last-child");
    log("addFileTools", td);
    var href = tr.find("a.hidden").first().attr("href"); // a.hidden does not get changed by lightbox
    var toolsDiv = $("<div class='tools'><a class='delete'>Delete</a><a class='rename'>Rename</a><a target='_blank' class='download'>Download</a></div>")
    toolsDiv.find("a").attr("href", href);
    td.append(toolsDiv);
}

function removeFileTools(tr) {
    tr.find(".tools").remove();
}
