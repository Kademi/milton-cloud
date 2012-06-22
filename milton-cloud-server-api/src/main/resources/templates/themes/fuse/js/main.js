(function($) {
    $(function() {
        $("a#login").click(function() {
            var $LoginPanel = $("div.LoginPanel");
            if($LoginPanel.hasClass("Hidden")) {
                $LoginPanel.removeClass("Hidden");
            } else {
                $LoginPanel.addClass("Hidden");
            }
            return false;
        });
    });
}(jQuery));