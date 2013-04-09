/**
 *  theme.js - functions which complement the theme
 */
$(function() {
    // just a nice little function to get classes
    $.fn.classes = function(f) {
        var c = [];
        $.each(this, function(i, v) {
            var _ = v.className.split(/\s+/);
            for (var j in _)
                '' === _[j] || c.push(_[j]);
        });
        c = $.unique(c);
        if ("function" === typeof f)
            for (var j in c)
                f(c[j]);
        return c;
    };

    // Stop the login form from disappearing
    $('.banner .dropdown-menu input, .banner .dropdown-menu label, .banner .login button').click(function(e) {
        e.stopPropagation();
    });
    // init the login form
    $(".login").user({
    });

    // setup text box resizing
    log("textarea resize");
    jQuery('textarea.autoresize').autogrow();

    log("initTheme: run page init functions", pageInitFunctions.length);
    $.each(pageInitFunctions, function(i, f) {
        log("run function" + i);
        pageInitFunctions[i]();
        log("done run function", i);

    });
    $("table.table-tappy tbody td").click(function(e) {
        var target = $(e.target);
        if (target.is("a")) {
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
    if (lastOpenedModal) {
        lastOpenedModal.modal('hide');
    }
}