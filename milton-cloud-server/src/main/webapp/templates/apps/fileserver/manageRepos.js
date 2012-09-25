function initManageRepositories() {
    log("initManageRepositories");
    $("form.addRepo").forms({
        callback: function(resp) {
            window.location.reload();
            $.tinybox.close();
        }
    });
}
