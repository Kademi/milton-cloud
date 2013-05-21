
    function initPosts(basePath, externalAuth) {    
        $.getJSON(basePath + "_postSearch?a", function(response) {
            log("got posts response", response);
            processPosts(response, externalAuth);
        });
    }

    function processPosts(comments, externalAuth) {
        $(".barList").html("");
        if( comments ) {
            if( comments.length > 0 ) {
                comments.sort( dateOrd );
                var cookieAuth = "?miltonUserUrl=" + $.cookie("miltonUserUrl");
                cookieAuth += "&miltonUserUrlHash=" + $.cookie("miltonUserUrlHash");
                for( i=0; i<comments.length; i++ ) {
                    var comment = comments[i];
                    var dt = new Date(comment.date);
                    var url;
                    if( externalAuth ) {
                        url = "/managePosts?gotoPostId=" + comment.id;
                    } else {
                        url = "http://" + comment.contentDomain + comment.contentHref;
                    }
                    displayPost(comment.user, dt, comment.notes, comment.contentTitle, url, externalAuth); // pageTitle and pagePath are only present for aggregated results
                }
                jQuery(".barList abbr").timeago();
            } else {
                $(".barList").html("<li>No recent posts</li>");
            }
        }
    }

    function displayPost(user, date, comment, pageTitle, pagePath, externalAuth) {   
        if( user == null ) {
            log("missing user, ignore");
            return;
        }
        log("user", user);
        var li = $("<li>");
        var manageUserHref = "manageUsers/" + user.userId;
        if( user.photoHash ) {
            li.append("<a class='profilePic' href='" + manageUserHref + "'><img src='/_hashes/files/" + user.photoHash + "' alt='' /></a>");
        } else {
            li.append("<a class='profilePic' href='" + manageUserHref + "'><img src='/templates/apps/user/profile.png' alt='' /></a>");
        }
        li.append("<a class='user' href='" + manageUserHref + "'>" + user.name + "</a> posted in <a class='module' href='" + pagePath + "'><span>" + pageTitle + "</span></a>");
        if( externalAuth ) {
            li.find("a.module").attr("target", "_blank");
        }
        var formattedDate = date.toISOString();
        var commentHtml = "<div class='post'>";    
        commentHtml += "<p>" + comment + "</p>";
        commentHtml += "<em><abbr class='timeago' title='" + formattedDate + "'>" + formattedDate + "</abbr></em>";
        commentHtml += "</div>";
        li.append(commentHtml);
    
        li.append();
        $(".barList").append(li);
            
        //                <li>
        //                    <a class="profilePic" href="#"><img src="/content/images/CarlBrown.jpg" alt="" /></a>
        //                    <a class='user' href='#'>Carl Brown</a> posted in <a class='module' href='module'><span>module 8</span></a>
        //                    <p>Lorem ipsum dolor sit amet, ultricies dui proin, eget aliquam tincidunt risus pede ullamcorper praesent...</p>
        //                    <em>Today 3:34</em>
        //                    <small>5 Comments</small>
        //                </li>             
    }
    
