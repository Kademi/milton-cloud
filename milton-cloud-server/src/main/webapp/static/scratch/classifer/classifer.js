(function($) {
$(function() {
	var $controllers = $('.controller-wrapper > .controller'),
		$contents =  $('.content-wrapper > .content'),
		handlerEvent = function(e) {			
			var $this = $(this).addClass('active'),
				index = $(this).index() + 1;
			
			$controllers.not($this).removeClass('active');
			$contents
				.filter('.active')
					.removeClass('active')
				.end()
				.eq(index)
					.addClass('active');
		},
		is_touch_screen = (/iphone|ipad|ipod|android|blackberry|mini|windows\sce|palm|windows\sphone|iemobile|mobile/i.test(window.navigator.userAgent.toLowerCase()));
		
	$contents
		.addClass('hide')
		.eq(0)
		.addClass('active');
		
	if(is_touch_screen) {
		$controllers.click(handlerEvent);
	} else {
		$controllers.mouseover(handlerEvent);
	}
});


}(jQuery));