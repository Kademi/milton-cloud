function saveApps(form) {
    log("saveApps", form);
    try {
        $.ajax({
            type: 'POST',
            url: window.location.pathname,
            data: form.serialize(),
            success: function(data) {
                alert("saved ok", data);
            },
            error: function(resp) {
                log("error", resp);
                alert("err");
            }
        });          
    } catch(e) {
        log("exception in saveapps", e);
    }
    return false;
}