var currentModuleName;
var currentModuleId;
var currentPage;
var isWritable;
var modStatusUrl;
var modStatusComplete;



function initModuleNav(pStatUrl, pFinished) {
    modStatusUrl = pStatUrl;
    modStatusComplete = pFinished;
    $("#saveBtn").click(function() {
        saveExit();
    });
    $("#submitBtn").click(function() {
        finishQuiz();
    });
    $("div.header").append("<a class='backToMyLearning' href='../'>Back to My Learning</a>"); // href is modified in user.js to be relative to userUrl

    initPageNav();    
    $('.commetnArea a').pjax('.panelBox', {
        fragment: ".panelBox",
        success: function() {
            log("done!");
            initPageNav();
        }
    });
}

function initPageNav() {
    // set active on current page nav
    log("initPageNav. modStatusComplete=", modStatusComplete, "userurl", userUrl);
    
    $('html, body').animate({scrollTop:0}, 'slow');
    
    initJPlayer();
    
    $(".pages a").removeClass("active")
    var pageName = getFileName(window.location.href);
    var current = "#page_" + pageName;
    $(current).addClass("active")

    $(".prevBtn").attr("href", getMoveHref(-1));
    $(".nextBtn").attr("href", getMoveHref(1));

    if( pageName == "finished") {
        $(".nextBtn").hide();
    } else {
        $(".nextBtn").show();
    }

    // insert spans so we can use sprites for background images
    $(".dropdown, .btnHideFollowing, .activity, .keyPoint, .lightbulb h6").prepend("<span class='sprite'>&nbsp;</span>");
    
    $(".btnHideFollowing").nextAll().hide(); // initially hide everything after it
    $(".btnHideFollowing").click(function() {
        var toToggle = $(this).nextUntil(".btnHideFollowing");
        log("btnHideFollowing: toggle:", toToggle);
        toToggle.show(200);
        // also show the next button, if there is one
        var last = $(toToggle[toToggle.length-1]);
        last.next().show(); // because toToggle is nextUntil .btnHideFollowing, the next after the last should be a btnHideFollowing, or nothing
    });

    $("div.dropdown > h3, div.dropdown > h4, div.dropdown > h5,div.dropdown > h6,div.dropdown span.sprite").click(function() {
        var dropDownDiv = $(this).parents("div.dropdown");
        if( dropDownDiv.length > 1) {
            dropDownDiv = $(dropDownDiv[0]);
        }
        log("toggle visibility", $("> div", dropDownDiv));
        $("> div", dropDownDiv).toggle(200, function() {
            log("set open class", this, $(":visible", $(this)));
            var vis = $(":visible", $(this)).length > 0;
            if( vis ) {
                dropDownDiv.addClass("open");
            } else {
                dropDownDiv.removeClass("open");
            }
        });
        return false;
    });

    
    if( modStatusComplete ) {
        log("initPageNav: module is complete so do nothing");            
    } else {
        if( pageName == "pFinished.html") {
            log("initPageNav: module has now been finished, so mark completed");
            completed();
        } else {
            log("initPageNav: module is started, save page");
            window.setTimeout(function() {
                save();
            }, 1000);        
        }
    }
}

function getMoveHref(count) {
    var i = currentPageIndex();
    log("cur index", i);
    var allLinks = $(".pages a");
    i = i + count;
    if( i < 0) {
        i = 0;
    }
    if( i >= allLinks.length ) {
        //i = allLinks.length-1;
        return "finished";
    } else {
        var href = $($(".pages a")[i]).attr("href");
        log("getMoveHref: ", count, "moved index", i, "result:", href);
        return href;
    }
}

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
        log("hiding comments")
    }
    
    
    $("#comments").comments({
        currentUser : {
            name: userName,
            href: userUrl, 
            photoHref: ""
        },
        pageUrl: pageUrl,
        renderCommentFn: function(user, date, comment) {
            log("module.js renderCommentFn", date, comment);
            if( user == null ) {
                log("no user so dont render");
                return;
            }
            var profLink = $("<a class='profilePic' href='" + user.href + "'><img src='/users/a1/profilePic.gif' alt='' /></a>");
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


function setReadonly() {
    //readonly = true;
    log("setReadOnly");
    $("#submitBtn").attr('disabled', 'disabled');
    $("#submitBtn").hide();
    $("#saveBtn").css("visibility", "hidden");
}

/**
* A completed quiz is made readonly
*/
function isReadonly() {
    return $("#submitBtn").attr('disabled');
}


function save(callback) {
    log("save. isWritable=", isWritable)
    //    if( !isWritable ) {
    //        return;
    //    }
    var currentPage = getFileName(window.location.href);
    var url = modStatusUrl + "_DAV/PROPPATCH";
    log("save. url=", url, "currentPage", currentPage);
    $.ajax({
        type: "POST",
        url: url,
        data: "milton:currentPage=" + currentPage,
        success: function(response){
            log('saved ok', response);
            if( callback ) {
                callback();
            }
        },
        error: function(response) {
            log('error saving moduleStatus', response);
            alert("There was an error saving your progress");
        }
    });
}

/*
* Called to indicate that the module is complete
*/
function completed() {
    log("completed", modStatusUrl);
    var url = modStatusUrl + "_DAV/PROPPATCH";
    ajaxLoadingOn();
    $.ajax({
        type: "POST",
        url: url,
        data: "milton:complete=true",
        success: function(response){
            ajaxLoadingOff();
        },
        error: function(response) {
            ajaxLoadingOff($("#submitBtn"));
            showDialog("Error!" ,"There was an error saving your progress");
        }
    });
}


function controlButtons(allowProgression) {
    var cur = currentPageIndex();
    var num = numberOfPages();
    log("controlButtons",cur, num);
    if( cur == num - 1) { // this is the quiz page or, in the case of the booster, the only page
        $("#nextBtn").hide();
        $(".module .intro").show();
        if(allowProgression) {
            $("#submitBtn").show();		  
        } else {
            $("#submitBtn").hide();
        }		
    } else {
        $(".module .intro").hide();
        if(allowProgression) {
            $("#nextBtn").show();
        } else {
            $("#nextBtn").hide();
        }
        $("#submitBtn").hide();
    }
}

function currentPageIndex() {
    var current = $(".pages a.active").attr("id");    
    var currentIndex = 0;
    var all = $(".pages a");
    all.each(function(index) {
        if( $(this).attr("id") == current ) {
            currentIndex = index;
        }
    });
    log("currentPageIndex", current, currentIndex);
    return currentIndex;
}

/**
 *Returns teh current page object
 */
function getCurrentPage() {
    return pagesArray[currentPageIndex()];
}

function numberOfPages() {
    var all = $(".pages a");
    return all.length;
}

function isLastPage() {    
    b = currentPageIndex() == (numberOfPages()-1);
    log("isLastPage", currentPageIndex(), numberOfPages(), b)
    return b;
}

function initMedia() {
   
}

