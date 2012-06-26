function initManageWebsite() {
	initWebsiteControllers();
}

function initWebsiteControllers() {
	var temp = $("#websiteController").html();
	$("#manageWebsite table.Summary tr th").each(function() {
		$(this).html(temp);
	});
	
	$("body").on("click", "table.Summary .ViewWebsite", function(e) {
		e.preventDefault();
		
		window.location.href = $(this).parent().parent().attr("rel");
	});
}