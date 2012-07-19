/** Start theme.js */


function initFuseTheme() {
    log("initFuseTheme");
    
    initTabPanel();
    initMenuEffects ();
    initDropDownControl();
    initCheckbox();
            
    log("finished initFuseTheme");
}



function initMenuEffects () {	
    //  JS FOR SIDEBAR MENU
    if($('.sidebar ')[0]) {
        $('.sidebar .menu ul li h4 a').click(function () {            
            var li = $(this).closest("li");
            var submenu = $("ul", li);
            changebg = $(this).find('a.Arrow');
            if (submenu.is(':visible')){
                submenu.slideUp(150);
                changebg.addClass("closed");
            }else{
                submenu.slideDown(200);	
                changebg.removeClass("closed");
            }
            return false;   
        });
    }	
}


function initDropDownControl() {
    // Functionality for DropDown Control
    $("div.DropdownControl a.Control").on("click", function(e) {
        var $content = $(this).parent().siblings().filter("div.DropdownContent");
        if($content.hasClass("Hidden")) {
            $content.removeClass("Hidden");
        } else {
            $content.addClass("Hidden");
        }
        e.preventDefault();
    });
	
    $("div.DropdownControl span.Label").on("click", function() {
        $(this).parent().find("a.Control").trigger("click");
    });
}

function initCheckbox() {
    $("input[type=checkbox].FuseChk").each(function() {
        var _this = $(this);
        var _label = _this.parent();
        var _checked = _this.is(":checked")?" Checked":"";
        var _disabled = _this.is(":disabled")?" Disabled":"";
		
        _label.addClass("FuseChkLabel" + _checked + _disabled);
    });
	
    $("body").on("click", "input[type=checkbox].FuseChk", function() {
        var _this = $(this);
        var _label = _this.parent();
        if(_this.is(":checked")) {
            _label.addClass("Checked");
        } else {
            _label.removeClass("Checked");
        }
    });
}

// Function check or uncheck for FuseChk checkbox
$.fn.check = function(yes_or_no) {
    var _this = $(this);
    if(_this.is(".FuseChk")) {
        _this
        .attr("checked", yes_or_no)
        if(yes_or_no) {
            _this.parent().addClass("Checked");
        } else {
            _this.parent().removeClass("Checked");
        }
			
    }
}


if(!String.prototype.trim) {
    String.prototype.trim = function() {
        return this.replace(/^\s+|\s+$/g,"");
    };
}

var typewatch = (function(){
    var timer = 0;
    return function(callback, ms){
        clearTimeout (timer);
        timer = setTimeout(callback, ms);
    }  
})();

/** End theme.js */