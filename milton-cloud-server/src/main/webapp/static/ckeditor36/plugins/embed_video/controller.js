function initPlugin() {
    log("init");
    $("#myTree").mtree({
        basePath: "/organisations/3dn",
        excludedEndPaths: [".mil/"],
        includeContentTypes: ["video"],
        onselect: function(n) {
            log("onselect", n);
            var url = $("#myTree").mtree("getSelectedFolderUrl");
            log("onselect", url);
            $("#myUploaded").mupload("setUrl", url);
        },
        onselectFile: function(n, url) {
            log("selected file", n, url);
            playVideo( "#vidContainer .jp-jplayer", url);
                        
        }
    });                
    $("#myUploaded").mupload({
        oncomplete: function(data, name, href) {
            log("oncomplete", data);
            $("#myTree").mtree("addFile", name, href);
            playVideo( "#vidContainer .jp-jplayer", href);
        }
    });
                
    // Setup the jplayer
    insertJPlayer($("#vidContainer"), null, "640px", "360px", "");
}