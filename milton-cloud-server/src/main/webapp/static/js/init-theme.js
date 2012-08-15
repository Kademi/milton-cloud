/** Start theme.js */

/**
 *  Editor support: Note that this relies on a global variable called toolbarSets
 *  
 *  A default is defined in toolbars.js. You should override that file in your
 *  application to get the toolbars you want
 */

// Templates should push a page init function into this array. It will then be run after outer template init functions
var pageInitFunctions = new Array();
// Templates should push theme css files into this array, so they will be included in the editor
var themeCssFiles = new Array();


Array.max = function( array ){
    return Math.max.apply( Math, array );
};

Array.min = function( array ){
    return Math.min.apply( Math, array );
};

function initTheme() {
    log("initTheme: init-theme.js");
    
    // the login box in header is normally for logging in from a public page. So
    // in this case we want to navigate to the user's dashboard
    $(".header .Login").user({
        afterLoginUrl: "/dashboard"
    });
    // the login form appears in content when the requested page requires a login
    // so in this case we do not give a post-login url, we will just refresh the current page
    $("#content .Login").user();
    
    jQuery('textarea.autoresize').autoResize();    
      
    initEdify();    
    initNav();
    initActiveNav(".initActive");
    initFontSwitching();
    initHelp();
    initModal();
    initTabPanel();
    initRotation();
    initPlaceholder();
    
         
    log("initTheme: run page init functions");
    for( i=0; i<pageInitFunctions.length; i++) {
        pageInitFunctions[i]();
    }
         
    log("finished init-theme");
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

function initNav() {    
    var bodyClasses = jQuery("body").attr("class");
    if(bodyClasses) {
        c = bodyClasses.split(" ");
        for( i=0; i<c.length; i++  ) {
            jQuery(".nav-" + c[i]).addClass("active");
        }    
    }    
}

function initFontSwitching() {    
    log("initFontSwitching");
    $(".ZoomOut").click(function() {            
        var currentFontSize = $('html').css('font-size');
        var currentFontSizeNum = parseFloat(currentFontSize, 10);
        var newFontSize = currentFontSizeNum*1.2;
        $('html').css('font-size', newFontSize);
        log("ZoomOut", newFontSize);
        return false;
    });			
    $(".ZoomIn").click(function() {            
        var currentFontSize = $('html').css('font-size');
        var currentFontSizeNum = parseFloat(currentFontSize, 10);
        var newFontSize = currentFontSizeNum/1.2;
        $('html').css('font-size', newFontSize);
        log("ZoomOut", newFontSize);
        return false;
    });		

    $(".ZoomReset").click(function() {            
        var newFontSize = "1em";
        $('html').css('font-size', newFontSize);
        log("ZoomReset", newFontSize);
        saveFontSizeInCookie(newFontSize);
        return false;
    });		
    var initialFontSize = getSavedFontSize();
    if( initialFontSize ) {
        log("set initial font size", initialFontSize)
        $('html').css('font-size', initialFontSize);
    }
    
			
//    $(".ZoomIn").click(function() {
//        log("zoomin");
//        if(defaultFont < 15) {
//            defaultFont += 1;
//            $content.css("font-size", defaultFont + "px");
//            $.cookie("font-size", defaultFont + "px", {
//                expires: 99999, 
//                path: "/"
//            });
//        }
//        return false;
//    });
//			
//    $(".ZoomReset").click(function() {
//        defaultFont = 12;
//        $content.css("font-size", defaultFont + "px");
//        $.cookie("font-size", "", {
//            expires: -1, 
//            path: "/"
//        });
//        return false;
//    });
}

function saveFontSizeInCookie(fontSize) {    
    $.cookie("font-size", "", {
        expires: 99999, 
        path: "/" 
    });   
}
function getSavedFontSize() {
    return $.cookie("font-size");
}

/**
 * Make sure you push any required css files into "themeCssFiles" before calling
 */
function initHtmlEditors(elements, height, width, extraPlugins, removePlugins) {    
    log("initHtmlEditors: css files=", themeCssFiles);
    if( !$('.htmleditor').ckeditor ) {
        log("ckeditor jquery adapter is not loaded");
        return;
    }
    if( !elements ) {
        elements = $(".htmleditor");
    }
    if( !extraPlugins ) {
        extraPlugins = 'autogrow,embed_video,modal';  // see /static/js/toolbars.js
    }
    if( !removePlugins ) {
        removePlugins = 'resize';
    }    
    log("prepare html editors", elements);
    elements.each(function(i,n) {
        var inp = $(n);        

        var inputClasses = inp.attr("class");
        var id = inp.attr("id");
        var toolbar = "Default";
        if(inputClasses) {
            c = inputClasses.split(" ");
            for( i=0; i<c.length; i++  ) {
                var s = c[i];
                if( s.startsWith("toolbar-")) {
                    s = s.substring(8);
                    toolbar = s;
                    break;
                }
            }    
        }

        log("using toolbar",toolbar,"=>", toolbarSets[toolbar]);
        var config = {
            skin: editorSkin,
            contentsCss: themeCssFiles, // mainCssFile,
            bodyId: "editor",
            templates_files : [ '/static/editor/templates.js' ],
            templates_replaceContent: false,
            toolbar: toolbarSets[toolbar],
            extraPlugins : extraPlugins,
            removePlugins : removePlugins,
            enterMode: "P",
            forceEnterMode:true,
            filebrowserBrowseUrl : '/static/fckfilemanager/browser/default/browser.html?Type=Image&Connector=/fck_connector.html',
            filebrowserUploadUrl : '/uploader/upload',
            format_tags : 'p;p2;h2;h3;h4;h5',
            format_p2 : {
                element : 'p', 
                attributes : {
                    'class' : 'lessSpace'
                }
            }
        };    
        if( height ) {
            config.height = height;            
        }
        if( width ) {
            config.width = width;
        }
    
        config.stylesSet = 'myStyles:/templates/themes/fuse/styles.js'; // TODO: needs to be configurable, based on theme
        log("create editor", inp, config);
        inp.ckeditor(config);
    });  
}


function initTabPanel() {
    var tab_container = $('.TabContainer');
    log("initTabPanel", tab_container);
    if(tab_container[0]) {
        var tab_content = tab_container.find('.TabContent');
        var tab_nav = tab_container.find('nav a');
		
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

// Function check/uncheck for checkbox
$.fn.check = function(is_check) {
    return $(this).attr('checked', is_check);
};

// Function disable/enable for form control
$.fn.disable = function(is_disable) {
    return $(this).attr('disabled', is_disable);
};

// Ensure support for toISOString in all browsers
if (!Date.prototype.toISOString) {
    Date.prototype.toISOString = function() {
        function pad(n) {
            return n < 10 ? '0' + n : n
        }
        return this.getUTCFullYear() + '-'
        + pad(this.getUTCMonth() + 1) + '-'
        + pad(this.getUTCDate()) + 'T'
        + pad(this.getUTCHours()) + ':'
        + pad(this.getUTCMinutes()) + ':'
        + pad(this.getUTCSeconds()) + 'Z';
    };
}

var typewatch = (function(){
    var timer = 0;
    return function(callback, ms){
        clearTimeout (timer);
        timer = setTimeout(callback, ms);
    }  
})();



function showAddItem(source) {
    var modal = $(source).parent().find(".Modal");
    $.tinybox.show(modal, {
        overlayClose: false,
        opacity: 0
    }); 
    return false;
}

function initRotation() {
    $(function() {
        var degree = 0;
	
        setInterval(function() {
            if(degree === 360) {
                degree = 0;
            } else {
                degree += 2;
            }
		
            $('.rotate.anticlockwise').rotate(-degree);
            $('.rotate.clockwise').rotate(degree);
        }, 50);
    });            
}

function initPlaceholder() {
    $('input, textarea').placeholder();    
}


/** End init-theme.js */