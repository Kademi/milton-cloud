var initialSelect;
var mtype = '';
var url = '';
var src = '';
var selObj;
var initDone = false;



CKEDITOR.plugins.add( 'embed_video',
{
    init: function( editor ) {
        log("init embed_video");
        var iconPath = this.path + 'images/icon.png';
        var jsPath = this.path + "video/javascript";
        var basePath = this.path;
 
        log("vid command");
        editor.addCommand( 'videoDialog', new CKEDITOR.dialogCommand( 'videoDialog' ) );
        log("done command");
 
        editor.ui.addButton( 'Video', {
            label: 'Insert Video',
            command: 'videoDialog',
            icon: iconPath
        } );
        log("done button", iconPath);
 
        if ( editor.contextMenu ) {
            editor.addMenuGroup( 'videoGroup' );
            editor.addMenuItem( 'videoItem',
            {
                label : 'Edit Video',
                icon : iconPath,
                command : 'videoDialog',
                group : 'videoGroup'
            });
            editor.contextMenu.addListener( function( element )
            {
                if ( element )
                    element = element.getAscendant( 'img', true );
                if ( element && !element.isReadOnly() && !element.data( 'cke-realelement' ) )
                    return {
                        videoItem : CKEDITOR.TRISTATE_ON
                    };
                return null;
            });
        }

        // These interfere with page themes, but might need to be put back somehow for editor layout
        // 
        editor.element.getDocument().appendStyleSheet(this.path + 'template.css');
        //        editor.element.getDocument().appendStyleSheet("/static/common/bootstrap.min.css");
        //        editor.element.getDocument().appendStyleSheet("/static/common/bootstrap-responsive.min.css");
        //        editor.element.getDocument().appendStyleSheet("/static/common/bootstrap-image-gallery.min.css");

        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.jstree.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.hotkeys.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.cookie.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.iframe-transport.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/canvas-to-blob.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.fileupload.js'));
        
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/jplayer/jquery.jplayer.min.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.milton-tree.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.milton-upload.js'));        
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/jplayer/x.jplayer.init.js'));        
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('plugins/embed_video/controller.js'));        
  
        
        /*CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('plugins/embed_video/video/upload/js/vendor/jquery.ui.widget.js'));
		CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('plugins/embed_video/video/upload/js/tmpl.min.js'));
		CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('plugins/embed_video/video/upload/js/load-image.min.js'));
		CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('plugins/embed_video/video/upload/js/canvas-to-blob.min.js'));
		CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('plugins/embed_video/video/upload/js/bootstrap.min.js'));
		CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('plugins/embed_video/video/upload/js/bootstrap-image-gallery.min.js'));
		CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('plugins/embed_video/video/upload/js/jquery.iframe-transport.js'));
		CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('plugins/embed_video/video/upload/js/canvas-to-blob.js'));
		CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('plugins/embed_video/video/upload/js/jquery.fileupload.js'));*/
 
        var pagePath = getFolderPath(window.location.pathname);
        CKEDITOR.dialog.add( 'videoDialog', function( editor ) {
            log("add to editor", editor);
            return {
                title : 'Insert/Edit Video',
                minWidth : 900,
                minHeight : 480,
                contents :
                [
                {
                    id : 'video',
                    label : 'Insert/Edit Video',
                    elements :
                    [
                    {
                        type : 'html',
                        html: "<div id='myTree'></div><div id='myUploaded'></div><div class='vidEditor' style='position: absolute; top: 85px; left: 250px'><div id='vidContainer' class='jp-video'></div></div>",
                        commit: function(data) {
                            log("commit, data=", data);
                        }
                    }
                    ]
                }
                ],
                onShow : function() {
                    
                    var sel = editor.getSelection();
                    var element = sel.getStartElement();
                    log("onShow", sel, element, editor, editor.getData());
                    if ( element ) {
                        element = element.getAscendant( 'img', true );
                        log("onShow2", element);
                    }
                    
                    if ( !element || element.getName() != 'img' || element.data( 'cke-realelement' ) ) {
                        element = editor.document.createElement( 'img' );
                        this.insertMode = true;
                    } else {
                        this.insertMode = false;
                        var p = getPathFromHref(window.location.href);
                        p = getFolderPath(p);
                        initialSelect = p;
                        mtype = element.getAttribute("mtype");
                        src = element.getAttribute("src");
                        url = getFolderPath(src); // the url of the video is the parent of the preview image
                        log("update mode", initialSelect, url, mtype, src);
                    }
 
                    this.element = element;
 
                    this.setupContent( this.element );
                    if( !initDone ) {
                        initDone = true;                        
                        $("#myTree").mtree({
                            basePath: pagePath,
                            pagePath: "",
                            excludedEndPaths: [".mil/"],
                            includeContentTypes: ["video"],
                            onselectFolder: function(n) {
                                var selectedVideoUrl = $("#myTree").mtree("getSelectedFolderUrl");
                                log("onselect: folder=", url);
                                $("#myUploaded").mupload("setUrl", selectedVideoUrl);
                            },
                            onselectFile: function(n, selectedVideoUrl) {
                                url = selectedVideoUrl;
                                log("selected file", n, url);                                
                                //playVideo( "#vidContainer .jp-jplayer", url);
                                //loadJPlayer("360", "640", "jp-video-360p", $(".vidEditor div.jp-video"), 100, url);
                                buildJPlayer("360", "640", "jp-video-360p", $(".vidEditor div.jp-video"), 100, url);
                            }
                        });                
                        $("#myUploaded").mupload({
                            oncomplete: function(data, name, href) {
                                log("oncomplete", data);
                                $("#myTree").mtree("addFile", name, href);
                                url = href;
                                //playVideo( "#vidContainer .jp-jplayer", href);
                                //loadJPlayer("360", "640", "jp-video-360p", $(".vidEditor div.jp-video"), 100, href);
                                buildJPlayer("360", "640", "jp-video-360p", $(".vidEditor div.jp-video"), 100, href);
                            }
                        });
                
                        // Setup the jplayer
                        //insertJPlayer($("#vidContainer"), null, null, "360px","640px", "");
                        //buildJPlayer("360", "640", "jp-video-360p", $("#vidContainer"), 100, null);
                    }
                },
                onOk : function() {
                    $("#vidContainer .jp-jplayer").jPlayer("stop");
                    var dialog = this,
                    img = this.element;
                    var returnUrl = url;
                    returnUrl = returnUrl.substring(pagePath.length+1);
                    log("onOk", url, pagePath,"=", returnUrl);
                    img.setAttribute( "src", returnUrl + "/alt-640-360.png" );
                    img.setAttribute("class", "video");
                    if ( this.insertMode ) {
                        log("inserted")
                        editor.insertElement( img );
                    } else {
                        editor.updateElement();
                    }
                    this.commitContent( img );
                    log("commit content", img);
                }
            };
        } );
        log("done init");
    }
} );
