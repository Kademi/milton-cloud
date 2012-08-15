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
		
		$('.modal-table thead th:last-child').addClass('last');
		$('.modal-table tbody tr')
			.eq(0)
				.addClass('first')
			.end()
			.eq(1)
				.addClass('second')
			.end()
			.eq(2)
				.addClass('third');
    });

}(jQuery));