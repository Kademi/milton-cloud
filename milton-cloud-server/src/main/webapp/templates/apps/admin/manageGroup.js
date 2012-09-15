function initManageGroup() {
    initGroupDialog();
    addOrderGroup();
    addOrderProgramList();
    addOrderPermissionList();
    addGroupButton();
    eventForModal();
    initPermissionCheckboxes();
    initRegoMode();
}

function initPermissionCheckboxes() {
    $(".roles input[type=checkbox]").click(function() {
        var $chk = $(this);
        log("checkbox click", $chk, $chk.is(":checked"));
        var isRecip = $chk.is(":checked");
        var groupName = $chk.closest("aside").attr("rel");
        var permissionList = $chk.closest(".ContentGroup").find(".PermissionList");
        setGroupRole(groupName, $chk.attr("name"), isRecip, permissionList);
    });    
}

function setGroupRole(groupName, roleName, isRecip, permissionList) {
    log("setGroupRole", groupName, roleName, isRecip);
    try {
        $.ajax({
            type: 'POST',
            url: window.location.href,
            data: {
                group: groupName,
                role: roleName,
                isRecip: isRecip
            },
            success: function(data) {
                log("saved ok", data);
                if( isRecip ) {
                    permissionList.append("<li>" + roleName + "</li>");
                } else {
                    log("remove", permissionList.find("li:contains('" + roleName + "')") );
                    permissionList.find("li:contains('" + roleName + "')").remove();
                }
            },
            error: function(resp) {
                log("error", resp);
                alert("err");
            }
        });          
    } catch(e) {
        log("exception in createJob", e);
    }       
}

function initGroupDialog() {
    var tempDialog = $("#dialogGroup").html();
    $("div.Group").each(function() {
        $(this).find("header div").append(tempDialog);
    });
	
    // Bind event for header of forum - show dialog
    $("body").on("click", "div.Group header > div.ShowDialog", function(e) {
        e.preventDefault();
		
        var _dialog = $(this).find("div.Dialog");
        _dialog.toggle();
    });
	
    // Bind event for Delete forum
    $("body").on("click", "a.DeleteGroup", function(e) {
        e.preventDefault();
        var target = $(e.target);
        var name = target.closest(".ShowDialog").find("> span").text();
        var href = $.URLEncode(name);
        log("delete", href);
        confirmDelete(href, name, function() {
            log("remove ", this);
            target.parents("div.Group").remove();
        });	        
    });
	
    // Bind event for Edit forum
    $("body").on("click", "a.RenameGroup", function(e) {
        e.preventDefault();
		
        var _selectedForum = $(this).parents("div.Group");
		
        showModal("Group", "Rename group", "Rename", {
            name: $(this).parent().parent().find("> span").html(),
            group: _selectedForum.attr("data-group")
        });
    });
}



function addOrderGroup() {
    $("div.Group").each(function(i) {
        $(this).attr("data-group", i);
    });
}

function addOrderProgramList() {
    var tempControl = $("#modalListController").html();
    $("#modalGroup tr[rel=Program] ul.ListItem li").each(function(i) {
        $(this)
        .attr("data-program", i)
        .append(tempControl)
        .find("label", "input")
        .each(function() {
            var _this = $(this);
            var _randomId = Math.round(Math.random() * 100000);
            var _for = _this.attr("for") || null;
            var _name = _this.attr("name") || null;
            var _id = _this.attr("id") || null;
						
            if(_for) {
                _this.attr("for", _for + _randomId);
            }
						
            if(_name) {
                _this.attr("name", _name + _randomId);
            }
						
            if(_id) {
                _this.attr("id", _id + _randomId);
            }
        });
    });
	
}
function addOrderPermissionList() {
    var tempControl = $("#modalListController").html();
    $("#modalGroup tr[rel=Permission] ul.ListItem li").each(function(i) {
        $(this).attr("data-permission", i).append(tempControl);
    });
}

function resetModalControl() {
    var _modal = $("#modalGroup");
	
    _modal.find("input[type=text]").val("");
	
    _modal.attr("data-group", "");
	
    _modal.find("input[type=checkbox]").check(false);
}

function showModal(name, title, type, data) {
    resetModalControl();
	
    var _modal = $("#modalGroup");
	
    _modal.find("header h3").html(title);
    _modal.find("button").html(type==="Save"?"Save changes":type).attr("rel", name);
			
    _modal
    .find("tr[rel=Group], tr[rel=Program], tr[rel=Permission]").addClass("Hidden")
    .end()
    .find("tr[rel=" + name + "]").removeClass("Hidden");
			
    if(data) {			
        if(data.name) {
            _modal.find("div.ModalContent input[name=name]").val(data.name);
        }
				
        if(data.group) {
            _modal.attr("data-group", data.group);
        }
		
        if(data.program) {
            var _programList = _modal.find("tr[rel=Program] ul.ListItem li");
            var _programs = data.program;
			
            for(var i = 0; i < _programs.length; i++) {
                _programList
                .filter("[data-program=" + _programs[i] + "]")
                .find("input[type=checkbox]")
                .check(true);
            }
        }
		
        if(data.permission) {
            var _permissionList = _modal.find("tr[rel=Permission] ul.ListItem li");
            var _permission = data.permission;
			
            for(var i = 0; i < _permission.length; i++) {
                _permissionList
                .filter("[data-permission=" + _permission[i] + "]")
                .find("input[type=checkbox]")
                .check(true);
            }
        }
    }
	
    $.tinybox.show(_modal, {
        overlayClose: false,
        opacity: 0
    });
}


function addGroupButton() {
    $("body").on("click", ".AddGroup", function(e) {
        e.preventDefault();
		
        showModal("Group", "Add new group", "Add");
    });
}



function showPermissionModal(source) {
    log("showPermissionModal", source);
    var modal = $(source).parent().parent().find(".Modal.roles");
    log("modal", modal);
    $.tinybox.show(modal, {
        overlayClose: false,
        opacity: 0
    });   
}

function maxOrderGroup() {
    var _order = [];
    $("div.Group").each(function() {
        _order.push($(this).attr("data-group"));
    });
	
    _order.sort().reverse();
	
    return (parseInt(_order[0]) + 1);
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
        log("Click add/edit group");
        var _this = $(this);
        var _rel = _this.attr("rel");
        var _type = _this.html();		
		
        switch(_rel) {
            // If is Group
            case "Group":
                var _name = _modal.find("input[name=name]").val();
                // Check name textbox is blank or not
                if(_name.trim() !== "") {
				
                    // If is adding Group		
                    if(_type === "Add") {
                        createFolder(_name, null, function(name, resp) {
                            window.location.reload();                        
                        });
					
                    // If is editing Group
                    } else {
                        $("div.Group")
                        .filter("[data-group=" + _modal.attr("data-group") + "]")
                        .find("header div.ShowDialog span")
                        .html(_name);
                    }
				
                    resetModalControl();
                    $.tinybox.close();
				
                // If name textbox is blank, alert the message
                } else {
                    alert("Please enter group name!");
                }
                break;
		
            // If is Program
            case "Program":
                var _programList = _modal.find("tr[rel=Program] ul.ListItem li");
			
                // Check checkboxes			
                var _programs = [];			
                _programList.each(function() {
                    var _this = $(this);
                    if(_this.find("input[type=checkbox]").is(":checked")) {
                        _programs.push({
                            id: _this.attr("data-program"),
                            name: _this.find("> span").html()
                        });
                    }
                });
			
                // Dont choose any programs
                if(_programs.length === 0) {
                    alert("Please choose at least one program!");
				
                // Chose at least one program
                } else {
                    var _programStr = "";
				
                    for(var i = 0; i < _programs.length; i++) {
                        var _randomId = Math.round(Math.random() * 100000);
                        _programStr += '\
						<li data-program="' + _programs[i].id + '">\
							<span>' + _programs[i].name + '</span>\
							<aside>\
								<label for="view_only' + _randomId + '" class="FuseChkLabel Checked">\
									<input type="checkbox" value="1" id="view_only' + _randomId + '" name="view_only' + _randomId + '" class="FuseChk" checked="checked" /> View only\
								</label>\
								<a href="" title="Delete this program" class="Delete DeleteProgram"><span class="Hidden">Delete this program</span></a>\
							</aside>\
						</li>\
					';
                    }
				
                    $("div.Group")
                    .filter("[data-group=" + _modal.attr("data-group") + "]")
                    .find("ul.ProgramList")
                    .html(_programStr);
				
                    resetModalControl();
                    $.tinybox.close();
                }
                break;
		
            // If is Permission
            case "Permission":
                var _permissionList = _modal.find("tr[rel=Permission] ul.ListItem li");
			
                // Check checkboxes			
                var _permissions = [];			
                _permissionList.each(function() {
                    var _this = $(this);
                    if(_this.find("input[type=checkbox]").is(":checked")) {
                        _permissions.push({
                            id: _this.attr("data-permission"),
                            name: _this.find("> span").html()
                        });
                    }
                });
			
                // Dont choose any permission
                if(_permissions.length === 0) {
                    alert("Please choose at least one permission!");
				
                // Chose at least one permission
                } else {
                    var _permissionStr = "";
				
                    for(var i = 0; i < _permissions.length; i++) {
                        _permissionStr += '\
						<li data-permission="' + _permissions[i].id + '">\
							<span>' + _permissions[i].name + '</span>\
							<aside>\
								<a href="" title="Delete this permission" class="Delete DeletePermission"><span class="Hidden">Delete this permission</span></a>\
							</aside>\
						</li>\
					';
                    }
				
                    $("div.Group")
                    .filter("[data-group=" + _modal.attr("data-group") + "]")
                    .find("ul.PermissionList")
                    .html(_permissionStr);
				
                    resetModalControl();
                    $.tinybox.close();
                }
                break;
			
            default:
                break;
        }
    });
}

function initRegoMode() {
    $("a.regoMode").click(function(e) {
        log("click", e.target);
        e.preventDefault();
        e.stopPropagation();        
        var target = $(e.target);
        var modal = $("#modalRegoMode");
        modal.find("a.Btn").unbind();
        modal.find("a.Btn").click(function(e) {
            var regoModeLink = $(e.target);
            setRegoMode(target, regoModeLink);
        });
        $.tinybox.show(modal, {
            overlayClose: false,
            opacity: 0
        });
    });
}

function setRegoMode(currentRegoModeLink, selectedRegoModeLink) {
    var val = selectedRegoModeLink.attr("rel");
    var text = selectedRegoModeLink.text();
    var data = "milton:regoMode=" + val;
    var href = currentRegoModeLink.closest("div.Group").find("header div > span").text();
    href = $.URLEncode(href);
    log("setRegoMode: val=", val, "text=", text, "data=", data, "href=", href);
    proppatch(href, data, function() {
        currentRegoModeLink.text(text);
    });
}