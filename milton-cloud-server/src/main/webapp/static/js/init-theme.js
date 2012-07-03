/** Start theme.js */

/**
 *  Editor support: Note that this relies on a global variable called toolbarSets
 *  
 *  A default is defined in toolbars.js. You should override that file in your
 *  application to get the toolbars you want
 */


function initTheme() {
    log("init ettrema-theme");
    
    $(".Login").user(); // setup login and logout
    
    jQuery('textarea.autoresize').autoResize();    
    
    initNav();
           
    initExtraInfo();
               
    initFontSwitching();
         
    log("finished init ettrema-theme");
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

function initHtmlEditors(mainCssFile) {    
    log("initHtmlEditors");
    if( !mainCssFile ) {
        mainCssFile = "/templates/style.css";
    }
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
            contentsCss: mainCssFile,
            bodyId: "editor",
            templates_files : [ '/templates/editor/ck-templates/templates.js' ],
            templates_replaceContent: false,
            toolbar: toolbarSets[toolbar],
            extraPlugins : 'autogrow',
            removePlugins : 'resize',
            enterMode: "P",
            forceEnterMode:true,
            filebrowserBrowseUrl : '/templates/filemanager/browser/default/browser.html?Type=Image&Connector=/fck_connector.html',
            filebrowserUploadUrl : '/uploader/upload',
            format_tags : 'p;h2;h3;h4;h5'
        };    
    
        log("ckeditor config", config);

        config.stylesSet = 'myStyles:/templates/themes/3dn/js/styles.js';
        $('.htmleditor').ckeditor(config);
    });  
}

function edify() {
    log("edify");
    initHtmlEditors(["/templates/apps/learner/learning.dyn.css","/templates/apps/learner/moduleLayout.dyn.css", "/templates/apps/learner/moduleContent.dyn.css"]);
    $(".panelBox").wrap("<form id='edifyForm' action='" + window.location + "' method='POST'></form>");
    var btn = $("<button type='submit'>Save</button>");
    $("#edifyForm").prepend(btn);
    $("#edifyForm").append("<input type='hidden' name='body' value='' />");
    $("#edifyForm").submit(function() {
        log("submit form");
        try {
            log("ckeditors", CKEDITOR.instances);
            $("#edifyForm input[name=body]").attr("value", CKEDITOR.instances["editor1"].getData() );
            $.ajax({
                type: 'POST',
                url: $("#edifyForm").attr("action"),
                data: $("#edifyForm").serialize(),
                dataType: "json",
                success: function(resp) {
                    ajaxLoadingOff();
                    log("save success", resp)
                    alert("done");
                },
                error: function(resp) {
                    ajaxLoadingOff();
                    alert("err");
                }
            });     
        } catch(e) {
            log("exception", e);
        }
        return false;
    });
//$("#edifyBody").wrap("<textarea></textarea>");
}

/** End theme.js */