function initManageUser() {	
	initShowBusinessBtn();
	initShowSettingBtn();
	initClearTextBtn();	
	initSettingPanel();
	initCheckBox();
	initControl();
	initTooltip();
	initSearchBusiness();
}

function initCheckBox() {
	$("input.CheckBox[type=checkbox]").each(function() {
		var _self = $(this).wrap("<div class='CheckBoxWrapper'></div>"),
			_id = _self.attr("id"),
			_label = $("label[for=" + _id + "]"),
			_checked = _self.attr("checked"),
			_wrapper = _self.parent();
			
		if(_checked) {
			_wrapper.addClass("Checked");
		}
			
		_wrapper.append(_label).bind("click", function() {
			if(_wrapper.hasClass("Checked")) {
				_wrapper.removeClass("Checked");
				_self.attr("checked", false);
			} else {
				_wrapper.addClass("Checked");
				_self.attr("checked", true);
			}
		});
		
		_label.bind("click", function() {
			_wrapper.trigger("click");
		});
	});
}

// Event for ClearText button
function initClearTextBtn() {
	$("div.Info a.ClearText").bind("click", function(e) {
		$(this).parent().find("input").val("");
		e.preventDefault();
	});
}

// Show Setting Panel
function initShowSettingBtn() {
	$("div.Setting > a").bind("click", function(e) {
		var _settingContent = $(this).parent().find("div.SettingContent");
		if(_settingContent.hasClass("Hidden")) {
			_settingContent.removeClass("Hidden");
		} else {
			_settingContent.addClass("Hidden");
		}
		e.preventDefault();
	});
}

// Show search business panel
function initShowBusinessBtn() {
	$("div.Left a.Search").bind("click", function(e) {
		var _hiddenPanel = $(this).parent().find("div.Info");
		if(_hiddenPanel.hasClass("Hidden")) {
			_hiddenPanel.removeClass("Hidden");
		} else {
			_hiddenPanel.addClass("Hidden");
		}
		e.preventDefault();
	});
}

function initSettingPanel() {	
	// Check cookie for user settings
	var _SettingContent = $("div.SettingContent"),
		_userSetting = $.cookie("user-setting"),
		_checkboxes = _SettingContent.find("input[type=checkbox]"),
		_remember = $("#remember");
	if(_userSetting) {
		_remember.attr("checked", true);
		_checkboxes.not(_remember).attr("checked", false);
		_userSetting = _userSetting.split("#");
		_SettingContent.find("select").val(_userSetting[0]);
		for(var i = 1, setting; setting = _userSetting[i]; i++) {
			_checkboxes.filter("#" + setting).attr("checked", true);
		}
	}
	
	// Event for save change button
	$("#saveChange").bind("click", function(e) {
		if(_remember.is(":checked")) {
			var setting = [];
			setting.push(_SettingContent.find("select").val());
			_checkboxes
				.not(_remember)
				.each(function() {
					var _self = $(this);
					if(_self.is(":checked")) {
						setting.push(_self.val());
					}
				});
				
			$.cookie("user-setting", setting.join("#"), { expires: 999 });
		} else {
			$.cookie("user-setting", null);
		}
		_SettingContent.addClass("Hidden");
		e.preventDefault();
	});	
}

// Add controller for business list panel
function initControl() {
	var _tempControl = $("#tempController").clone().removeAttr("id");
	
	$("div.Info")
		.not("[rel]")
		.find("table.Summary tr").each(function() {
			$(this).find("td:last").html(_tempControl.clone());
		})
		.on("click", "a.ShowDialog", function(e) {
			var _this = $(this);
			var _dialog = _this.parent().find("div.Dialog");
			$("div.Dialog").not(_dialog).addClass("Hidden");
			$("a.ShowDialog").not(_this).removeClass("Active");
			if(_dialog.hasClass("Hidden")) {
				_dialog.removeClass("Hidden");
				_this.addClass("Active");
			} else {
				_dialog.addClass("Hidden");
				_this.removeClass("Active");
			}
			e.preventDefault();
		});
}

function initTooltip() {
    $("body").append('<div id="tooltipPanel" class="Hidden"></div>');
	var _tooltip = $("#tooltipPanel");
	$("*[tooltip]").on({
		mouseover: function() {
			var _self = $(this);
			_tooltip
				.removeClass("Hidden")
				.html(_self.attr("tooltip"))
				.css({
					left: _self.offset().left - _tooltip.innerWidth() + _self.width(),
					top: _self.offset().top + _self.height() + 5
				});
		},
		mouseout: function() {
			_tooltip.addClass("Hidden");
		}
	});
}

function initSearchBusiness() {
	var _container = $("#pullDown");
	var _content = _container.find("table.Summary tbody");
    var _input = _container.find("input[type=text]");
	
	_container.find("a.ClearText").unbind("click").bind("click", function(e) {
		_input.val("");
		_content.html("");
		e.preventDefault();
	});
	
	_input.bind("input", function() {
	   var _keyword = _input.val().toLowerCase();
	   var _urlRequest = "/users/_DAV/PROPFIND?fields=name,clyde:title,clyde:templateName,clyde:suburb,clyde:postcode,clyde:address,clyde:state&depth=5";
	   if(_keyword.replace(/^\s+|\s+$/g, "") != "") {
     	   $.getJSON(_urlRequest, function(datas) {
     	       var result = "";
     	       $(datas).each(function() {
     	           var _data = $(this),
     	               _title = _data.attr("title").toLowerCase();
     	           if(_data.is("[state]") && _title.indexOf(_keyword) != -1) {
     	              result += "<tr>";
     	              result += "<td>" + _data.attr("title") + "</td>";
     	              result += "<td>" + _data.attr("suburb") + "</td>";
     	              result += "<td>" + _data.attr("postcode") + "</td>";
     	              result += "<td>" + _data.attr("address") + "</td>";
     	              result += "<td>" + _data.attr("state") + "</td>";
     	              result += "</tr>";
     	           }
     	       });
     	       _content.html(result);
     	   });
	   } else {
	       _content.html("");
	   }
	});
}