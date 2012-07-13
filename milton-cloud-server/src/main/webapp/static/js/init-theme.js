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
    
    initActiveNav();    
    initEdify();    
    initNav();           
    initExtraInfo();               
    initFontSwitching();
    initHelp();
    initModal();
    
         
    log("initTheme: run page init functions");
    for( i=0; i<pageInitFunctions.length; i++) {
        pageInitFunctions[i]();
    }
         
    log("finished init-theme");
} 

function initActiveNav() {
    var url = window.location.pathname;
    log("initActiveNav", url);
    $("a").each(function(i, n) {
        var node = $(n);
        var href = node.attr("href");
        if( href ) {
            if( href.startsWith(url) ) {
                node.addClass("active");
            }
        }
    });
}

function initModal() {
    $("body").on("click", ".Modal a.Close", function(e) {
        $.tinybox.close();
        e.preventDefault();
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

function initExtraInfo() {    
    jQuery(".infoIcon").click(function(item) {
        var expandContainer = $(item.target).parents(".wBox");
        log("clicked. container=", expandContainer);
        jQuery(".extraInfo", expandContainer).toggle(100);
        return false;
    });    
    
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
    $.cookie("font-size", fontSize, {
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
function initHtmlEditors() {    
    log("initHtmlEditors");
    if( !$('.htmleditor').ckeditor ) {
        log("ckeditor jquery adapter is not loaded");
        return;
    }
    log("prepare html editors");
    $(".htmleditor").each(function(i,n) {
        var inp = $(n);        

        var inputClasses = inp.attr("class");
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
            extraPlugins : 'autogrow,embed_video',
            removePlugins : 'resize',
            enterMode: "P",
            forceEnterMode:true,
            filebrowserBrowseUrl : '/static/fckfilemanager/browser/default/browser.html?Type=Image&Connector=/fck_connector.html',
            filebrowserUploadUrl : '/uploader/upload',
            format_tags : 'p;h2;h3;h4;h5'
        };    
    
        //config.stylesSet = 'myStyles:/templates/themes/3dn/js/styles.js';
        log("create editor", inp, config)
        inp.ckeditor(config);
    });  
}


/** End init-theme.js */