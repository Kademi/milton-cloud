function initManageEmail() {
	stripList();
	initController();
	initList();
	initSortableButton();
	checkCookie();
}

function checkCookie() {
	var _sort_type = $.cookie("email-sort-type");
	if(_sort_type) {
		_sort_type = _sort_type.split("#");
		var _type = _sort_type[0];
		var _asc = _sort_type[1] === "asc"?true:false;
		sortBy(_type, _asc);
		
		switch(_type) {
		case 'date':
			$("a.SortByDate").attr("rel", _asc?"desc":"asc");
			break;
		case 'name':
			$("a.SortByName").attr("rel", _asc?"desc":"asc");
			break;
		case 'status':
			$("a.SortByStatus").attr("rel", _asc?"desc":"asc");
			break;
		};
	}
}

function stripList() {	
	$("#manageEmail .Content ul li").removeClass("Odd").filter(":odd").addClass("Odd");
}

function initController() {

	//Bind event for Delete email
	$("body").on("click", "a.DeleteEmail", function(e) {
		e.preventDefault();
		$(this).parent().parent().parent().remove();
		stripList();
	});
}

function initList() {
	$("#manageEmail .Content ul li").each(function(i) {
		$(this).attr("rel", i);
	});
};

function initSortableButton() {
	// Bind event for Status sort button
	$("body").on("click", "a.SortByStatus", function(e) {
		e.preventDefault();
		
		var _this = $(this);
		var _rel = _this.attr("rel");
		
		if(_rel === "asc") {
			sortBy("status", true);			
			$.cookie("email-sort-type", "status#asc");
			_this.attr("rel", "desc");
		} else {			
			sortBy("status", false);
			$.cookie("email-sort-type", "status#desc");
			_this.attr("rel", "asc");
		}
	});
	
	// Bind event for Name sort button
	$("body").on("click", "a.SortByName", function(e) {
		e.preventDefault();
		
		var _this = $(this);
		var _rel = _this.attr("rel");
		
		if(_rel === "asc") {
			sortBy("name", true);			
			$.cookie("email-sort-type", "name#asc");
			_this.attr("rel", "desc");
		} else {			
			sortBy("name", false);
			$.cookie("email-sort-type", "name#desc");
			_this.attr("rel", "asc");
		}
	});
	
	// Bind event for Date sort button
	$("body").on("click", "a.SortByDate", function(e) {
		e.preventDefault();
		
		var _this = $(this);
		var _rel = _this.attr("rel");
		
		if(_rel === "asc") {
			sortBy("date", true);			
			$.cookie("email-sort-type", "date#asc");
			_this.attr("rel", "desc");
		} else {			
			sortBy("date", false);
			$.cookie("email-sort-type", "date#desc");
			_this.attr("rel", "asc");
		}
	});
}

function sortBy(type, asc) {
	var list = $("#manageEmail .Content ul li");
	var _list = {};
	var sortObject = function(obj) {		
		var sorted = {},
			array = [],
			key,
			l;
		
		for(key in obj) {
			if(obj.hasOwnProperty(key)) {
				array.push(key);
			}
		}
		
		array.sort();	
		if(!asc) {
			array.reverse();
		}
		
		for(key = 0, l = array.length; key < l; key++) {
			sorted[array[key]] = obj[array[key]];
		}
		return sorted;
	};
	
	switch(type) {
	case 'date':
		for(var i = 0, _item; _item = list[i]; i++) {
			_item = $(_item);
			var title = _item.find("span.Date").html();
			var rel = _item.attr("rel");
			_list[title + "#" + rel] = _item;
		}
		break;
	case 'name':
		for(var i = 0, _item; _item = list[i]; i++) {
			_item = $(_item);
			var title = _item.find("span.Name").html();
			var rel = _item.attr("rel");
			_list[title + "#" + rel] = _item;
		}
		break;
	case 'status':
		for(var i = 0, _item; _item = list[i]; i++) {
			_item = $(_item);
			var title = _item.find("span.Status").html();
			var rel = _item.attr("rel");
			_list[title + "#" + rel] = _item;
		}
		break;
	}
	
	_list = sortObject(_list);
	
	var _emailList = $("#manageEmail .Content ul");
	_emailList.html("");
	for(var i in _list) {
		_emailList.append(_list[i]);
	}
	
	stripList();
}

function initEditEmailPage() {
	initTabPanel();
	initGroupController();
	addGroupBtn();
	addOrderGroupList();
	eventForModal();
	initCheckbox();
	sendEmailBtn();
}

function addOrderGroupList() {
	var tempControl = $("#modalListController").html();
	$("#modalGroup ul.ListItem li").each(function(i) {
		$(this).attr("data-group", i).append(tempControl);
	});
}

function initGroupController() {
	var _tempController = $("#groupController").html();
	$("#manageEmail .TabContent .Content ul.GroupList li").append(_tempController);
	
	// Bind event for Delete button
	$('body').on('click', 'a.DeleteGroup', function(e) {
		e.preventDefault();
		$(this).parent().parent().remove();
	});
}

function addGroupBtn() {
	$('.AddGroup').on('click', function(e) {
		e.preventDefault();
		
		var _group = [];
		
		$("div.Recipient ul.GroupList li").each(function() {
			_group.push(this.getAttribute("data-group"));
		});
		
		showModal(_group);
	});
}

function showModal(group) {	
	var _modal = $("#modalGroup");
	
	_modal.find("input[type=checkbox]").check(false);
			
	if(group) {
		var _groupList = _modal.find("ul.ListItem li");
		
		for(var i = 0; i < group.length; i++) {
			_groupList
				.filter("[data-group=" + group[i] + "]")
				.find("input[type=checkbox]")
					.check(true);
		}
	}
	
	$.tinybox.show(_modal, {
		overlayClose: false,
		opacity: 0
	});
}

function eventForModal() {
	var _modal = $("#modalGroup");
	
	// Bind close function to Close button
	_modal.find("a.Close").click(function(e) {
		resetModalControl();
		e.preventDefault();
	});
	
	// Event for Add/Edit button
	_modal.find("button").click(function(e) {
		e.preventDefault();
		
		var _groupList = _modal.find("ul.ListItem li");
				
		// Check checkboxes			
		var _groups = [];			
		_groupList.each(function() {
			var _this = $(this);
			if(_this.find("input[type=checkbox]").is(":checked")) {
				_groups.push({
					id: _this.attr("data-group"),
					name: _this.find("> span").html()
				});
			}
		});
		
		// Dont choose any group
		if(_groups.length === 0) {
			alert("Please choose at least one group!");
			
		// Chose at least one group
		} else {
			var _groupStr = "";
			
			for(var i = 0; i < _groups.length; i++) {
				_groupStr += '\
					<li data-group="' + _groups[i].id + '">\
						<span>' + _groups[i].name + '</span>\
						<aside>\
							<a href="" title="Delete this group" class="Delete DeleteGroup"><span class="Hidden">Delete this group</span></a>\
						</aside>\
					</li>\
				';
			}
			
			$("div.Recipient ul.GroupList").html(_groupStr);
			
			$.tinybox.close();
		}
	});
}

function sendEmailBtn() {
	$('button.SendEmail').on('click', function(e) {
		e.preventDefault();
		
		$('div.Content')
			.filter('.Send')
				.addClass('Hidden')
			.end()
			.filter('.SendProgress')
				.removeClass('Hidden');
		
		progress();
	});	
}

var percent = 0;

function progress() {
	var timer = setInterval(function() {
		checkPercent();
	}, 500);
}

function checkPercent() {
	if(percent === 100) {
		$('div.Content')
			.filter('.SendProgress')
				.addClass('Hidden')
			.end()
			.filter('.Success')
				.removeClass('Hidden');
	} else {
		percent += 5;
		$('div.Percent').css('width', percent + '%');
	}
}