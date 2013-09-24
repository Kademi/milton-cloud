jQuery(document).ready(function() {
    initHtmlEditors($(".Modal .htmleditor"), $(window).height() - 300 + "px", null, null, standardRemovePlugins + ",autogrow"); // disable autogrow
    initFiles();
    initAddPageModal();
    $("#myUploaded").mupload({
        url: window.location.pathname,
        buttonText: "Upload a file",
        oncomplete: function(data, name, href) {
            // reload the file list
            log("uploaded ok, now reload file list")
            reloadFileList();
        }
    });
    $(".createFolder").click(function(e) {
        e.stopPropagation();
        e.preventDefault();        
        var parentHref = window.location.pathname;
        showCreateFolder(parentHref, "New folder", "Please enter a name for the new folder", function() {
            reloadFileList();
        });
    });
    
    $(".uploadFiles").click(function(e) {
        e.stopPropagation();
        e.preventDefault();                
        showModal( $("#modalUpload") );
    });
    $(".importFromUrl").click(function(e) {
        e.stopPropagation();
        e.preventDefault();                
        showImportFromUrl();
    });
    $(".filesList").on("click", "a.Edit", function(e) {
        e.preventDefault();
        log("click edit page", e, this);
        var a = $(this);
        var name = a.attr("href");
        var article = a.closest("article");
        showEditModal(name, article);
    });
    $(".filesList").on("click", "a.Delete", function(e) {
        e.stopPropagation();
        e.preventDefault();
        var target = $(e.target);
        var href = target.attr("href");
        log("click delete. href", href);
        var name = getFileName(href);
        var article = target.closest("article");
        confirmDelete(href, name, function() {
            log("deleted", article);
            article.remove();
            alert("Deleted " + name);
        });
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
    $('.lightbox a.image').each(function(i, n) {
        var href = $(n).attr("href");
        $(n).attr("href", href + "/alt-640-360.png");
    });
    log("init lightbox", $('a.image'));
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

    $("#fileList tbody").on("click", "a.delete", function(e) {
        e.stopPropagation();
        e.preventDefault();
        var target = $(e.target);
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
    $("#fileList tbody").on("click", "a.history", function(e) {
        e.stopPropagation();
        e.preventDefault();

    });
    $("a.history").history();
}

function initAddPageModal() {
    log("initAddPageModal", $(".AddModulePage"));
    $(".AddModulePage").click(function(e) {
        e.preventDefault();
        log("initAddPageModal: click");
        var modal = $("#modalCreatePage");
        modal.find("input[type=text], textarea,input[name=pageName]").val("");
        modal.find("form").unbind();
        modal.find("form").submit(function(e) {
            log("submit clicked", e.target);
            e.preventDefault();
            //createPage(modal.find("form"));
            doSavePage(modal.find("form"), null, false);
        });
        $.tinybox.show(modal, {
            overlayClose: false,
            opacity: 0
        });
    });
}


function showEditModal(name, pageArticle) {
    log("showEditModal", name, pageArticle);
    var modal = $("#modalCreatePage");

    modal.find("input[name=pageName]").val(name);
    $.tinybox.show(modal, {
        overlayClose: false,
        opacity: 0,
        top: "10px"
    });
    modal.find("input[type=text], textarea").val("");
    modal.find("form").unbind();
    modal.find("form").submit(function(e) {
        e.preventDefault();
        e.stopPropagation();
        log("edit submit click", e.target);
        doSavePage(modal.find("form"), pageArticle);
    });
    modal.find(".historyBtn").unbind();
    modal.find(".historyBtn").history({
        pageUrl: name,
        showPreview: false,
        afterRevertFn: function() {
            loadModalEditorContent(modal, name);
        }
    });
    loadModalEditorContent(modal, name);
}

function loadModalEditorContent(modal, name) {
    $.ajax({
        type: 'GET',
        url: name + "?type=json",
        dataType: "json",
        success: function(resp) {
            var data = resp.data;
            log("resp", resp);
            var t = data.template;
            if (!t.endsWith(".html"))
                t += ".html";
            log("select template", modal.find("select option[value='" + t + "']"));
            modal.find("select option[value='" + t + "']").attr("selected", true);
            modal.find("input[name=title]").val(data.title);
            modal.find("textarea").val(data.body);
        },
        error: function(resp) {
            log("error", resp);
            alert("err: couldnt load page data");
        }
    });
}

function doSavePage(form, pageArticle) {
    var $form = $(form);
    log("doSavePage", $form);

    resetValidation($form);
    if (!checkRequiredFields(form)) {
        return;
    }

    var $title = $form.find("input[name=title]");
    var data;
    log("check ck editors", CKEDITOR.instances);
    for (var key in CKEDITOR.instances) {
        var editor = CKEDITOR.instances[key];
        var content = editor.getData();
        log("got ck content", key, content, editor);
        var inp = $("textarea[name=" + key + "]", $form);
        if (inp) {
            inp.html(content);
            log("updated", inp);
        }
    }
    data = $form.serialize();

    var url = form.find("input[name=pageName]").val();
    if (url === null || url.length === 0) {
        url = "autoname.new";
    }

    log("do ajax post", $form.attr("action"), data);
    try {
        form.find("button[type=submit]").attr("disabled", "true");
        log("set disabled", form.find("button[type=submit]"));
        form.addClass("ajax-processing");
        $.ajax({
            type: 'POST',
            url: url,
            data: data,
            dataType: "json",
            success: function(data) {
                log("set enabled", form.find("button[type=submit]"));
                form.removeClass("ajax-processing");
                form.find("button[type=submit]").removeAttr("disabled");
                if (data.status) {
                    var title = $title.val();
                    if (pageArticle == null) { // indicated new page
                        var pageName = getFileName(data.messages[0]);
                        var href = data.nextHref;
                        addPageToList(pageName, href, title);
                    } else {
                        pageArticle.find("> span").text(title);
                    }
                    $.tinybox.close();
                } else {
                    alert("There was an error saving the page: " + data.messages);
                }
            },
            error: function(resp) {
                form.removeClass("ajax-processing");
                log("error", resp);
                alert("Sorry, an error occured saving your changes. If you have entered editor content that you dont want to lose, please switch the editor to source view, then copy the content. Then refresh the page and re-open the editor and paste the content back in.");
            }
        });
    } catch (e) {
        log("exception in createJob", e);
    }
    return false;
}


function addPageToList(pageName, href, title) {
    var newRow = $("<article class='modulePage'></article>");
    $(".filesList").append(newRow);
    log("newRow", newRow, $(".fileList"));
    newRow.append("<input type='hidden' value='' name='" + pageName + "'/>");
    newRow.append("<span>" + title + "</span>");
    var aside = $("<aside class='Hidden'></aside>");
    newRow.append(aside);
    var newFileName = getFileName(href);
    aside.append("<a href='" + newFileName + "' class='Edit' title='Edit page'><span class='Hidden'>Edit page</span></a>");
    aside.append("<a href='?goto=" + newFileName + "' target='_blank' class='View' title='View page'><span class='Hidden'>View page</span></a>");
    aside.append("<a href='' class='Move' title='Move up or down'><span class='Hidden'>Move up or down</span></a>");
    aside.append("<a href='" + href + "' class='Delete' title='Delete page'><span class='Hidden'>Delete page</span></a>");
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