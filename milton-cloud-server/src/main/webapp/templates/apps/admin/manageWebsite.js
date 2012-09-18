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
            dataType: "json",
            success: function(data) {
                if( data.status ) {
                    log("saved ok", data);
                    if( isRecip ) {
                        $(".GroupList").append("<li>" + name + "</li>");
                        log("appended to", $(".GroupList"));
                    } else {
                        var toRemove = $(".GroupList li").filter(function() {
                            return $(this).text() == name;
                        });
                        toRemove.remove();
                    }
                } else {
                    log("error", data);
                    alert("Sorry, couldnt save " + data);
                }
            },
            error: function(resp) {
                log("error", resp);
                alert("Sorry, couldnt save - " + resp);
            }
        });          
    } catch(e) {
        log("exception in createJob", e);
    }       
}