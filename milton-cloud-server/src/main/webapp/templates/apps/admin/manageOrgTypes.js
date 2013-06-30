function initManageOrgTypes() {
    initEditing();
    initManageOrgTypeModal();    
}

function initManageOrgTypeModal() {
    $("body").on("click", ".addField", function(e) {
        e.preventDefault();
        var name = prompt("Please enter a name for the new field");
        if( name === "" ) {
            return;
        } else if( name.contains(" ")) {
            alert("Please enter a name without spaces or other special characters");
            return;
        }
        var newLi = $("<li>");
        $("ul.fields").append(newLi);
        newLi.append($("<h4>").text(name) );
        newLi.append($("<input type='text' name='field-" + name + "'/>"));
        newLi.append($("<a href='" + name +"' class='removeField'>Delete</a>"));                                                            
    });
}

function initEditing() {
    var modal = $("div.Modal.newOrgType");
    $("a.newOrgType").click(function(e) {
        e.preventDefault();
        e.stopPropagation();
        modal.find("input").val("");
        $.tinybox.show(modal, {
            overlayClose: false,
            opacity: 0
        });
    });

    $("body").on("click", "a.editOrgType", function(e) {
        e.preventDefault();
        e.stopPropagation();

        var node = $(e.target);
        var href = node.attr("href");
        showEditForm(href);
    });

    $("body").on("click", "a.deleteOrgType", function(e) {
        e.preventDefault();
        e.stopPropagation();

        var node = $(e.target);
        var href = node.attr("href");
        var name = getFileName(href);
        log("delete", href);
        confirmDelete(href, name, function() {
            log("remove ", this);
            window.location.reload();
        });
    });
    modal.find("form").forms({
        callback: function(resp) {
            log("done new org type", resp);
            $.tinybox.close();
            window.location.reload();
        }
    });
}


function showEditForm(orgHref) {
    var modal = $(".Modal.editOrgType");
    modal.load(orgHref + " #editOrgTypeModal", function() {
        modal.find("form").forms({
            callback: function(resp) {
                log("done", resp);
                $.tinybox.close();
                //window.location.reload();
                $("div.content").load(window.location.pathname + " div.content > *", function() {

                });
            }
        });
        log("done forms", modal);

        $.tinybox.show(modal, {
            overlayClose: false,
            opacity: 0
        });
        log("showed modal");
    });

}