function initManageGroup() {
    initGroupDialog();
    addOrderGroup();
    addOrderProgramList();
    addOrderPermissionList();
    addGroupButton();
    eventForModal();
    initPermissionCheckboxes();
    initRegoMode();
    initRemoveRole();
    initAddRole();
    initCopyMembers();
    initOptInGroups();
}

var currentGroupDiv;


function showPermissionModal(source) {
    log("showPermissionModal", source);
    $("ul.appliesTo input").removeAttr("checked");
    currentGroupDiv = $(source).closest("div.Group");
    var modal = $(".Modal.roles");
    log("modal", modal);
    $.tinybox.show(modal, {
        overlayClose: false,
        opacity: 0
    });
}


function initAddRole() {
    $("body").on("click", "ul.appliesTo input", function(e) {
        var inp = $(e.target);
        var appliesTo = inp.closest("ul.appliesTo");
        appliesTo.find("select").hide();
        appliesTo.find("input[type=radio]:checked").closest("li").find("select").show();
    });

    $("body").on("click", "div.roles button.Add", function(e) {
        e.preventDefault();
        var roleLi = $(e.target).closest("li");
        var appliesTo = $("ul.appliesTo");
        var appliesToType = appliesTo.find("input:checked");

        if (appliesToType.length == 0) {
            alert("Please select what the role applies to");
            return;
        }
        var appliesToTypeVal = appliesToType.val();
        var select = appliesToType.closest("li").find("select");
        var appliesToVal = ""; // if need to select a target then this has its value
        if (select.length > 0) {
            appliesToVal = select.val();
            if (appliesToVal.length == 0) {
                alert("Please select a target for the role");
                return;
            }
            appliesToText = select.find("option:checked").text();
        }

        log("add role", appliesToTypeVal, appliesToVal);
        log("currentGroupDiv", currentGroupDiv);
        var groupHref = currentGroupDiv.find("header > div > span").text();
        var roleName = roleLi.find("> span").text();
        addRoleToGroup(groupHref, roleName, appliesToTypeVal, appliesToVal, function(resp) {
            var newLi = $("<li></li>");
            if (appliesToVal.length == 0) {
                appliesToVal = "their own organisation";
            }
            var newSpan = $("<span>").text(roleName + ", on " + appliesToText);
            newLi.append(newSpan);
            var newDelete = $("<a href=''>Delete</a>");
            newDelete.attr("href", resp.nextHref);
            newLi.append(newDelete);
            currentGroupDiv.find("ul.PermissionList").append(newLi);
        });
    });
}

function addRoleToGroup(groupHref, roleName, appliesToType, appliesTo, callback) {
    log("addRoleToGroup", groupHref, roleName, appliesToType, appliesTo);
    try {
        $.ajax({
            type: 'POST',
            url: groupHref,
            dataType: "json",
            data: {
                appliesToType: appliesToType,
                role: roleName,
                appliesTo: appliesTo
            },
            success: function(data) {
                log("success", data);
                if (data.status) {
                    log("saved ok", data);
                    callback(data);
                    alert("Added role");
                } else {
                    var msg = data.messages + "\n";
                    if (data.fieldMessages) {
                        $.each(data.fieldMessages, function(i, n) {
                            msg += "\n" + n.message;
                        });
                    }
                    log("error msg", msg);
                    alert("Couldnt save the new role: " + msg);
                }
            },
            error: function(resp) {
                log("error", resp);
                alert("Error, couldnt add role");
            }
        });
    } catch (e) {
        log("exception in createJob", e);
    }
}

function initRemoveRole() {
    $("body").on("click", "ul.PermissionList li a", function(e) {
        log("click", this);
        e.preventDefault();
        e.stopPropagation();
        if (confirm("Are you sure you want to remove this role?")) {
            var a = $(e.target);
            log("do it", a);
            var href = a.attr("href");
            deleteFile(href, function() {
                a.closest("li").remove();
            });
        }
    });
}

function initPermissionCheckboxes() {
    $("body").on("click", ".roles input[type=checkbox]", function(e) {
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
                if (isRecip) {
                    permissionList.append("<li>" + roleName + "</li>");
                } else {
                    log("remove", permissionList.find("li:contains('" + roleName + "')"));
                    permissionList.find("li:contains('" + roleName + "')").remove();
                }
            },
            error: function(resp) {
                log("error", resp);
                alert("err");
            }
        });
    } catch (e) {
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

    $("body").on("click", "a.RenameGroup", function(e) {
        e.preventDefault();

        var _selectedForum = $(this).parents("div.Group");

        showGroupModal("Group", "Rename group", "Rename", {
            name: $(this).parent().parent().find("> span").html(),
            group: _selectedForum.attr("data-group")
        });
    });

    $("body").on("click", "a.ViewGroupMembers", function(e) {
        e.preventDefault();
        var groupName = $(this).parent().parent().find("> span").text();
        window.location.href = groupName + "/members";
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

            if (_for) {
                _this.attr("for", _for + _randomId);
            }

            if (_name) {
                _this.attr("name", _name + _randomId);
            }

            if (_id) {
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

function showGroupModal(name, title, type, data) {
    resetModalControl();

    var _modal = $("#modalGroup");
    log("showGroupModal", _modal);

    _modal.find("header h3").html(title);
    _modal.find("button").html(type === "Save" ? "Save changes" : type).attr("rel", name);

    _modal
            .find("tr[rel=Group], tr[rel=Program], tr[rel=Permission]").addClass("Hidden")
            .end()
            .find("tr[rel=" + name + "]").removeClass("Hidden");

    if (data) {
        if (data.name) {
            _modal.find("div.ModalContent input[name=name]").val(data.name);
        }

        if (data.group) {
            _modal.attr("data-group", data.group);
        }

        if (data.program) {
            var _programList = _modal.find("tr[rel=Program] ul.ListItem li");
            var _programs = data.program;

            for (var i = 0; i < _programs.length; i++) {
                _programList
                        .filter("[data-program=" + _programs[i] + "]")
                        .find("input[type=checkbox]")
                        .check(true);
            }
        }

        if (data.permission) {
            var _permissionList = _modal.find("tr[rel=Permission] ul.ListItem li");
            var _permission = data.permission;

            for (var i = 0; i < _permission.length; i++) {
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
        log("addGroupButton: click");
        showGroupModal("Group", "Add new group", "Add");
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

        switch (_rel) {
            // If is Group
            case "Group":
                var _name = _modal.find("input[name=name]").val();
                // Check name textbox is blank or not
                if (_name.trim() !== "") {
                    if (_type === "Add") { // If is adding Group		
                        createFolder(_name, null, function(name, resp) {
                            window.location.reload();
                        });
                    } else { // If is editing Group
                        var groupDiv = $("div.Group").filter("[data-group=" + _modal.attr("data-group") + "]");
                        var groupNameSpan = groupDiv.find("header div.ShowDialog span");
                        var src = groupNameSpan.text();
                        src = $.URLEncode(src);
                        var dest = _name;
                        //dest = $.URLEncode(dest);
                        dest = window.location.pathname + dest;
                        move(src, dest, function() {
                            groupNameSpan.text(_name);
                        });
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
                    if (_this.find("input[type=checkbox]").is(":checked")) {
                        _programs.push({
                            id: _this.attr("data-program"),
                            name: _this.find("> span").html()
                        });
                    }
                });

                // Dont choose any programs
                if (_programs.length === 0) {
                    alert("Please choose at least one program!");

                    // Chose at least one program
                } else {
                    var _programStr = "";

                    for (var i = 0; i < _programs.length; i++) {
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
                    if (_this.find("input[type=checkbox]").is(":checked")) {
                        _permissions.push({
                            id: _this.attr("data-permission"),
                            name: _this.find("> span").html()
                        });
                    }
                });

                // Dont choose any permission
                if (_permissions.length === 0) {
                    alert("Please choose at least one permission!");

                    // Chose at least one permission
                } else {
                    var _permissionStr = "";

                    for (var i = 0; i < _permissions.length; i++) {
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
    $("body").on("click", "a.regoMode", function(e) {
        log("click", e.target);
        e.preventDefault();
        e.stopPropagation();
        var target = $(e.target);
        var href = target.closest("div.Group").find("header div > span").text();
        href = $.URLEncode(href) + "/";
        var modal = $("#modalRegoMode");
        modal.load(href + " #modalRegoCont", function() {
            initOptInGroups();
            $("#modalRegoMode form.general").forms({
                callback: function(resp) {
                    log("done", resp);
                    $.tinybox.close();
                    //window.location.reload();
                    $("div.content").load(window.location.pathname + " div.content > *", function() {

                    });
                }
            });

            $("#modalRegoMode form.fields").forms({
                confirmMessage: null,
                callback: function(resp, form) {
                    if (resp.status) {
                        var key = form.find("input[name=addFieldName]").val();
                        var val = form.find("input[name=addFieldValue]").val();
                        newLi = $("<li><h4>" + key + "</h4>" + val + "<a href='" + key + "' class='removeField'>Delete</a></li>");
                        $("ul.fields").append(newLi);
                        $('.addField').toggle();
                        form.find("input").val("");
                    } else {
                        alert("Couldnt add the field. Please check your input and try again");
                    }
                }
            });

            modal.on("click","a.removeField", function(e) {
                log("click removeField");
                e.preventDefault();
                var target = $(e.target);
                var li = target.closest("li");
                var fieldName = target.attr("href");
                var groupHref = li.closest("form").attr("action");
                removeField(groupHref, fieldName, li);
            });

            log("done forms", modal);

            $.tinybox.show(modal, {
                overlayClose: false,
                opacity: 0
            });
            log("showed modal");
        });

    });
}

function removeField(groupHref, fieldName, li) {
    try {
        $.ajax({
            type: 'POST',
            url: groupHref,
            data: {
                removeFieldName: fieldName
            },
            success: function(data) {
                log("saved ok", data);
                li.remove();
            },
            error: function(resp) {
                log("error", resp);
                alert("There was an error removing the field. Please check your internet connection");
            }
        });
    } catch (e) {
        log("exception in createJob", e);
    }
}

function setRegoMode(currentRegoModeLink, selectedRegoModeLink) {
    var val = selectedRegoModeLink.attr("rel");
    var text = selectedRegoModeLink.text();
    var data = "milton:regoMode=" + val;
    var href = currentRegoModeLink.closest("div.Group").find("header div > span").text();
    href = $.URLEncode(href) + "/";
    log("setRegoMode: val=", val, "text=", text, "data=", data, "href=", href);
    proppatch(href, data, function() {
        currentRegoModeLink.text(text);
    });
}

function initCopyMembers() {
    $("body").on("click", ".CopyMembers", function(e) {
        log("click", e.target);
        e.preventDefault();
        e.stopPropagation();

        var modal = $("#modalCopyMembers");
        var target = $(e.target);
        var href = target.closest("div.Group").find("header div > span").text();
        modal.find("span").text(href);
        href = $.URLEncode(href) + "/";
        modal.find("form").attr("action", href);

        $.tinybox.show(modal, {
            overlayClose: false,
            opacity: 0
        });
    });

    $("#modalCopyMembers form").forms({
        callback: function(resp) {
            log("done", resp);
            $.tinybox.close();
            alert("Copied members");
            window.location.reload();
        }
    });
}

function initOptInGroups() {
    $(".optins input[type=checkbox]").click(function(e) {
        updateOptIn($(e.target));
    }).each(function(i, n) {
        updateOptIn($(n));
    });
}

function updateOptIn(chk) {
    if (chk.is(":checked")) {
        chk.closest("li").addClass("checked");
    } else {
        chk.closest("li").removeClass("checked");
    }

}