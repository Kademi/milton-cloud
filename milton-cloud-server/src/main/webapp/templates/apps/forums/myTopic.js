function initMyTopic() {
    log("initMyTopic");
    $("#postQuestion form").forms({
        callback: function(resp) {
            log("done post", resp);
            window.location.reload();
        }
    });
}