/**
 *
 *  jquery.comments.js
 *
 * Config:
 * pageUrl - url of the resource to add comments to. Must end with a slash
 * a submit handler
 * renderCommentFn - a callback function to render the markup for a comment. Takes the following arguments user, comment, commentDate, where user
 * is an object containing name, href, photoHref
 * clearContainerFn - callback function to clear the comments container. Takes no arguments
 * ajaxLoadingFn - callback function to show ajax loading. Takes one argument isLoading (true/false)
 * currentUser - user object containing name, href, photoHref
 * 
 */

(function( $ ) {
    $.fn.comments = function(options) {
        var container = this;
        var config = $.extend( {
            'pageUrl' : window.location,
            'renderCommentFn' : function(user, date, comment) {
                var s = "";
                s = s + "<p class='forumAnnotation'>" + user.name + " | " + toDisplayDateNoTime(date) + "</p>";
                s = s + "<p class='forumText'>" + comment + "</p>";
                container.append("<div class='forumComment'>" + s + "</div>");
            },
            'clearContainerFn' : function() {
                $(".forumComment", this).remove();
            },
            'ajaxLoadingFn' : function(isLoading) {
                if( isLoading ) {
                    ajaxLoadingOn();
                } else {
                    ajaxLoadingOff();
                }
            },
            'aggregated': false  // if true will list all comments under the given page 
        }, options);  
  
        log("register submit event", $("form", this));
  
        $("form", this).submit(function(e) {
            e.preventDefault();
            e.stopPropagation();
            try {
                sendNewForumComment(config.pageUrl, $("textarea",this), config.renderCommentFn, config.currentUser);                            
            } catch(e) {
                log("exception sending forum comment", e);
            }
            return false;
        });  
  

        loadComments(config.pageUrl, config.renderCommentFn, config.clearContainerFn, config.aggregated);
    };
})( jQuery );

function sendNewForumComment( pageUrl, commentInput, renderComment, currentUser) {
    log("sendNewForumComment", pageUrl, commentInput, currentUser);
    if( currentUser.href == null ) {
        alert("You must be logged in to post comments");
        return;
    }
    var comment = commentInput.val();
    commentInput.removeClass("errorField");
    if( comment.trim().length < 1 ) {
        commentInput.addClass("errorField");
        return;
    }
    var url = pageUrl;
    if( !url.endsWith("/")) {
        url += "/";
    }
    url += "_DAV/PROPPATCH";
    ajaxLoadingOn();
    $.ajax({
        type: 'POST',
        url: url,
        data: "milton:newComment=" + comment,
        dataType: "text",
        success: function() {
            ajaxLoadingOff();
            commentInput.val('');
            commentInput.keyup();
            currentDate = new Date();
            renderComment(currentUser, currentDate, comment);
        },
        error: function() {
            ajaxLoadingOff();
            alert('Sorry, we could process your comment. Please try again later');
        }
    });
}

function loadComments(page, renderCommentFn, clearContainerFn, aggregated) {
    commentUrl = page;
    var url = page;
    if( !url.endsWith("/")) {
        url += "/";
    }    
    if( aggregated ) {
        url += "/_comments";
    } else {
        url += "_DAV/PROPFIND?fields=milton:comments&depth=0";
    }
    
    
    $.getJSON(url, function(response) {
        log("got comments response", response);
        clearContainerFn();
        if( aggregated ) {
            processComments(response, renderCommentFn);
        } else {
            var comments = response[0].comments;
            processComments(comments, renderCommentFn);
        }
    });
}

function processComments(comments, renderCommentFn) {
    if( comments ) {
        comments.sort( dateOrd );
        $.each(comments, function(i, comment) {
            var dt = new Date(comment.date);
            renderCommentFn(comment.user, dt, comment.comment, comment.pageTitle, comment.pagePath); // pageTitle and pagePath are only present for aggregated results            
        });
    }
    
}


