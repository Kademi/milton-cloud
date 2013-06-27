function initPlugin() {
    log("init");
    $("#myVidTree").mtree({
        basePath: "/organisations/3dn",
        excludedEndPaths: [".mil/"],
        includeContentTypes: ["video"],
        onselect: function(n) {
            log("onselect", n);
            var url = $("#myVidTree").mtree("getSelectedFolderUrl");
            log("onselect", url);
            $("#myVidTree").parent.find(".myUploaded").mupload("setUrl", url);
        },
        onselectFile: function(n, url) {
            log("selected file", n, url);
            playVideo( "#vidContainer .jp-jplayer", url);
                        
        }
    });                
    $("#myVidTree").parent.find(".myUploaded").mupload({
        oncomplete: function(data, name, href) {
            log("oncomplete2", data, name, href);
            $("#myVidTree").mtree("addFile", name, href);
            playVideo( "#vidContainer .jp-jplayer", href);
        }
    });
                
    // Setup the jplayer
    insertJPlayer($("#vidContainer"), null, "640px", "360px", "");
}