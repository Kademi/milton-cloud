var initialSelect;
var mtype = '';
var url = '';
var src = '';
var selObj;
var initDone = false;



CKEDITOR.plugins.add( 'image2',
{
    init: function( editor ) {
        log("init image2");
        var iconPath = this.path + 'images/icon.png';
        var jsPath = this.path + "video/javascript";
        var basePath = this.path;
 
        log("vid command");
        editor.addCommand( 'imageDialog', new CKEDITOR.dialogCommand( 'imageDialog' ) );
        log("done command");
 
        editor.ui.addButton( 'Image2', {
            label: 'Insert Image',
            command: 'imageDialog',
            icon: iconPath
        } );
        log("done button", iconPath);
 
        if ( editor.contextMenu ) {
            editor.addMenuGroup( 'imageGroup' );
            editor.addMenuItem( 'imageItem',
            {
                label : 'Edit image',
                icon : iconPath,
                command : 'imageDialog',
                group : 'imageGroup'
            });
            editor.contextMenu.addListener( function( element )
            {
                if ( element )
                    element = element.getAscendant( 'img', true );
                if ( element && !element.isReadOnly() && !element.data( 'cke-realelement' ) )
                    return {
                        imageItem : CKEDITOR.TRISTATE_ON
                    };
                return null;
            });
        }

        // These interfere with page themes, but might need to be put back somehow for editor layout
        // 
        editor.element.getDocument().appendStyleSheet(this.path + 'imagePlugin.css');
        editor.element.getDocument().appendStyleSheet('/static/common/plugin.css');

        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.jstree.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.hotkeys.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.cookie.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.iframe-transport.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/canvas-to-blob.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.fileupload.js'));
        
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.milton-tree.js'));
        CKEDITOR.scriptLoader.load(CKEDITOR.getUrl('/static/js/jquery.milton-upload.js'));        
          
        var pagePath = getFolderPath(window.location.pathname);
        CKEDITOR.dialog.add( 'imageDialog', function( editor ) {
            log("add to editor", editor);
            return {
                title : 'Insert/Edit Image',
                minWidth : 900,
                minHeight : 480,
                contents :
                [
                {
                    id : 'image2',
                    label : 'Insert/Edit Image',
                    elements :
                    [
                    {
                        type : 'html',
                        html: "<div id='imageTree' class='tree'></div><div id='imageUploaded'></div><div class='imageEditor' style='position: absolute; top: 85px; left: 250px'><div id='imageContainer'></div></div>",
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
                        src = "";
                        url = "";
                    } else {
                        this.insertMode = false;
                        var p = getPathFromHref(window.location.href);
                        p = getFolderPath(p);
                        initialSelect = p;
                        mtype = element.getAttribute("mtype");
                        src = element.getAttribute("src");
                        url = src;
                        log("update mode", initialSelect, url, mtype, src);
                    }
 
                    this.element = element;
 
                    this.setupContent( this.element );
                    var imageEditor = $(".imageEditor");
                    var imageCont = imageEditor.find("#imageContainer");
                    var previewImg = $("#imageContainer img");
                    if( previewImg.length == 0 ) {
                        previewImg = $("<img/>");
                        imageCont.prepend(previewImg);
                        imageCont.append(loremIpsum());
                    }
                    var imageFloat = $("#imageUploaded").find("select");
                    if( imageFloat.length == 0 ) {
                        imageFloat = $("<select id='imageFloat'><option value=''>No alignment/float</option><option value='Left'>Align Left</option><option value='Right'>Align Right</option></select>");                        
                        imageFloat.click(function() {
                            previewImg.removeClass("floatLeft").removeClass("floatRight");
                            var val = imageFloat.val();
                            if( val ) {
                                previewImg.addClass("float" + val);
                            }
                        });
                        $("#imageUploaded").append(imageFloat);                        
                    }
                    log("set image source", previewImg, src);
                    previewImg.attr("src", src);
                                        
                    if( !initDone ) {
                        log("init not done, lets do it");
                        initDone = true;
                                                
                        $("#imageTree").mtree({
                            basePath: pagePath,
                            pagePath: "",
                            excludedEndPaths: [".mil/"],
                            includeContentTypes: ["image"],
                            onselectFolder: function(n) {
                                var selectedVideoUrl = $("#imageTree").mtree("getSelectedFolderUrl");
                                log("onselect: folder=", url);
                                $("#imageUploaded").mupload("setUrl", selectedVideoUrl);
                            },
                            onselectFile: function(n, selectedVideoUrl) {
                                url = selectedVideoUrl;
                                log("selected file1", n, url);
                                var img = imageCont.find("img");
                                img.attr("src", url);
                                log("done set img", img);
                            }
                        });                
                        $("#imageUploaded").mupload({
                            buttonText: "Upload image",
                            oncomplete: function(data, name, href) {
                                log("oncomplete", data);
                                $("#imageTree").mtree("addFile", name, href);
                                url = href;
                            }
                        });                
                    }
                },
                onOk : function() {
                    $("#imageContainer .jp-jplayer").jPlayer("stop");
                    var dialog = this,
                    img = this.element;
                    var returnUrl = url;
                    if( returnUrl.startsWith("/")) {
                        returnUrl = returnUrl.substring(pagePath.length+1); // convert to relative path
                    }
                    log("onOk", url, pagePath,"=", returnUrl);
                    img.setAttribute( "src", returnUrl );
                    var imageEditor = $(".imageEditor");
                    var previewImage = imageEditor.find("#imageContainer img");
                    var imageClass = previewImage.attr("class");
                    if( imageClass ) {
                        img.setAttribute("class", imageClass);
                    }
                    if ( this.insertMode ) {
                        log("insert mode", this.insertMode, img);
                        editor.insertElement( img );
                    } else {
                        log("update mode", this.insertMode, img);
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

/**
 * Just returns arbitrary text as paragraphs
 */
function loremIpsum() {    
    return "<p>" +
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec mollis fermentum libero, laoreet sodales enim sagittis at. In in dui a purus pharetra semper. Sed tincidunt varius lorem quis iaculis. Fusce placerat tellus eget mauris ultricies bibendum vestibulum diam lobortis. Donec in lacus ante, ac euismod lacus. Donec nibh sem, vehicula non eleifend non, posuere et enim. Curabitur venenatis eros in orci semper vehicula. Morbi venenatis lectus at tellus mollis quis porttitor purus vehicula." +
    "</p>" +
    "<p>" +
    "Vivamus nibh elit, convallis vitae iaculis a, iaculis nec libero. Nulla diam lacus, ornare sed semper ac, faucibus eget neque. Sed ultricies erat vestibulum tortor bibendum iaculis. Sed consectetur nisl eu leo pharetra euismod. Pellentesque sed metus ligula. Vestibulum vel enim erat. Donec felis neque, gravida laoreet lacinia at, fringilla nec erat." +
    "</p>" +
    "<p>" +
    "Fusce nec eros vel dolor iaculis fringilla. Suspendisse a felis enim, at iaculis arcu. Suspendisse vel nunc nec lorem suscipit mollis. In non nisl in velit rutrum commodo. Etiam bibendum ante non velit posuere tempus sodales turpis consequat. Nulla dapibus dignissim erat, a pretium massa tincidunt vel. Integer sit amet lacinia lectus. Pellentesque felis felis, aliquam eu laoreet et, facilisis non orci. Integer ligula lorem, dictum ut sodales eget, pretium quis ligula. Vivamus tincidunt." +
    "</p>";
}