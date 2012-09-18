function initProfile() {
    log("initProfile");
    initOrgSearch();
    initNewMembershipForm();
    $("form").not("div.Modal.newUser form").forms({
        callback: function(resp, form) {                        
            confirmMessage = form.closest("div").find("p.confirm");                        
            log("done", confirmMessage);
            confirmMessage.show(500, function() {
                confirmMessage.hide(5000);
            })
        }
    });
    $("#myUploaded").mupload({
        url: "",
        buttonText: "Select a new photo",
        oncomplete: function(data, name, href) {
            log("set img", jQuery(".profilePic img"));
            jQuery(".profilePic img").attr("src", "pic");
        }
    });  
    log("init delete membersip");
    $("div.groups").on("click", "li a", function(e) {
        log("click", this);
        e.preventDefault();
        e.stopPropagation();
        if( confirm("Are you sure you want to delete this group membership? WARNING: If this is the last membership you will not be able to edit the user.")) {
            var a = $(e.target);
            log("do it", a);
            var href = a.attr("href");
            deleteFile(href, function() {
                a.closest("li").remove();
            });
        }
    });
}

function initNewMembershipForm() {
    var modal = $("div.Modal.newUser");
    $(".AddGroup").click(function(e) {
        e.preventDefault();
        e.stopPropagation();
        $.tinybox.show(modal, {
            overlayClose: false,
            opacity: 0
        });            
    });
    modal.find("form").forms({
        callback: function(resp) {
            log("done new membership", resp);
            $.tinybox.close();
            reloadMemberships();
        }
    });   
}

function reloadMemberships() {
    $.ajax({
        type: 'GET',
        url: window.location.pathname,
        success: function(data) {
            log("success", data)
            var $fragment = $(data).find("ul.memberships");
            var orig = $("ul.memberships");
            log("replace", orig, $fragment);
            orig.replaceWith($fragment);
            
        },
        error: function(resp) {
            alert("error: " + resp);
        }
    });      
}