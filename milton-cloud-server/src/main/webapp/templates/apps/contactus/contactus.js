function initContactUs() {
    $("form").forms({
        callback: function() {
            $("form.contactus").animate({opacity: 0}, 500, function() {
                $("form.contactus").hide();
                $(".thankyou").show(100);
            });
        }
    });
}
