var $content;
var selectedProgram = null;
var selectedCourse = null;
var selectModule = null;
var parentOrgHref;

function initManageModule(baseHref) {
    parentOrgHref = baseHref;
    $content = $("#manageModule").find("> div.Content");
		
    stripTable();
    initController();
    initTabPanel();
    initFormDetails();	
    initModuleList();
    initDropDown();
}


function stripTable() {
    $content
    .find("div.MainContent article")
    .removeClass("Even")
    .filter(":even")
    .addClass("Even");
}
	
// Insert button in list of modules
function initController() {
}
	
// Event for tab panel
function initTabPanel() {
    $("nav.TabNav a.Item").on("click", function(e) {
        log("initTabPanel:click", this);
        var href = $(this).attr("href");
        $(this)
        .addClass("Active")
        .siblings()
        .removeClass("Active");
			
        $content
        .addClass("Hidden")
        .filter("[rel=" + href + "]")
        .removeClass("Hidden");
					
        e.preventDefault();
    });		
		
    $("#manageModule").find("nav.TabNav a.Item").eq(0).trigger("click");
}
	
// Event for Add and Edit Certificate and Reward button in Module details panel
function initFormDetails() {
    var $tableDetail = $("#manageModule").find("form.Details table");
    var certTemp = $("#certTemp").clone().removeAttr("id");
    var rewardTemp = $("#rewardTemp").clone().removeAttr("id");
    var btnAddNoText = function(name) {
        return '<button class="SmallBtn Add NoText" title="Add ' + name.toLowerCase() + '"><span>Add</span></button></div>';
    };
    var updatePanel = function(name) {
        var selector = "td[rel=" + name.toLowerCase() + "] div." + name;
        $tableDetail.find(selector).each(function(i) {
            var idx = i + 1;
            var $this = $(this);
					
            if(idx === 1) {
                $this.addClass("First");
            }
						
            $this.find("button.Add").remove();
					
            $this.find("select, label, input, button").each(function() {
                var $self = $(this);
                var _id = $self.attr("id") || null;
                var _for = $self.attr("for") || null;
                var _name = $self.attr("name") || null;
							
                if(_id) {
                    $self.attr("id", _id.replace(/\d+/g, "") + idx);
                }
                if(_for) {
                    $self.attr("for", _for.replace(/\d+/g, "") + idx);
                }
                if(_name) {
                    $self.attr("name", _name.replace(/\d+/g, "") + idx);
                }
            });
					
            if($this.is(":last-child")) {
                $this.append(btnAddNoText(name));
            }
        });
    };
		
    // For Certificate
    $("#addCert").on("click", function(e) {
        $(this).addClass("Hidden").parent().append(certTemp.clone());				
        updatePanel("Certificate");
        e.preventDefault();
    });
		
    $tableDetail.on("click", "div.Certificate button.Delete", function(e) {
        $(this).parent().remove();
			
        if($tableDetail.find("div.Certificate")[0]) {
            updatePanel("Certificate");
        } else {
            $("#addCert").removeClass("Hidden");
        }
    });
		
    $tableDetail.on("click", "div.Certificate button.Add", function(e) {
        $(this).parents("td").append(certTemp.clone());
        updatePanel("Certificate");
    });
		
    // For reward
    $("#addRewards").on("click", function(e) {
        $(this).addClass("Hidden").parent().append(rewardTemp.clone());				
        updatePanel("Reward");
        e.preventDefault();
    });
		
    $tableDetail.on("click", "div.Reward button.Delete", function(e) {
        $(this).parent().remove();
			
        if($tableDetail.find("div.Reward")[0]) {
            updatePanel("Reward");
        } else {
            $("#addRewards").removeClass("Hidden");
        }
    });
		
    $tableDetail.on("click", "div.Reward button.Add", function(e) {
        $(this).parents("td").append(rewardTemp.clone());
        updatePanel("Reward");
    });
		
    // For email
    $("#email").on("click", function() {
        var $controls = $tableDetail.find("tr[rel=email]").find("small, div.EmailMessage");
        if($(this).is(":checked")) {
            $controls.removeClass("Hidden");
        } else {
            $controls.addClass("Hidden");
        }
    });
}
	
function initModuleList() {
    var $moduleWrapper = $content.filter("[rel=#list]").find("div.MainContent");
    // Dragable row
    $moduleWrapper.sortable({
        items: "article",
        sort: function() {
            $(this).removeClass( "ui-state-default" );
        },
        stop: function(event, ui) {
            stripTable();
        }
    });
		
    // Delete button
    $content.on("mousedown", "article a.Delete", function() {
        $(this).parents("article").remove();
        stripTable();
    });
		
    // Show Dialog button
    $content.on("click", "article a.Dialogue", function(e) {
        var $dialog = $(this).addClass("Active").closest("article").find("div.Dialog");
        log("click",$(this), $dialog);        
        $("article div.Dialog").not($dialog).addClass("Hidden");
        $("article div.a.Dialogue").not($(this)).removeClass("Active");
        if($dialog.hasClass("Hidden")) {
            $dialog.removeClass("Hidden");
        } else {
            $dialog.addClass("Hidden");
        }
        e.preventDefault();
    });
}
	
function initDropDown() {
    var $DropDown =  $("#manageModule").find("div.DropdownControl");
    var $ContentDropDown = $DropDown.find("div.Content");
    var $ProgramContent = $ContentDropDown.find("section[rel=program]");
    var $CourseContent = $ContentDropDown.find("section[rel=course]");
    var $ModuleContent = $ContentDropDown.find("section[rel=module]");
    updateLabel = function() {
        var str = selectedProgram.name;
        if(selectedCourse) {
            str += " / " + selectedCourse.title;
        }
        if(selectModule) {
            str += " / " + selectModule.title;
        }
        $DropDown.find("div.DropdownWrapper > span").html(str);
    };
		
    // Init Program					
    var url = request_url();
    log("load programs", url);
    $.getJSON(url, function(data) {
        log("got response", data);
        var programStr = "";
        for(var i = 1; i < data.length; i++) {
            programStr += "<a href='#' rel='" + data[i]["name"] + "'>" + data[i]["title"] + "</a>";
        }
        $ProgramContent.html(programStr);
        $CourseContent.html("");
        $ModuleContent.html("");
    });
		
    // Add event for item of Program list
    $ProgramContent.on("click", "a", function(e) {	
        var $this = $(this);
        selectedCourse = null;
        selectModule = null;
			
        if(!$this.hasClass("Active")) {
            $this.addClass("Active").siblings().removeClass("Active");
						
            selectedProgram = {
                name: $this.attr("rel"),
                title: $this.html()
            };
				
            updateLabel();
				
            $ModuleContent.html("");
            $CourseContent.html("").addClass("Loading");
				
            $.getJSON(request_url(), function(data) {
                var courseStr = "";
                for(var i = 1; i < data.length; i++) {
                    courseStr += "<a href='#' rel='" + data[i]["name"] + "'>" + data[i]["title"] + "</a>";
                }
                $CourseContent.html(courseStr).removeClass("Loading");
            });
        }
			
        e.preventDefault();
    });
		
    // Add event for item of Course list
    $CourseContent.on("click", "a", function(e) {				
        var $this = $(this);
			
        if(!$this.hasClass("Active")) {
            $this
            .addClass("Active")
            .siblings()
            .removeClass("Active");
						
            $ModuleContent.html("").addClass("Loading");
				
            selectedCourse = {
                name: $this.attr("rel"),
                title: $this.html()
            };
				
            updateLabel();
				
            $.getJSON(request_url(), function(data) {
                var moduleStr = "";
                for(var i = 1; i < data.length; i++) {
                    moduleStr += "<a href='" + data[i]["href"] + "manage' rel='" + data[i]["name"] + "'>" + data[i]["title"] + "</a>";
                }
                $ModuleContent.html(moduleStr).removeClass("Loading");
            });
        }
			
        e.preventDefault();
    });
		
    // Add event for item of Module list
    $ModuleContent.on("click", "a", function(e) {				
        var $this = $(this);
			
        if(!$this.hasClass("Active")) {
            $this.addClass("Active").siblings().removeClass("Active");
				
            selectModule  = {
                name: $this.attr("rel"),
                title: $this.html()
            };	
            $(".DropdownContent").hide(600);
            updateLabel();
        }				
        //e.preventDefault();
    });
}

function request_url() {
    var str = "";
    if(selectedProgram) {
        str += selectedProgram.name + "/";
    }
    if(selectedCourse) {
        str += selectedCourse.name + "/trunk/"; // TODO: branches
    }
    var s = parentOrgHref + "/" + str + "_DAV/PROPFIND?fields=href,name,milton:title";
    log("request_url", s);
    return s;
}
