
function initMessage(markRead, page) {
    log("initMessage", $(".notificationBox .close"));
    $(".notificationBox .close").click(function(e) {
        e.preventDefault();
        e.stopPropagation();
        
        var link = $(this);
        var deletedHref = link.attr("href");
        confirmDelete(deletedHref, getFileName(deletedHref), function() {
            log("deleted", link);
            var parent = link.closest("div");
            var next = parent.next();
            var container = $(".notificationBox");
            parent.remove();

            var messages = container.find("div.message");
            log("container", container, messages);

            // if we've deleted the current page need to go elsewhere
            log("check if deleted current page", deletedHref, window.location.pathname);
            if( deletedHref == window.location.pathname ) {                
                if( messages.length == 0 ) {                
                    $(".nav-menuNotifications span").text("");
                    window.location.href = "/inbox/";
                } else {
                    $(".nav-menuNotifications span").text(messages.length);
                    if( next.length == 0 ) {
                        next = messages.first();
                    }                
                    var nextHref = next.find("h3 a").attr("href");
                    log("next page", next, nextHref);
                    if( nextHref ) {
                        log("next href", nextHref);
                        window.location.href = nextHref;
                    }
                }
            }
            
        });
    });
    $(".notificationBox div.message").click(function(e) {
        log("click", this);
        e.preventDefault();
        e.stopPropagation();        
        var href = $(this).find("h3 a").attr("href");
        window.location.href = href;
    });
    
    jQuery("abbr.timeago").timeago();
    if(!markRead) {
        return;
    }
    log("mark read");
    jQuery.ajax({
        type: 'POST',
        url: page,
        data: "read=true",
        success: function(resp) {
            log("set read status", resp);
        },
        error: function(resp) {
            log("Failed to set read flag", resp);

        }
    });                
}
