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

function insertJPlayer(container, src, title, height, width, cssClass) {
    log("insertJPlayer", container, src, title, height, width, cssClass);
    container.load("/static/jplayer/player-fragment.html", function(i, n) {
        log("loaded player fragment", container);
        var container = $("div.jp-video", container);
            
        // set the title
        $(".jp-title li").text(title);
            
        // setup id's as required by jp
        var uniqueId = "vid_" + Math.ceil(Math.random() * 1000000);
        var containerId = uniqueId + "_cont";
        var playerId = uniqueId + "_player";
        container.attr("id", containerId);
        $(".jp-jplayer", container).attr("id", playerId);
            
        // now setup the player
        log("setup jplayer", $("#" + containerId));
        $(".jp-jplayer", container).jPlayer({
            swfPath: "/static/jplayer",
            supplied: "mp4, webmv, flv",
            size: {
                width: width,
                height: height,
                cssClass: cssClass
            },        
            cssSelectorAncestor: "#" + containerId
        });
    
        if( src ) {
            log("play", src);
            playVideo("#" + playerId, src);             
        }        
    });            
}
 
function playVideo(playerSel, primaryUrl) {
    log("playVideo", playerSel, $(playerSel), primaryUrl);
    // given the posterUrl, calculate path to other media..
    
    log("filename", primaryUrl);
    var posterUrl = primaryUrl + "/alt-800-455.png";
    $(playerSel).jPlayer("setMedia", {
        webmv: primaryUrl + "/alt-800-455.webm",  
        //ogv: primaryUrl + "/alt-800-455.ogv",        
        flv: primaryUrl + "/alt-800-455.flv",
        m4v: primaryUrl + "/alt-800-455.mp4",   
        //                m4v: "http://www.jplayer.org/video/m4v/Big_Buck_Bunny_Trailer_480x270_h264aac.m4v",
        //                ogv: "http://www.jplayer.org/video/ogv/Big_Buck_Bunny_Trailer_480x270.ogv",
        poster: posterUrl //"http://www.jplayer.org/video/poster/Big_Buck_Bunny_Trailer_480x270.png"
    });
    //$(playerSel).jPlayer("play");
}
