(function($) {

    $.fn.extend({
        leanModal: function(options) {

            var defaults = {
                top: 100,
                overlay: 0.5,
                closeButton: null
            }
            checkOverlayExists();

            options = $.extend(defaults, options);

            return this.each(function() {

                var o = options;

                $(this).click(function(e) {
                    e.preventDefault();
                    var modal_id = $(this).attr("href");
                    var modal = $(modal_id);
                    showModal(modal);
                });

            });
        }
    });

})(jQuery);

function checkOverlayExists(modal) {
    var parent = $("body");
    if (modal) {
        parent = modal.parent();
    }
    var overlay = parent.find(".lean-overlay");
    if (overlay.length > 0) {
        return overlay;
    }
    overlay = $("<div class='lean-overlay'></div>");
    overlay.css({
        "position": "fixed",
        "z-index": "100",
        "top": "0px",
        "left": "0px",
        "height": "100%",
        "width": "100%",
        "background": "#000",
        "display": "none"
    });
    parent.append(overlay);
    overlay.click(function() {
        closeModals();
    });
    return overlay;
}

function initModal() {
    $("body").on("click", ".Modal a.Close", function(e) {
        log("close tinybox");
        closeModals();
        e.preventDefault();
    });
    $('a.ShowModal').leanModal();
}


var shownModal;

function showModal(modal) {
    shownModal = modal;
    var overlay = checkOverlayExists(modal);
    modal.find(".close").click(function() {
        closeModals();
    });
    $(document).on('keydown',modal, function(event) {
        if (event.which === 27)
            closeModals();
    });

    var modal_height = modal.outerHeight();
    var modal_width = modal.outerWidth();

    overlay.css({'display': 'block', opacity: 0});

    overlay.fadeTo(200, "0.5");

    modal.css({
        'display': 'block',
        'position': 'fixed',
        'opacity': 0,
        'z-index': 11000,
        'left': 50 + '%',
        'margin-left': -(modal_width / 2) + "px",
        'top': "100px"

    });

    modal.fadeTo(200, 1);
}

function closeModals() {
    $(".lean-overlay").fadeOut(200);
    if (shownModal) {
        shownModal.css({'display': 'none'});
    }
}