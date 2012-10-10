function initManageRepositories() {
    log("initManageRepositories");
    $("form.addRepo").forms({
        callback: function(resp) {
            window.location.reload();
            $.tinybox.close();
        }
    });
    
    $("a.DeleteRepo").click(function(e) {
        e.preventDefault();
        var node = $(e.target);
        var href = node.attr("href");
        var name = getFileName(href);
        confirmDelete(href, name, function() {
            alert("Repository deleted");
            window.location.reload();
        });
    });
    
    $("a.togglePublic").click(function(e) {
        e.preventDefault();
        var node = $(e.target);
        if( node.hasClass("togglePublic-true")) {
            setRepoPublicAccess(node, false);
        } else {
            setRepoPublicAccess(node, true);
        }
    });
}

function setRepoPublicAccess(link, isPublic) {
    $.ajax({
        type: 'POST',
        data: {isPublic: isPublic},
        url: link.attr("href"),
        success: function(data) {
            link.parent().find(".repoPublicAccess").text(isPublic);
            link.addClass("togglePublic-" + isPublic);
            link.removeClass("togglePublic-" + !isPublic);
        },
        error: function(resp) {
            alert("Sorry, couldnt update public access: " +resp);            
        }
    });       
}