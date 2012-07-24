
var selectedForum;

function initManageForum() {
    initTopicController();
    initForumDialog();
    addOrderForumNTopic();
    addForumButton();
    addTopicButton();
    eventForModal();
    initWebsiteDropdown();
}

function initWebsiteDropdown() {
    $(".DropdownWrapper").click(function(e) {
        var t = $(e.target);
        log("click", t);
        t.closest("div.DropdownControl").find(".DropdownContent").toggle(300);
    });
}

function initTopicController() {
	
    // Bind event for Delete button
    $("body").on("click", "a.DeleteTopic", function(e) {
        e.preventDefault();
	confirmDeleteTopic($(this).closest("li"));
    });
	
    // Bind event for Edit button
    $("body").on("click", "a.RenameTopic", function(e) {
        e.preventDefault();
		
        var _selectedTopic = $(this).parent().parent();
		
        showModal("Topic", "Rename", {
            name: _selectedTopic.find("> span").html(),
            topic: _selectedTopic.attr("data-topic"),
            forum: _selectedTopic.parent().attr("data-forum")
        }, function() {
            renameTopic(_selectedTopic);
        });
    });
}

function addOrderForumNTopic() {
    $("div.Forum").each(function(i) {
        $(this).attr("data-forum", i)
        .find("ul")
        .attr("data-forum", i)
        .find("> li")
        .each(function(idx) {
            $(this).attr("data-topic", idx);
        });
    });
}

function initForumDialog() {
    var tempDialog = $("#dialogForum").html();
    $("div.Forum").each(function() {
        $(this).find("header div").append(tempDialog);
    });
	
    // Bind event for header of forum - show dialog
    $("body").on("click", "div.Forum header > div.ShowDialog", function(e) {
        e.preventDefault();
		
        var _dialog = $(this).find("div.Dialog");
		
        $("div.Dialog").not(_dialog).addClass("Hidden");
		
        if(_dialog.hasClass("Hidden")) {
            _dialog.removeClass("Hidden");
        } else {
            _dialog.addClass("Hidden");
        }
    });
	
    // Bind event for Delete forum
    $("body").on("click", "a.DeleteForum", function(e) {
        e.preventDefault();
        confirmDeleteForum(e);
    });
	
    // Bind event for Edit forum
    $("body").on("click", "a.RenameForum", function(e) {
        e.preventDefault();
		
        var _selectedForum = $(this).parents("div.Forum");
		
        showModal("Forum", "Rename", {
            name: $(this).parent().parent().find("> span").html(),
            forum: _selectedForum.attr("data-forum")
        }, function() {
            renameForum(_selectedForum);
        });
    });
}

function addForumButton() {
    $("body").on("click", "button.AddForum", function(e) {
        e.preventDefault();
        showModal("Forum", "Add", null, function() {
            addForum();
        });
    });
}

function addTopicButton() {
    $("body").on("click", "button.AddTopic", function(e) {
        e.preventDefault();
        log("add topic onclick");
        selectedForum = $(e.target).closest("div.Forum");
        var forumLink = selectedForum.find("header div.ShowDialog > a");
        var forumHref = forumLink.attr("href");        
        log("selectedForum", selectedForum);
        showModal("Topic", "Add", {
            forum: $(this).parent().parent().attr("data-forum")
        }, function() {
            addTopic(forumHref);
        });
    });
}

function showModal(name, type, data, callback) {
    var _modal = $("#modalForum");
	
    _modal.find("header h3").html(name + " Details")    
    _modal.find("div.ModalContent label[for=name]").html("Enter " + name.toLowerCase() + " name")
    _modal.find("div.ModalContent input[name=name]").attr("placeholder", name + " name").val("")
    
    _modal.find("button").html(type).attr("rel", name);
    
    _modal.find("button").unbind();
    _modal.find("button").click(callback);
			
    if(data) {			
        if(data.name) {
            _modal.find("div.ModalContent input[name=name]").val(data.name);
        }
				
        if(data.forum) {
            _modal.attr("data-forum", data.forum);
        }
		
        if(data.topic) {
            _modal.attr("data-topic", data.topic);
        }
    }
	
    $.tinybox.show(_modal, {
        overlayClose: false,
        opacity: 0
    });
}

function maxOrderForum() {
    var _order = [];
    $("div.Forum").each(function() {
        _order.push($(this).attr("data-forum"));
    });
	
    _order.sort().reverse();
	
    return (parseInt(_order[0]) + 1);
}

function maxOrderTopic(obj) {
    var _order = [];
    obj.find("ul.TopicList li").each(function() {
        _order.push($(this).attr("data-topic"));
    });
	
    _order.sort().reverse();
	
    return (parseInt(_order[0]) + 1);
}

function eventForModal() {
    var _modal = $("#modalForum");
	
    // Function to close the modal
    var resetControllers = function() {
        _modal.attr({
            "data-forum": "",
            "data-topic": ""
        });
    };
	
    // Bind close function to Close button
    _modal.find("a.Close").click(function(e) {
        resetControllers();
        e.preventDefault();
    });	
}

function addForum() {
    var modal = $("#modalForum");
    var name = $("input[name=name]", modal).val();
    log("addForum", name);
    if( name == null || name.length == 0 ) {
        alert("Please enter a name for the new forum");
        return;
    }
    $.ajax({
        type: 'POST',
        url: window.location.href,
        dataType: "json",
        data: {
            newName: name
        },
        success: function(data) {
            log("response", data);
            if( !data.status ) {
                alert("Failed to add forum: " + data.mssages);
                return;
            }
            $.tinybox.close();
            
            var tempDialog = $("#dialogForum").html();
            $("#manageForum").append('\
                        <div class="Forum" data-forum="' + maxOrderForum() + '">\
                                <header class="ClearFix">\
                                        <div class="ShowDialog"><a href="' + data.nextHref + '">' + name + '</a>\
                                                ' + tempDialog + '\
                                        </div>\
                                </header>\
                                <div class="ContentForum ClearFix">\
                                        <h4>Available Topics</h4>\
                                        <ul class="TopicList" data-forum="' + maxOrderForum() + '"></ul>\
                                        <button class="SmallBtn Add AddTopic"><span>Add New Topic</span></button>\
                                </div>\
                        </div>\
                ');    
                
        },
        error: function(resp) {
            log("error", resp);
            alert("err");
        }
    });          
    
}

function addTopic(forumName) {
    var modal = $("#modalForum");
    var name = $("input[name=name]", modal).val();
    log("addTopic", name);
    if( name == null || name.length == 0 ) {
        alert("Please enter a name for the new topic");
        return;
    }
    
    $.ajax({
        type: 'POST',
        url: forumName,
        dataType: "json",
        data: {
            newName: name
        },
        success: function(data) {
            log("response", data);
            
            $.tinybox.close();
                        
            var topicsUl = $("ul.TopicList", selectedForum);
	    var topicLi = $("<li><span>" + name + "</span></li>");
            topicsUl.append(topicLi);
            var aside = $("<aside></aside>");
            topicLi.append(aside);
            var topicHref = forumName + "/" + data.nextHref;
            aside.append("<a title='Rename this topic' class='Edit RenameTopic' href='" + topicHref + "'><span class='Hidden'>Rename this topic</span></a> ");
            aside.append("<a title='Delete this topic' class='Delete DeleteTopic' href='" + topicHref + "'><span class='Hidden'>Delete this topic</span></a> ");            
        },
        error: function(resp) {
            log("error", resp);
            alert("err");
        }
    });          
                
}

function updateForum() {
    $("div.Forum")
    .filter("[data-forum=" + _modal.attr("data-forum") + "]")
    .find("header div.ShowDialog span")
    .html(_name);
    
}

function updateTopic() {
    $("div.Forum")
    .filter("[data-forum=" + _modal.attr("data-forum") + "]")
    .find("ul.TopicList li")
    .filter("[data-topic=" + _modal.attr("data-topic") + "]")
    .find("> span")
    .html(_name);    
}

function confirmDeleteForum(e) {
    var forum = $(e.target).closest("div.Forum");
    var forumLink = forum.find("header div.ShowDialog > a");
    var href = forumLink.attr("href");
    var name = forumLink.text();
    confirmDelete(href, name, function() {
        forum.remove();
    });            
}

function confirmDeleteTopic(topicLi) {
    var href = topicLi.find("a.Delete").attr("href");
    var name = topicLi.find("> span").text();
    log("Cofirm delete topic", href, name);
    confirmDelete(href, name, function() {
        topicLi.remove();
    });        
}

function renameForum(forumDiv) {    
    var modal = $("#modalForum");
    var title = $("input[name=name]", modal).val();
    var forumHref = forumDiv.find("header div.ShowDialog > a").attr("href");
    log("renameForum", forumHref, forumDiv, forumDiv.find("header div.ShowDialog > a"));
    var data = new Object();
    data["milton:title"] = title;
    $.ajax({
        type: 'POST',
        url: forumHref + "/_DAV/PROPPATCH",
        dataType: "json",
        data: data,
        success: function(data) {
            log("response", data);
            forumDiv.find("header div.ShowDialog > a").text(title);
            $.tinybox.close();
        },
        error: function(resp) {
            log("error", resp);
            alert("err");
        }
    });    
}

function renameTopic(topicLi) {    
    var modal = $("#modalForum");
    var title = $("input[name=name]", modal).val();
    var topicHref = topicLi.find("a.RenameTopic").attr("href");
    log("renameTopic", topicHref);
    var data = new Object();
    data["milton:title"] = title;
    var url = topicHref;
    if( !url.endsWith("/")) {
        url += "/";
    }
    url += "_DAV/PROPPATCH";
    $.ajax({
        type: 'POST',
        url: url,
        dataType: "json",
        data: data,
        success: function(data) {
            log("response", data);
            topicLi.find("> span").text(title);
            $.tinybox.close();
        },
        error: function(resp) {
            log("error", resp);
            alert("err");
        }
    });    
}