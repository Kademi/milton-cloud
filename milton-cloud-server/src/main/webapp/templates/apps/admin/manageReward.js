function initEditReward() {
	initPlaceHolderForDateTime();
}

function initPlaceHolderForDateTime() {
	$("input[type=text].DateTime").each(function() {
		var _this = $(this);
		
		var today = new Date();
		var curr_date = today.getDate();
		curr_date = curr_date > 9?curr_date:"0" + curr_date;
		var curr_month = today.getMonth() + 1;
		curr_month = curr_month > 9?curr_month:"0" + curr_month;
		var curr_year = today.getFullYear();
		
		_this
			.attr("placeholder", curr_month + "/" + curr_date + "/" + curr_year)
			.datepicker()
			.after(
				$('<img src="images/icon_calendar.png" alt="" width="21" height="24" class="DateTimeIcon" />')
					.bind("click", function(e) {
						e.preventDefault();
						_this.trigger("focus");
					})
			)
	});
}

var list;

function initManageReward() {
	stripList();
	initController();
	initDialog();
	initSortableButton();
	initList();
	checkCookie();
}

function checkCookie() {
	var _sort_type = $.cookie("reward-sort-type");
	if(_sort_type) {
		_sort_type = _sort_type.split("#");
		var _type = _sort_type[0];
		var _asc = _sort_type[1] === "asc"?true:false;
		sortBy(_type, _asc);
		
		if(_type === "status") {
			$("a.SortByStatus").attr("rel", _asc?"desc":"asc");
		} else {
			$("a.SortByTitle").attr("rel", _asc?"desc":"asc");
		}
	}
}

function initList() {
	list = $("#manageReward .Content ul li").each(function(i) {
		$(this).attr("rel", i);
	}).clone();
};

function stripList() {	
	$("#manageReward .Content ul li").removeClass("Odd").filter(":odd").addClass("Odd");
}

function initController() {
	var _tempController = $("#rewardControllers").html();
	$("#manageReward .Content ul li").append(_tempController);
	
	//Bind event for Delete reward
	$("body").on("click", "a.DeleteReward", function(e) {
		e.preventDefault();
		$(this).parent().parent().remove();
		stripList();
	});
}

function initDialog() {
	var _tempDialog = $("#rewardForum").html();
	$("#manageReward .Content ul li div").append(_tempDialog);
	
	// Bind event for ShowDialog button
	$("body").on("click", "a.ShowDialog", function(e) {
		e.preventDefault();
		
		var _dialog = $(this).parent().find("div.Dialog");
		
		$("div.Dialog").not(_dialog).addClass("Hidden");
		
		if(_dialog.hasClass("Hidden")) {
			_dialog.removeClass("Hidden");
		} else {
			_dialog.addClass("Hidden");
		}
	});
	
	// Bind event for ActiveReward button
	$("body").on("click", "div.Dialog a", function(e) {
		e.preventDefault();
		
		var _this = $(this);
		var _status = _this.html();
		
		_this
			.parent()
				.addClass("Hidden")
			.parent()
				.attr("class", _status)
			.find("> a")
				.html(_status);
	});
}

function sortBy(type, asc) {
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
	
	if(type === "title") {
		for(var i = 0, _item; _item = list[i]; i++) {
			_item = $(_item);
			var title = _item.find("> span").html();
			var rel = _item.attr("rel");
			_list[title + "#" + rel] = _item;
		}
	} else {
		for(var i = 0, _item; _item = list[i]; i++) {
			_item = $(_item);
			var status = _item.find("> div > a.ShowDialog").html();
			var rel = _item.attr("rel");
			_list[status + "#" + rel] = _item;
		}
	}
	
	_list = sortObject(_list);
	
	var _rewardList = $("#manageReward .Content ul");
	_rewardList.html("");
	for(var i in _list) {
		_rewardList.append(_list[i]);
	}
	
	stripList();
}

function initSortableButton() {
	// Bind event for Status sort button
	$("body").on("click", "a.SortByStatus", function(e) {
		e.preventDefault();
		
		var _this = $(this);
		var _rel = _this.attr("rel");
		
		if(_rel === "asc") {
			sortBy("status", true);			
			$.cookie("reward-sort-type", "status#asc");
			_this.attr("rel", "desc");
		} else {			
			sortBy("status", false);
			$.cookie("reward-sort-type", "status#desc");
			_this.attr("rel", "asc");
		}
	});
	
	// Bind event for Title sort button
	$("body").on("click", "a.SortByTitle", function(e) {
		e.preventDefault();
		
		var _this = $(this);
		var _rel = _this.attr("rel");
		
		if(_rel === "asc") {
			sortBy("title", true);			
			$.cookie("reward-sort-type", "title#asc");
			_this.attr("rel", "desc");
		} else {			
			sortBy("title", false);
			$.cookie("reward-sort-type", "title#desc");
			_this.attr("rel", "asc");
		}
	});
}