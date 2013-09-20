function initManageUsers() {	
    initNewUserForm();
    initShowBusinessBtn();
    initShowSettingBtn();
    initClearTextBtn();	
    initSettingPanel();
    initTooltip();
    initSearchBusiness();
    initSearchUser();
    initOrgSearch();
    initSelectAll();
    initRemoveUsers();
    initUploadUsers();
    initLoginAs();
}

function initChangeUserId() {
    $(".change-userid").click(function(e) {
        e.preventDefault();
        e.stopPropagation();
        if( confirm("Changing the userID will invalidate the user's password. They will need to reset their password. Are you sure you want to continue?") ) {
            var newId = prompt("Please enter a new UserID. This must be unique across all users in this system");
            if( newId ) {
                doUpdateUserId(newId);
            }
        }
    });
}

function initUploadUsers() {
    $(".showUploadCsvModal").click(function() {
        $.tinybox.show($("#modalUploadCsv"), {
            overlayClose: false,
            opacity: 0
        });                       
    });   

    $(".showMatchOrgsModal").click(function() {
        $.tinybox.show($("#modalMatchOrgsCsv"), {
            overlayClose: false,
            opacity: 0
        });                       
    });   

    $("#doUploadCsv").mupload({
        buttonText: "Upload spreadsheet",
        url: "users.csv",
        useJsonPut: false,
        oncomplete: function(data, name, href) {
            log("oncomplete:", data.result.data, name, href);
            $(".results .numUpdated").text(data.result.data.numUpdated);
            $(".results .numInserted").text(data.result.data.numInserted);
            $(".results .numUnmatched").text(data.result.data.unmatched.length);
            showUnmatched(data.result.data.unmatched);
            $(".results").show();
            alert("Upload completed. Please review any unmatched members below, or refresh the page to see the updated list of members");
        }
    });    
    
    var uploadForm = $("#doUploadCsv form");
    $("#allowInserts").click(function(e) {
        log("click", e.target);
        if( $(e.target).is(":checked")) {
            uploadForm.attr("action", "users.csv?insertMode=true");
        } else {
            uploadForm.attr("action", "users.csv");
        }        
    });    
}

function showUnmatched(unmatched) {
    var unmatchedTable = $(".results table");
    var tbody = unmatchedTable.find("tbody");
    tbody.html("");
    $.each(unmatched, function(i, row) {
        log("unmatched", row);
        var tr = $("<tr>");
        $.each(row, function(ii, field) {
            tr.append("<td>" + field + "</td>");
        });        
        tbody.append(tr);
    });
    unmatchedTable.show();
}     


function initSearchUser() {
    $("#userQuery").keyup(function () {
        typewatch(function () {
            log("do search");
            doSearch();
        }, 500);
    });    
}

function doSearch() {
    $.ajax({
        type: 'GET',
        url: window.location.pathname + "?q=" + $("#userQuery").attr("value"),
        success: function(data) {
            log("success", data)
            var $fragment = $(data).find("#userSearchResults");
            $("#userSearchResults").replaceWith($fragment);
        },
        error: function(resp) {
            alert("An error occured doing the user search. Please check your internet connection and try again");
        }
    });      
}

function doUpdateUserId(newUserId) {
    $.ajax({
        type: 'POST',
        url: window.location.pathname,
        dataType: "json",
        data: {
            newUserId: newUserId
        },
        success: function(data) {
            log("success", data)
            if( data.status ) {
                window.location.reload();
            } else {
                alert("Could not change the user's ID: " + data.messages);
            }
            
        },
        error: function(resp) {
            alert("An error occured attempting to update the userID. Please check your internet connection");
        }
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
            _checkboxes.filter("#" + setting).check(true);
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
				
            $.cookie("user-setting", setting.join("#"), {
                expires: 999
            });
        } else {
            $.cookie("user-setting", null);
        }
        _SettingContent.addClass("Hidden");
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

function initNewUserForm() {
    var modal = $("div.Modal.newUser");
    $("a.newUser").click(function(e) {
        e.preventDefault();
        e.stopPropagation();
        $.tinybox.show(modal, {
            overlayClose: false,
            opacity: 0
        });            
    });
    modal.find("form").forms({
        callback: function(resp) {
            log("done new user", resp);
            if( resp.nextHref ) {
                window.location.href = resp.nextHref;
            }
            $.tinybox.close();
        }
    });   
}

function initOrgSearch() {
    log("initOrgSearch", $("#orgTitle"));
    $("#orgTitle").on("focus click", function() {
        $("#orgSearchResults").show();
        log("show", $("#orgSearchResults")  );
    });
    $("#orgTitle").keyup(function () {
        typewatch(function () {
            log("do search");
            doOrgSearch();
        }, 500);
    });         
    $("div.groups").on("click", "a", function(e) {
        log("clicked", e.target);
        e.preventDefault();
        e.stopPropagation();        
        var orgLink = $(e.target);
        $("#orgId").val(orgLink.attr("href"));
        $("#orgTitle").val(orgLink.text());
        $("#orgSearchResults").hide();
        log("clicked", $("#orgId").val(), $("#orgTitle").val() );
    });
}

function doOrgSearch() {
    $.ajax({
        type: 'GET',
        url: window.location.pathname + "?orgSearch=" + $("#orgTitle").val(),
        success: function(data) {
            log("success", data)
                
            var $fragment = $(data).find("#orgSearchResults");
            $("#orgSearchResults").replaceWith($fragment);
            $fragment.show();
            log("frag", $fragment);
        },
        error: function(resp) {
            alert("An error occurred searching for organisations");
        }
    });      
}    

function initRemoveUsers() {
    $(".removeUsers").click(function(e) {
        var node = $(e.target);
        log("removeUsers", node, node.is(":checked"));
        var checkBoxes = node.closest(".Info").find("tbody td input:[name=toRemoveId]:checked");
        if( checkBoxes.length == 0 ) {
            alert("Please select the users you want to remove by clicking the checkboxs to the right");
        } else {
            if( confirm("Are you sure you want to remove " + checkBoxes.length + " users?") ) {
                doRemoveUsers(checkBoxes);
            }
        }
    });
}

function doRemoveUsers(checkBoxes) {
    $.ajax({
        type: 'POST',
        data: checkBoxes,
        dataType: "json",
        url: "",
        success: function(data) {
            log("success", data)
            if( data.status ) {
                doSearch();
                alert("Removed users ok");                
            } else {
                alert("There was a problem removing users. Please try again and contact the administrator if you still have problems");
            }
        },
        error: function(resp) {
            alert("An error occurred removing users. You might not have permission to do this");
        }
    });      
}

function initLoginAs() {
    $("body").on("click", "a.login-as", function(e) {
        e.preventDefault();
        var profileId = $(e.target).attr("href");
        showLoginAs(profileId);
    });
}

function showLoginAs(profileId) {
    var modal = $("#loginAsModal");
    modal.find("ul").html("<li>Please wait...</li>");
    $.tinybox.show(modal, {
        overlayClose: false,
        opacity: 0
    }); 
    $.ajax({
        type: 'GET',
        url: profileId + "?availWebsites",
        dataType: "json",
        success: function(response) {
            log("success", response.data);
            var newList = "";
            if( response.data.length > 0 ) {
            $.each( response.data, function(i, n) {
                newList += "<li><a target='_blank' href='" + profileId + "?loginTo=" + n + "'>" + n + "</a></li>";
            });
            } else {
                newList += "<li>The user does not have access to any websites. Check the user's group memberships, and that those groups have been added to the right websites</li>";
            }
            modal.find("ul")
                .empty()
                .html(newList);
        },
        error: function(resp) {
            alert("An error occured loading websites. Please try again");
        }
    });       
}