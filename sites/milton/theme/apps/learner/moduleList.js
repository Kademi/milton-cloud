function initMyLearning(){
    log("initMyLearning");

    initCoursePjax();

    $("body").bind("pjax:end", function() {
        log("pjax end");
        jQuery("a.courseBox").removeClass("active");
        setActiveCourse();
        var programPath = getPathFromHref(getFolderPath(getFolderPath(window.location.href))) + "/";
        initPjax(programPath);   
        initExtraInfo();        
    });
    log("now do initPjax");
    initPjax("${curProgram.url}");
}

function initCoursePjax() {
    var a = jQuery('a.course').not(".pjaxdone");    
    log("pjax11", a);
    jQuery('a.course').pjax('.modules tbody', {
        fragment: ".modules tbody"
    });        
    a.addClass("pjaxdone");
}

function initPjax(programPath) {
    log("initPjax", programPath);
        
    setActiveCourse();
    // Store current course in a cookie
    storeProgramCookie(programPath);
    jQuery(".modules tbody tr.title:even").addClass("even");
    jQuery(".modules tbody tr.brief:even").addClass("even");
    jQuery(".modules tbody tr.title").filter(":last").addClass("last");    
    jQuery(".modules tbody tr.brief:last-child").addClass("last");
    jQuery("body").on("click", ".modules tbody tr.title td.first, .modules tbody tr.title td.title", function(e) {
        e.stopPropagation();
        var href = $(e.target).closest("tr").find("a").attr("href");
        window.location.href = href;
    });
}

function storeProgramCookie(path) {
    log("storeProgramCookie", path);
    jQuery.cookie("currentProgram", path, {
        path: "/"
    });                
}                 

function setActiveCourse() {                
    jQuery("a.courseBox").each(function(i, node) {
        log("active?", window.location.pathname, $(node).attr("href"));
        if($(node).attr("href") == window.location.pathname) {
            $(node).addClass("active");
        }
    });                                    
}   
function initExtraInfo() {
    log("initExtraInfo");
    jQuery(".infoIcon").click(function(e) {
        log("click", e);
        e.stopPropagation();
        $(e.target).closest("tr").toggleClass("expanded");
        var expandContainer = $(e.target).closest("tr").next();
        log("clicked. container=", expandContainer);
        jQuery(expandContainer).toggle(300).toggleClass("expanded");
        return false;
    });        
}    