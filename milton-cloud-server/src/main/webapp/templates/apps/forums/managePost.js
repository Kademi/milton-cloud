function initManagePost() {
	initController();
}

function initController() {
	var _temp = $('#controller').html();
	
	$('div.Post > footer > div').append(_temp);
	
	// Bind event for Delete button
	$('body').on('click', '.DeletePost', function(e) {
		e.preventDefault();
		
		$(this).parent().parent().parent().remove();
	});
}