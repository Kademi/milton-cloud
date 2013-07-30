function initManageCustomReports() {
    $("#manageCustomReports .Add").click(function() {
        showAddReport(this);
    });
    $("#manageCustomReports form.addReward").forms({
        callback: function(resp) {
            log("done");
            window.location.href = resp.nextHref;
        }
    });
    
}

function showAddReport(source) {
    var modal = $(source).parent().find(".Modal");
    $.tinybox.show(modal, {
        overlayClose: false,
        opacity: 0
    }); 
    return false;
}
