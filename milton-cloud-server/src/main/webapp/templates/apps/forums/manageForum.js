function initManageForum() {
    initTopicController();
    initForumDialog();
    addOrderForumNTopic();
    addForumButton();
    addTopicButton();
    eventForModal();
}

function initTopicController() {
    var tempController = $("#topicController").html();
    $("div.Forum div.ContentForum ul li").append(tempController);
	
    // Bind event for Delete button
    $("body").on("click", "a.DeleteTopic", function(e) {
        e.preventDefault();
		
        $(this).parent().parent().remove();
    });
	
    // Bind event for Edit button
    $("body").on("click", "a.RenameTopic", function(e) {
        e.preventDefault();
		
        var _selectedTopic = $(this).parent().parent();
		
        showModal("Topic", "Rename", {
            name: _selectedTopic.find("> span").html(),
            topic: _selectedTopic.attr("data-topic"),
            forum: _selectedTopic.parent().attr("data-forum")
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
		
        $(this).parents("div.Forum").remove();
    });
	
    // Bind event for Edit forum
    $("body").on("click", "a.RenameForum", function(e) {
        e.preventDefault();
		
        var _selectedForum = $(this).parents("div.Forum");
		
        showModal("Forum", "Rename", {
            name: $(this).parent().parent().find("> span").html(),
            forum: _selectedForum.attr("data-forum")
        });
    });
}

function addForumButton() {
    $("body").on("click", "button.AddForum", function(e) {
        e.preventDefault();
        showModal("Forum", "Add");
    });
}

function addTopicButton() {
    $("body").on("click", "button.AddTopic", function(e) {
        e.preventDefault();
        showModal("Topic", "Add", {
            forum: $(this).parent().parent().attr("data-forum")
        });
    });
}

function showModal(name, type, data) {
    var _modal = $("#modalForum");
	
    _modal
    .find("header h3")
    .html(name + " Details")
    .end()
    .find("div.ModalContent label[for=name]")
    .html("Enter " + name.toLowerCase() + " name")
    .end()
    .find("div.ModalContent input[name=name]")
    .attr("placeholder", name + " name")
    .val("")
    .end()
    .find("button")
    .html(type)
    .attr("rel", name);
			
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
	
    // Event for Add/Edit button
    _modal.find("button").click(function(e) {
        var _this = $(this);
        var _rel = _this.attr("rel");
        var _name = _modal.find("input[name=name]").val();
		
        // Check name textbox is blank or not
        if(_name.replace(/^|$/g,"") !== "") {
			
            // If button is Add
            if(_this.html() === "Add") {
                switch(_rel) {
					
                    // If is adding Forum
                    case "Forum":
                        var tempDialog = $("#dialogForum").html();
                        $("#manageForum").append('\
						<div class="Forum" data-forum="' + maxOrderForum() + '">\
							<header class="ClearFix">\
								<div class="ShowDialog"><span>' + _name + '</span>\
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
                        break;
				
                    // If is adding Topic
                    case "Topic":
                        var tempController = $("#topicController").html();
                        var _forumId = _modal.attr("data-forum");
                        var _ulList = $("div.Forum[data-forum=" + _forumId + "] ul.TopicList");
					
                        _ulList.append('\
						<li data-topic="' + maxOrderTopic(_ulList) + '">\
							<span>' + _name + '</span>\
							' + tempController + '\
						</li>\
					');
                        break;
                    default:
                        break;
                }			
			
            // If button is Edit
            } else {
                switch(_rel) {
					
                    // If is editing Forum
                    case "Forum":
                        $("div.Forum")
                        .filter("[data-forum=" + _modal.attr("data-forum") + "]")
                        .find("header div.ShowDialog span")
                        .html(_name);
                        break;
				
                    // If is editing Topic
                    case "Topic":
                        $("div.Forum")
                        .filter("[data-forum=" + _modal.attr("data-forum") + "]")
                        .find("ul.TopicList li")
                        .filter("[data-topic=" + _modal.attr("data-topic") + "]")
                        .find("> span")
                        .html(_name);
                        break;
                    default:
                        break;
                }			
				
            }
			
            resetControllers();
            $.tinybox.close();
			
        // If name textbox is blank, alert the message
        } else {
            alert("Please enter " + _rel.toLowerCase() + " name!");
        }
    });
}