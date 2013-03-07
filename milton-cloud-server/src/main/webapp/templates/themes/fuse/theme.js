/** Start theme.js */


function initFuseTheme() {
    log("initFuseTheme");
    
    initActiveNav("body");
    initTabPanel();
    initMenuEffects ();
    initDropDownControl();
    initHelp();
    initModal();
    initCheckbox();
            
    log("finished initFuseTheme");
}


function showModal(modal) {
    $.tinybox.show(modal, {
        overlayClose: false,
        opacity: 0
    });      
}

function closeModal() {
    $.tinybox.close();
}

function initModal() {
    $("body").on("click", ".Modal a.Close", function(e) {
        log("close tinybox");
        $.tinybox.close();
        e.preventDefault();
    });
    $('a.ShowModal').tinybox({
        overlayClose: false,
        opacity: 0
    });    
}


function initHelp () {
    $(".helpIcon").click(function(e) {
        $.tinybox.show("#modalHelp", {
            overlayClose: false,
            opacity: 0
        });
        e.preventDefault();
    });	
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

function initTabPanel() {
    var tab_container = $('.TabContainer');
    if(tab_container[0]) {
        var tab_content = tab_container.find('.TabContent');
        var tab_nav = tab_container.find('nav a');
        tab_content.addClass('Hidden');
		
        tab_nav.on('click', function(e) {
            e.preventDefault();
			
            var _this = $(this);
            var _this_content = tab_content.eq(_this.index());
			
            if(!_this.hasClass('Active')) {
                tab_nav.filter('.Active').removeClass('Active');
                _this.addClass('Active');
                tab_content.not(_this_content).addClass('Hidden');
                _this_content.removeClass('Hidden');
            }
        });
		
        tab_nav.eq(0).trigger('click');
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