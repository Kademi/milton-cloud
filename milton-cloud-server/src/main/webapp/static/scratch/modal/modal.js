(function($) {
$(function() {
	$('div.linked-modal').each(function() {
		$(this).append('<a href="#" title="Close" class="close-modal">Close</a>')
	});
	
	$('a.anchor-modal').tinybox({
		opacity: 0
	});
	
	$('a.close-modal').click(function(e) {
		$.tinybox.close();
	});
});


}(jQuery));