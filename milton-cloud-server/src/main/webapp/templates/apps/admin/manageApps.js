$(function() {
    initApps();
});
function initApps() {
    log("initApps");
    $("td.CheckBoxWrapper input:checked").closest("tr").addClass("enabled");
    $("table.apps").on("change", ".CheckBoxWrapper input", function() {
        log("changed", this);
        $chk = $(this);
        if($chk.is(":checked")) {                        
            setEnabled($chk.val(), true, function() {
                $chk.closest("tr").addClass("enabled");
            });
        } else {                        
            setEnabled($chk.val(), false, function() {
                $chk.closest("tr").removeClass("enabled");
            });
        }
    });
    $("button.settings").click(function(e) {
        e.preventDefault();
        var modal = $("#settings_" + $(this).attr("rel"));
        log("show", $(this), $(this).attr("rel"), modal);
        $.tinybox.show(modal, {
            overlayClose: false,
            opacity: 0
        });
    });     
    $(".settings form").forms({
        callback: function(resp) {
            log("done save", resp);
            $.tinybox.close();
            window.location.reload();
        }
    });
    
}
function setEnabled(appId, isEnabled, success) {
    $.ajax({
        type: 'POST',
        url: window.location.pathname,
        dataType: "json",
        data: {
            appId: appId,
            enabled: isEnabled
        },
        success: function(data) {
            log("response", data);
            if( !data.status ) {
                alert("Failed to set status: " + data.mssages);
                return;
            }
            success(data);
        },
        error: function(resp) {
            log("error", resp);
            alert("err");
        }
    });                    
}