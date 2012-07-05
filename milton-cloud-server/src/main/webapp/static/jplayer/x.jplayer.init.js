/** Start jplayer-init.js **/


function initJPlayer() {
    log("initJPlayer", $("img.video"))
    $("img.video").each(function(i, n) {
        var img = $(n);
        log("replace image with div", img);        
        var src = img.attr("src");
        var title = img.attr("alt");
        log("create jplayer for:", src);
        //        var width = img.attr("width");
        //        var height = img.attr("height");
        var vidDiv = $("<div class='video'></div>");
        img.replaceWith(vidDiv);
        vidDiv.load("/static/jplayer/player-fragment.html", function(i, n) {
            log("loaded player fragment", vidDiv);
            var container = $("div.jp-video", vidDiv);
            
            // set the title
            $(".jp-title li").text(title);
            
            // setup id's as required by jp
            var uniqueId = "vid_" + Math.ceil(Math.random() * 1000000);
            var containerId = uniqueId + "_cont";
            var playerId = uniqueId + "_player";
            container.attr("id", containerId);
            $(".jp-jplayer", container).attr("id", playerId);
            
            // now setup the player
            makeJPlayer("#" + playerId, "#" + containerId, src, title);             
        });        
    });
}
 
function makeJPlayer(playerSel, contSel, posterUrl, title) {
    log("makeJPlayer", $(playerSel), posterUrl);
    // given the posterUrl, calculate path to other media..
    var p = posterUrl.lastIndexOf("/");
    log("p", p);
    var primaryUrl = posterUrl.substring(0, p);
    log("filename", primaryUrl);
    $(playerSel).jPlayer({
        ready: function () {
            $(this).jPlayer("setMedia", {
                webmv: primaryUrl + "/alt-800-455.webm",  
                ogv: primaryUrl + "/alt-800-455.ogv",        
                flv: primaryUrl + "/alt-800-455.flv",
                m4v: primaryUrl + "/alt-800-455.mp4",   
                //                m4v: "http://www.jplayer.org/video/m4v/Big_Buck_Bunny_Trailer_480x270_h264aac.m4v",
                //                ogv: "http://www.jplayer.org/video/ogv/Big_Buck_Bunny_Trailer_480x270.ogv",
                poster: posterUrl //"http://www.jplayer.org/video/poster/Big_Buck_Bunny_Trailer_480x270.png"
            });
        },
        swfPath: "/static/jplayer",
        supplied: "webmv, mp4, ogv, flv",
        size: {
            width: "800px",
            height: "455px",
            cssClass: "jp-video-fuse"
        },        
        cssSelectorAncestor: contSel
    });
}
