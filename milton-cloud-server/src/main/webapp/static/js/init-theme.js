/** Start theme.js */

/**
 *  Editor support: Note that this relies on a global variable called toolbarSets
 *  
 *  A default is defined in toolbars.js. You should override that file in your
 *  application to get the toolbars you want
 */

CKEDITOR_BASEPATH = "/static/ckeditor36/";


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
    initPseudoClasses();
    initPrintLink();
    initLoginDropDown();
    initDropDownHiding();     
         
    log("initTheme: run page init functions", pageInitFunctions.length);
    $.each(pageInitFunctions, function(i, f) {
        log("run function" + i);
        pageInitFunctions[i]();
        log("done run function" , i);
        
    });

    $(".DropdownWrapper").click(function(e, node) {        
        log("initDropDown click", e);
        var div = $(e.target).closest("div.DropdownControl");        
        log("dropdown", $(".DropdownContent", div));
        $(".DropdownContent", div).toggle(300);
    });  
         
    log("finished init-theme");
} 

function initLoginDropDown() {
    log("init login", $(".Login"));
    var login = $(".Login");
    var dropdown = login.find(".dropBox");
    login.find("> a").click(function(e) {
        e.preventDefault();
        e.stopPropagation();
        log("hide1", e.target);
        dropdown.toggle(300);
    });
    dropdown.click(function(e) {
        e.stopPropagation();
    });
    $("body").click(function() {
        if( dropdown.is(":visible")) {
            log("hide2");
            dropdown.toggle(300);
        }
    });
}


function initHelp () {
    $(".helpIcon").click(function(e) {
        var modal = $("#modalHelp");
        $.tinybox.show(modal, {
            overlayClose: false,
            opacity: 0
        });
        e.preventDefault();
        modal.find(".ModalContent").html("Loading help...");
        log("meta", $(document).find("meta"));
        var page = $(document).find("meta[name=templateName]").attr("value");
        // TODO: make this domain configurable
        //var href = "http://fuselms.loopbackdns.com:8080/docs";
        var href = "http://www.fuselms.com/docs";
        href += page;
        modal.find(".ModalContent").load(href + " #helpContent", function(responseText, textStatus, XMLHttpRequest) {
            log("finished", textStatus);
            if( textStatus == "error") {
                modal.find(".ModalContent").html("Sorry, help content for this page is unavailable");
            }
        });
        
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
        extraPlugins = 'autogrow,embed_video,image2,modal';  // see /static/js/toolbars.js
    }
    if( !removePlugins ) {
        removePlugins = 'resize,image';
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
            format_tags : 'p;h1;h2;h3;h4;h5', // removed p2
            format_p2 : {
                element : 'p', 
                attributes : {
                    'class' : 'lessSpace'
                }
            }
        };    
        if( height ) {
            config.height = height;            
        } else {
            config.height = "300";
        }
        if( width ) {
            config.width = width;
        }
    
        config.stylesSet = 'myStyles:/templates/themes/fuse/styles.js'; // TODO: needs to be configurable, based on theme
        log("create editor", inp, config);
        inp.ckeditor(config);
    });  
}


// Event for tab panel
function initTabPanel() {
    log("initTabPanel");
    $("nav.TabNav a").on("click", function(e) {
        //e.preventDefault();
        log("initTabPanel:click1", this);
        var href = $(this).attr("href");
        $(this).addClass("Active").siblings().removeClass("Active");
        $(".TabContent").hide().filter("[rel=" + href + "]").show();
					        
    });		
    
    var tabLinks = $("body").find("nav.TabNav a");
    var url = window.location.href;
    if( url.contains("#")) {
        var tabId = url.substring(url.indexOf("#"));
        log("tabId", tabId);
        var selectedTab = tabLinks.filter("[href=" + tabId + "]");
        log("selectedTab", selectedTab);
        if( selectedTab.length > 0 ) {
            selectedTab.first().click();
            return;
        }
    }
    var firstTab = $("body").find("nav.TabNav a").eq(0);
    log("select first tab", firstTab);
    firstTab.trigger("click");
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

function initPseudoClasses() {
    $("table tr td:first-child").addClass("first");
    $("table tr td:last-child").addClass("last");
    $("table tr:nth-child(odd)").addClass("odd");
    $("table tr:nth-child(even)").addClass("even");
    $("ul li:nth-child(even)").addClass("even");
    $("ul li:nth-child(odd)").addClass("odd");    
}

function initPrintLink() {
    $("a.print").click(function(e) {
        e.preventDefault();
        window.print();
    });
}

function initDropDownHiding() {
    $(".content").on("click", "a.ShowDialog",function(e) {
        log("hi111");
        var _this = $(this);
        var dialog = _this.parent().find("> div");
        log("show dialog", dialog, _this);        
        dialog.toggle();
        e.preventDefault();
    });
    
    // Hide DropDownContent which clicking elsewhere
    $('body').click(function(event) {
        var target = $(event.target);        
        if( target.closest("div.DropdownControl, .ShowDialog, .Dialogue").length > 0 ) {
            return; // don't handle events inside the dropdown'
        }
        var dropdown = $("div.DropdownContent, .Dialog").filter(":visible");
        if( dropdown.length > 0 ) {
            if (!target.closest('div.DropdownContent, Dialog').length) {
                log("initDropDownHiding-click: hide", target);
                dropdown.hide();
            }
        }
    });
    
}



/**
 *  Although this function is defined here in the theme, it should be called
 *  from each page.
 *  
 *  Each page should decide what url to pass as the pageUrl, as this can be used
 *  to share comments across pages (such as when the logical context is the folder
 *  the pages are in, rather then each page)
 *  
 *  Eg initComments(window.location.pathname);
 */
function initComments(pageUrl) {
    log("initComments", pageUrl);
    $(".hideBtn").click(function() {
        var oldCommentsHidden = $("#comments:visible").length == 0;
        log("store new comments hidden", oldCommentsHidden);
        jQuery.cookie("commentsHidden", !oldCommentsHidden, {
            path: "/"
        });
        $("#comments").toggle(100, function() {
            if(!oldCommentsHidden) {
                $(".hideBtn a").text("Show comments");
                $(".hideBtn a").addClass("ishidden");
            } else {
                $(".hideBtn a").text("Hide comments");
                $(".hideBtn a").removeClass("ishidden");
            }
        });
        return false;
    });
    var commentsHidden = jQuery.cookie("commentsHidden", {
        path: "/"
    });
    log("comments hidden", commentsHidden);    
    if( commentsHidden == "true" ) {
        $("#comments").hide();
        $(".hideBtn a").text("Show comments");
        $(".hideBtn a").addClass("ishidden");
    }
    
    $("body").on("click", ".commentContainer textarea", function(e) {
        $(e.target).closest("div").find(".commentControls").show();
    });    
    $('.commentContainer textarea').css('overflow', 'hidden').autogrow()
    
    $("#comments").comments({
        currentUser : {
            name: userName,
            href: userUrl, 
            photoHref: "/profile/pic"
        },
        pageUrl: pageUrl,
        renderCommentFn: function(user, date, comment) {
            log("renderCommentFn", user);
            if( user == null ) {
                log("no user so dont render");
                return;
            }            
            var profilePic = profileImg(user);
            var profLink = $("<a class='profilePic' href='" + user.href + "'>" + profilePic + "</a>");
            var nameLink = $("<a class='user' href='" + user.href + "'>" + user.name + "</a>");
            var commentPara = $("<p>" + comment + "</p>");
            //var dateSpan = $("<span class='auxText'>" + toDisplayDateNoTime(date) + "<a href='#'>Reply to this comment</a></span>");
            
            var dateSpan = $("<abbr title='" + date.toISOString() + "' class='auxText'>" + toDisplayDateNoTime(date) + "</abbr>");
            var toolsDiv = $("<div></div>");
            /**
            var del = $("<a class='auxText' href='#'>Delete</a>");
            var abuse = $("<a class='auxText' href='#'>Report abuse</a>");
            toolsDiv.append(del);
            toolsDiv.append(abuse);
             **/
            var outerDiv = $("<div class='forumReply'></div>");
            outerDiv.append(profLink);
            outerDiv.append(nameLink);
            outerDiv.append(commentPara);
            outerDiv.append(dateSpan);
            outerDiv.append(toolsDiv);
            outerDiv.insertAfter($("#comments .fBox"));
            
            jQuery("abbr.auxText", outerDiv).timeago();
        }
    });    
}


function initSelectAll() {
    $("body").on("click", ".selectAll", function(e) {
        var node = $(e.target);
        log("selectall", node, node.is(":checked"));
        var chkName = node.attr("name");
        var checked = node.is(":checked");
        checkBoxes = node.closest("table").find("tbody td input:[type=checkbox]:[name=" + chkName + "]");
        if( checked ) {
            checkBoxes.attr("checked", "true");
        } else {
            checkBoxes.removeAttr("checked");
        }
    });
}

/** End init-theme.js */