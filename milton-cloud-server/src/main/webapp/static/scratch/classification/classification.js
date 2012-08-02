(function($) {
$(function() {
	var $controllers = $('.controller-wrapper > div[class^=controller-]'),
		$contents =  $('.content-wrapper > div[class^=content-]'),
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
		
	if(is_touch_screen) {
		$controllers.click(handlerEvent);
	} else {
		$controllers.mouseover(handlerEvent);
	}
});


}(jQuery));