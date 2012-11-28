function initManageOrgTypes() {	
    initEditing();
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
    var modal = $("div.Modal.newOrgType");
    $.tinybox.show(modal, {
        overlayClose: false,
        opacity: 0
    });     
    modal.find("form").attr("action", orgHref);
    $.ajax({
        type: 'GET',
        url: orgHref,
        dataType: "json",
        success: function(resp) {
            log("success", resp);
            for(var key in resp.data) {
                var val = resp.data[key];
                modal.find("[name='" + key + "']").val(val);
            }
        },
        error: function(resp) {
            alert("err");
        }
    });     
}