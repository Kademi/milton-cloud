function initJPlayer(height, width, cssClass) {
    var images = $("img.video");
    replaceImagesWithJPlayer(images, height, width, cssClass);
}

function replaceImagesWithJPlayer(images, height, width, cssClass) {
    images.each(function(i, n) {
        var img = $(n);
        var posterUrl = img.attr("src");
        var src = getFolderPath(posterUrl);        
        log("item", img, i, src);
        buildJPlayer(height, width, cssClass, img, i+10, src);
    });
}

function buildJPlayer(height, width, cssClass, itemToReplace, count, src) {
    var div = buildJPlayerContainer(count);
    itemToReplace.replaceWith(div);    
    
    //initJPlayer("360", "640", "jp-video-360p", $("#jquery_jplayer_1"), "jp_container_1");
    var playerDiv = div.find(".jp-jplayer");
    log("player div", playerDiv, "parent div", div);
    if( src ) {
        loadJPlayer(height, width, cssClass, playerDiv, "jp_container_" + count, src);
    }
}

function loadJPlayer(height, width, cssClass, jplayerDiv, containerId, src) {
    log("loadJPlayer", jplayerDiv, src);
    // We need src to be absolute for flash player
    if( src && !src.startsWith("/")) {
        var folderPath = getFolderPath(window.location.pathname);
        src = folderPath + "/" + src;
        log("converted to absolute path", src);
    }

    var basePath = src + "/alt-640-360.";
    jplayerDiv.jPlayer({
        ready: function () {
            log("setMedia", this);
            $(this).jPlayer("setMedia", {
                m4v: basePath + "m4v",
                webmv: basePath + "webm",
                poster: basePath + "png"
            });
            log("done set media");
        },
        swfPath: "/static/jplayer",
        solution: "html, flash",
        supplied: "webmv, m4v",
        cssSelectorAncestor: "#" + containerId,
        size: {
            width: width + "px",
            height: height + "px",
            cssClass: cssClass
        }
    });    

}


function buildJPlayerContainer(count) {
    var c = "<div id='jp_container_" + count + "' class='jp-video jp-video-360p'>" +
    "<div id='jquery_jplayer_" + count + "' class='jp-jplayer'></div>" +
    "<div class='jp-gui'>" +
    "<div class='jp-interface'>" +
    "<div class='jp-controls-holder'>" +
    "<a href='javascript:;' class='jp-play' tabindex='1'>play</a>" +
    "<a href='javascript:;' class='jp-pause' tabindex='1'>pause</a>" +
    "<span class='separator sep-1'></span>" +
    "<div class='jp-progress'>" +
    "<div class='jp-seek-bar'>" +
    "<div class='jp-play-bar'><span></span></div>" +
    "</div>" +
    "</div>" +
    "<div class='jp-current-time'></div>" +
    "<span class='time-sep'>/</span>" +
    "<div class='jp-duration'></div>" +
    "<span class='separator sep-2'></span>" +
    "<a href='javascript:;' class='jp-mute' tabindex='1' title='mute'>mute</a>" +
    "<a href='javascript:;' class='jp-unmute' tabindex='1' title='unmute'>unmute</a>" +
    "<div class='jp-volume-bar'>" +
    "<div class='jp-volume-bar-value'><span class='handle'></span></div>" +
    "</div>" +
    "<span class='separator sep-2'></span>" +
    "<a href='javascript:;' class='jp-full-screen' tabindex='1' title='full screen'>full screen</a>" +
    "<a href='javascript:;' class='jp-restore-screen' tabindex='1' title='restore screen'>restore screen</a>" +
    "</div>" +
    "</div>" +
    "</div>" +
    "<div class='jp-no-solution'>" +
    "<span>Update Required</span>" +
    "Here's a message which will appear if the video isn't supported. A Flash alternative can be used here if you fancy it." +
    "</div>" +
    "</div>";
    return $(c);
}