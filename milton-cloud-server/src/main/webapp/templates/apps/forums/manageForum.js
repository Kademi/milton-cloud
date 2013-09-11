
var selectedForum;

function initManageForum() {
    initTopicController();
    initForumDialog();
    addOrderForumNTopic();
    addForumButton();
    addTopicButton();
    eventForModal();
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
		
        showForumModal("Topic", "Rename", {
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
    // Bind event for header of forum - show dialog
    $("body").on("click", "div.Forum header div.ShowDialog", function(e) {
        e.preventDefault();	
        var dialog = $(this).closest(".Forum").find("div.Dialog");		
        dialog.toggle();
        log("show", dialog);        
    });
	
    // Bind event for Delete forum
    $("body").on("click", "a.DeleteForum", function(e) {
        e.preventDefault();
        confirmDeleteForum(e);
    });
	
    // Bind event for Edit forum
    $("body").on("click", "a.EditForum", function(e) {
        e.preventDefault();
	
        var selectedForum = $(this).closest("div.Forum");
        showModal( selectedForum.find(".Modal") );
    });
}

function addForumButton() {
    $("body").on("click", "button.AddForum", function(e) {
        e.preventDefault();
        showForumModal("Forum", "Add", null, function() {
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
        showForumModal("Topic", "Add", {
            forum: $(this).parent().parent().attr("data-forum")
        }, function() {
            addTopic(forumHref);
        });
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
    var cont = modal.find(".ModalContent");
    resetValidation(cont);
    if( !checkRequiredFields(cont)) {
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
            var newDiv = $('\
                        <div class="Forum" data-forum="' + maxOrderForum() + '">\
                                <header class="ClearFix">\
                                        <div class="ShowDialog"><a href="' + data.nextHref + '">' + name + '</a>\
                                                ' + tempDialog + '\
                                        </div>\
                                </header>\
                        </div>\
                ');
            $("#manageForum").append(newDiv);    
            var offset = newDiv.offset();
            $(document).scrollTop(offset.top);
            newDiv.css("opacity", 0);
            newDiv.animate({opacity: 1}, 2000);
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