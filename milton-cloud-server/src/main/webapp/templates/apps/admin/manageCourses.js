var selectedCourse;
var branch = "trunk"; // todo: make dynamic when we implement branching

function initManageCourse(programsUrl) {
    initCourseController();
    initModuleController();
    dragAndDropItem();
    orderCourseModuleItem();	
    eventCourseItem();
    funcModal();
    addCourseButton();
    addModuleButton();	
    addProgramButton();
    initProgramList(programsUrl);
    initSave();
}

function initCourseController() {
    var _courseList = $("div.Course > ul");
    var tempController = $("#courseController").clone().removeAttr("id");
    var	tempDialog = $("#courseDialog").clone().removeAttr("id");
	
    // Add controllers to Course item list	
    _courseList.find("> li")
    .find("div:first")
    .append(tempController)
    .append(tempDialog)
	
    // Event for Dialogue button
    $("body").on("click", "div.Course a.DialogueCourse", function(e) {
        var _div = $(this).parent().parent();
        var _dialog = _div.find("div.Dialog");
        _courseList.find("div.Dialog").not(_dialog).addClass("Hidden");
        if(_dialog.hasClass("Hidden")) {
            _dialog.removeClass("Hidden");
        } else {
            _dialog.addClass("Hidden");
        }
        e.preventDefault();
    });
	
    // Event for Delete button in Dialog
    $("body").on("click", "div.Course a.DeleteCourse", function(e) {
        var _selectedCourse = $(this).parent().parent().parent();
        $("#modulePanel")
        .find("div.Module")
        .filter("[data-course=" + _selectedCourse.attr("data-course") + "]")
        .remove();
		
        _selectedCourse.remove();
        e.preventDefault();
    });
	
    // Event for Edit button in Dialog	
    $("body").on("click", "div.Course a.EditCourse", function(e) {
        var _selectedCourse = $(this).parent().parent().parent();
        showModal("Course", "Edit", {
            name: _selectedCourse.find("div:first > span").html(),
            notes: _selectedCourse.attr("data-author-notes"),
            course: _selectedCourse.attr("data-course")
        });
        $(this).parent().addClass("Hidden");
        e.preventDefault();
    });
}


function initModuleController() {
    var _moduleList = $("div.Module ul");
    var tempController = $("#moduleController").clone().removeAttr("id");
    var	tempDialog = $("#moduleDialog").clone().removeAttr("id");
	
    // Add controllers to Module item list
    _moduleList.find("> li")
    .append(tempController)
    .append(tempDialog)
    .filter(".Splitter")
    .find("a.EditModule, a.AddSplitter")
    .remove();
		
    // Event for Dialogue button
    $("body").on("click", "div.Module a.DialogueModule", function(e) {
        var _li = $(this).parent().parent();
        var _dialog = _li.find("div.Dialog");
        _moduleList.find("div.Dialog").not(_dialog).addClass("Hidden");
        if(_dialog.hasClass("Hidden")) {
            _dialog.removeClass("Hidden");
        } else {
            _dialog.addClass("Hidden");
        }
        e.preventDefault();
    });
		
    // Event for Delete button in Dialog
    $("body").on("click", "div.Module a.DeleteModule", function(e) {
        $(this).parent().parent().remove();
        e.preventDefault();
    });
	
    // Event for Edit button in Dialog	
    $("body").on("click", "div.Module a.EditModule", function(e) {
        var _selectedCourse = $(this).parent().parent();
        showModal("Module", "Edit", {
            name: _selectedCourse.find("span").html(),
            notes: _selectedCourse.attr("data-author-notes"),
            module: _selectedCourse.attr("data-module")
        });
        $(this).parent().addClass("Hidden");
        e.preventDefault();
    });
	
    // Event for Edit button in Dialog	
    $("body").on("click", "div.Module a.AddSplitter", function(e) {
        var _selectedCourse = $(this).parent().parent();
        _selectedCourse.after('\
			<li class="Splitter" style="">\
				<span>Level Splitter</span> \
				<aside>\
					<a title="Move up or down" class="Move" href=""><span class="Hidden">Move up or down</span></a> \
					<a title="Show dialogue menu" class="Dialogue DialogueModule" href=""><span class="Hidden">Show dialogue menu</span></a> \
				</aside>\
				<div class="Dialog Hidden">\
					<a class="Delete DeleteModule" href="">Delete this module</a>\
				</div>\
			</li>'
            );
        $(this).parent().addClass("Hidden");
        e.preventDefault();
    });
}


function initProgramController() {
    var _programList = $("ul.Program");
    var _tempController = $("#programController").clone().removeAttr("id");
	
    _programList
    .find("li")
    .append(_tempController);
			
    // Event for Delete button
    $("body").on("click", "div.Program a.DeleteProgram", function(e) {
        $(this).parent().parent().remove();
        e.preventDefault();
    });
	
    // Event for Edit button
    $("body").on("click", "div.Program a.EditProgram", function(e) {
        var _selectedCourse = $(this).parent().parent();
        showModal("Program", "Edit", {
            name: _selectedCourse.find("a").html(),
            notes: _selectedCourse.attr("data-author-notes"),
            program: _selectedCourse.attr("data-program")
        });
        e.preventDefault();
    });
}

function dragAndDropItem() {
    $("div.Course ul").sortable({
        items: "> li",
        sort: function() {
            $(this).removeClass( "ui-state-default" );
        }
    });
	
    $("div.Module ul").sortable({
        items: "> li",
        sort: function() {
            $(this).removeClass( "ui-state-default" );
        }
    });
}

function orderCourseModuleItem() {
    $("div.Course > ul > li").each(function(i) {
        $(this).attr({
            "data-course": i + 1
        }).find("div.Module")
        .attr("data-course", i + 1)
        .find("ul > li")
        .not(".Splitter")
        .each(function(idx) {
            $(this).attr("data-module", idx + 1);
        });
    });
}

function eventCourseItem() {
    $("body").on("click", "div.Course > ul > li", function(e) {
        var _selectedItem = $(this)/*.parent().parent()*/;
        selectedCourse = _selectedItem;
		
        $("div.Course li").not(_selectedItem).removeClass("Active");
		
        if(_selectedItem.hasClass("Active")) {
        // do nothing
        } else {
            _selectedItem.addClass("Active");
            showModuleList(_selectedItem);
        }
        e.preventDefault();
    });
    $("div.Course > ul > li:first").trigger("click");
}

function showModuleList(course) {
    var _modulePanel = $("#modulePanel");
    var _currentModuleList = _modulePanel.find("div.Module").css("display","");
    var _currentCourse = _currentModuleList.attr("data-course");
    _currentModuleList.detach();
    $("div.Course > ul > li")
    .filter("[data-course=" + _currentCourse + "]")
    .append(_currentModuleList);
			
    _modulePanel.append(
        course
        .find("div.Module")
        .css("display", "block")
        );
}

function showModal(name, type, data) {
    var _modal = $("#modal");
	
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
    .find("div.ModalContent textarea[name=note]")
    .val("")
    .end()
    .find("button")
    .html(type)
    .attr("rel", name);
			
    if(data) {			
        if(data.name) {
            _modal.find("div.ModalContent input[name=name]").val(data.name);
        }
		
        if(data.notes) {
            _modal.find("div.ModalContent textarea[name=note]").val(data.notes);
        }
				
        if(data.course) {
            _modal.attr("data-course", data.course);
        }
		
        if(data.module) {
            _modal.attr("data-module", data.module);
        }
		
        if(data.program) {
            _modal.attr("data-program", data.program);
        }
    }
	
    $.tinybox.show(_modal, {
        overlayClose: false,
        opacity: 0
    });
}

function maxOrderCourse() {
    var _order = [];
    $("div.Course > ul > li").each(function() {
        _order.push($(this).attr("data-course"));
    });
	
    _order.sort().reverse();
	
    return (parseInt(_order[0]) + 1);
}

function maxOrderModule() {
    var _order = [];
    $("#modulePanel div.Module ul li").each(function() {
        _order.push($(this).attr("data-course"));
    });
	
    _order.sort().reverse();
	
    return (parseInt(_order[0]) + 1);
}

function maxOrderProgram() {
    var _order = [];
    $("ul.Program li").each(function() {
        _order.push($(this).attr("data-program"));
    });
	
    _order.sort().reverse();
	
    return (parseInt(_order[0]) + 1);
}

function funcModal() {
    var _modal = $("#modal");
	
    // Function to close the modal
    var resetControllers = function() {
        _modal.attr({
            "data-module": "",
            "data-course": "",
            "data-program": ""
        });
    };
	
    // Bind close function to Close button
    _modal.find("a.Close").click(function(e) {
        resetControllers();
        e.preventDefault();
    });
	
    // Event for Add/Edit button
    _modal.find("button").click(function() {
        var _this = $(this);
        var _rel = _this.attr("rel");
        var _name = _modal.find("input[name=name]").val();
        var _note = _modal.find("textarea[name=note]").val();
		
        // Check name textbox is blank or not
        if(_name.replace(/^|$/g,"") !== "") {
			
            // If button is Add
            if(_this.html() === "Add") {
                switch(_rel) {
					
                    // If is adding Course
                    case "Course":
                        $("div.Course > ul").append(
                            $('<li data-course="' + maxOrderCourse() + '" data-author-notes="' + _note + '">\
							<div class="ClearFix">\
								<span class="title">' + _name + '</span>\
							</div>\
							<div class="Module ClearFix" data-course="' + maxOrderCourse() + '">\
								<ul></ul>\
								<button title="Add module" class="SmallBtn Add NoText AddModule"><span>Add</span></button>\
							</div>\
						</li>')
                            .find("> div:first")
                            .append(
                                $("#courseController")
                                .clone()
                                .removeAttr("id")
                                )
                            .append(
                                $("#courseDialog")
                                .clone()
                                .removeAttr("id")
                                )
                            .end()
                            .find("div.Module ul").sortable({
                                items: "> li",
                                sort: function() {
                                    $(this).removeClass( "ui-state-default" );
                                }
                            })
                            .end()
								
                            );
                        break;
					
                    // If is adding Module
                    case "Module":
                        $("#modulePanel div.Module ul").append(
                            $('<li data-module="' + maxOrderModule() + '" data-author-notes="' + _note + '">\
								<span class="title">' + _name + '</span>\
						</li>')
                            .append(
                                $("#moduleController")
                                .clone()
                                .removeAttr("id")
                                )
                            .append(
                                $("#moduleDialog")
                                .clone()
                                .removeAttr("id")
                                )
                            );
                        break;
					
                    // If is adding Program
                    case "Program":
                        createFolder(_name); // set _note
                        $("ul.Program").append($('\
						<li data-program="' + maxOrderProgram() + '" data-author-notes="' + _note + '">\
							<a href="#">' + _name + '</a>\
						</li>')
                            .append(
                                $("#programController")
                                .clone()
                                .removeAttr("id")
                                )
                            );
                        break;
                    default:
                //do nothing
                }
				
            // If button is Edit
            } else {
                switch(_rel) {
                    // If is edting Course
                    case "Course":
                        $("div.Course > ul li")
                        .filter("[data-course=" + _modal.attr("data-course") + "]")
                        .attr("data-author-notes", _note)
                        .find("> div:first span").html(_name);
                        break;
					
                    // If is edting Module
                    case "Module":
                        $("#modulePanel div.Module ul li")
                        .filter("[data-module=" + _modal.attr("data-module") + "]")
                        .attr("data-author-notes", _note)
                        .find("span").html(_name);
                        break;
					
                    // If is edting Program
                    case "Program":
                        $("ul.Program li")
                        .filter("[data-program=" + _modal.attr("data-program") + "]")
                        .attr("data-author-notes", _note)
                        .find("> a").html(_name);
                        break;
                    default:
                //do nothing
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

function initProgramList(programsUrl) {
    var _programList = $("div.DropdownContent ul.Program").html("");
    $.getJSON( programsUrl + "_DAV/PROPFIND?fields=href,name,milton:title,milton:notes&depth=1" , function(data) {
        var _programStr = '';
        for(var i = 1, _this; _this = data[i]; i++) {
            _this = $(_this);
            var _author_note = _this.attr("notes") || ""
            _programStr += '\
				<li data-program="' + (i + 1) + '" data-author-notes="' +_author_note + '">\
					<a href="' + _this.attr("href")  + 'manageCourses">' + _this.attr("title") + '</a>\
				</li>\
			';
        }
		log("initProgramList", _programList, _programStr);
        _programList.html(_programStr);		
        initProgramController();
    });
}

function addCourseButton() {
    $("button.AddCourse").click(function(e) {
        showModal("Course", "Add");
        e.preventDefault();
    });
}

function addModuleButton() {
    $("body").on("click", "button.AddModule", function(e) {
        showModal("Module", "Add");
        e.preventDefault();
    });
}

function addProgramButton() {
    $("body").on("click", "button.AddProgram", function(e) {
        showModal("Program", "Add");
        e.preventDefault();
    });
}

function initSave() {
    $("#publish").click(function() {
        saveModules();
    });
}

function saveModules() {
    log("Save", $(".Module li"), selectedCourse);
    var level = 1;
    var order = 0;
    var courseHref = selectedCourse.attr("data-href");
    $(".Module li").each(function(i, node) {
        var $node = $(node);
        if( $node.hasClass("Splitter")) {
            level++;
            log("found splitter", level);
        } else {
            order++;
            var title = $($("span", $node)[0]).text();
            var notes = $node.attr("data-author-notes");
            var name = $node.attr("id");
            log("module", name, title, notes, order, level);        
            saveModule(courseHref, name, title, notes, order, level);
        }
    });
}

function saveModule(courseHref, name, title, notes, order, level) {    
    var data = new Object();
    data["milton:title"] = title;
    data["miltonx:notes"] = notes;
    data["miltonx:order"] = order;
    data["miltonx:level"] = level;
    var href = courseHref + "/" + branch + "/" + name;
    log("saveModule", name, title);
    $.ajax({
        type: 'POST',
        url: href + "/_DAV/PROPPATCH",
        data: data,
        dataType: "json",
        success: function(resp) {
            log("saved module", name);
        },
        error: function(resp) {
            alert("Sorry couldnt update module: " + name);
        }
    });        
}