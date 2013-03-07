/**
 *  theme.js - functions which complement the theme
 */
$(function() {
    // Stop the login form from disappearing
    $('.banner .dropdown-menu input, .banner .dropdown-menu label, .banner .login button').click(function(e) {
        e.stopPropagation();
    });
    // init the login form
    $(".banner .login").user({
        
        });    
    
    log("initTheme: run page init functions", pageInitFunctions.length);
    $.each(pageInitFunctions, function(i, f) {
        log("run function" + i);
        pageInitFunctions[i]();
        log("done run function" , i);
        
    });
    $("table.table-tappy tbody td").click(function(e) {
        var target = $(e.target);
        if( target.is("a")) {
            return;
        }
        var td = target.closest("td");                    
        var href = td.find("a").attr("href");
        log("click", td, href);
        window.location.href = href;
    });

});

/**
 * Provided by each theme to integrate modals
 */
var lastOpenedModal;
function showModal(modal) {
    log("showModal");
    lastOpenedModal = modal;
    log("showModal", "lastOpenedModal", lastOpenedModal);
    modal.modal();
}
function closeModals() {
    log("closeModals", $(".modal"));
    if( lastOpenedModal ) {
        lastOpenedModal.modal('hide');
    }
}