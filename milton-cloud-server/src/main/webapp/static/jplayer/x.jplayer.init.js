/** Start jplayer-init.js **/

// "800px", "455px", "jp-video-fuse"
function initJPlayer(height, width, cssClass) {
    log("initJPlayer", $("img.video"))
    $("img.video").each(function(i, n) {
        var img = $(n);
        log("replace image with div", img);        
        var posterUrl = img.attr("src");
        var title = img.attr("alt");
        log("create jplayer for:", src);
        //        var width = img.attr("width");
        //        var height = img.attr("height");
        var vidDiv = $("<div class='video'></div>");
        img.replaceWith(vidDiv);
        var src = getFolderPath(posterUrl);
        insertJPlayer(vidDiv, src, title, height, width, cssClass);
    });
}

var jplayerId = 0;

function insertJPlayer(container, src, title, height, width, cssClass) {
    log("insertJPlayer", container, src, title, height, width, cssClass);
    container.load("/static/jplayer/player-fragment.html", function(i, n) {
        log("loaded player fragment", container);
        var vidContainer = $("div.jp-video", container);
            
        // set the title
        $(".jp-title li").text(title);
            
        // setup id's as required by jp
        var uniqueId = "vid_" + jplayerId++;
        log("jplayer id", uniqueId);
        var containerId = uniqueId + "_cont";
        var playerId = uniqueId + "_player";
        log("set id on vidContainer", vidContainer, containerId);
        vidContainer.attr("id", containerId);
        $(".jp-jplayer", vidContainer).attr("id", playerId);
            
        // now setup the player
        log("setup jplayer", $("#" + containerId));
        $(".jp-jplayer", vidContainer).jPlayer({
            swfPath: "/static/jplayer",
            supplied: "mp4, webmv, flv",
            size: {
                width: width + "px",
                height: height + "px",
                cssClass: cssClass
            },        
            cssSelectorAncestor: "#" + containerId
        });
    
        if( src ) {
            log("play", src);
            playVideo("#" + playerId, src,height, width);             
        }        
    });            
}
 
function playVideo(playerSel, primaryUrl, height, width) {
    log("playVideo", playerSel, $(playerSel), primaryUrl);
    // given the posterUrl, calculate path to other media..
    
    log("filename", primaryUrl);
    var basePath = primaryUrl + "/alt-" + width + "-" + height + ".";
    var posterUrl = basePath + "png";
    $(playerSel).jPlayer("setMedia", {
        webmv: basePath + "webm",  
        flv: basePath + "flv",
        m4v: basePath + "mp4",   
        poster: posterUrl
    });
}
