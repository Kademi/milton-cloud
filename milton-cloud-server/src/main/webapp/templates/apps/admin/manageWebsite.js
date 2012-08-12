function initManageWebsite() {
    log("initManageWebsite");
    $(".AddGroup").click(function() {
        var modal = $("#modalGroup");
        $.tinybox.show(modal, {
            overlayClose: false,
            opacity: 0
        });        
    });
    initGroupCheckbox();
}


function initGroupCheckbox() {
    $("#modalGroup input[type=checkbox]").click(function() {
        var $chk = $(this);
        log("checkbox click", $chk, $chk.is(":checked"));
        var isRecip = $chk.is(":checked");
        setGroupRecipient($chk.attr("name"), isRecip);
    });
}

function setGroupRecipient(name, isRecip) {
    log("setGroupRecipient", name, isRecip);
    try {
        $.ajax({
            type: 'POST',
            url: window.location.pathname,
            data: {
                group: name,
                isRecip: isRecip
            },
            success: function(data) {
                log("saved ok", data);
                if( isRecip ) {
                    $(".GroupList").append("<li class=" + name + ">" + name + "</li>");
                    log("appended to", $(".GroupList"));
                } else {
                    $(".GroupList li." + name).remove();
                    log("removed from", $(".GroupList"));
                }
            },
            error: function(resp) {
                log("error", resp);
                alert("err");
            }
        });          
    } catch(e) {
        log("exception in createJob", e);
    }       
}