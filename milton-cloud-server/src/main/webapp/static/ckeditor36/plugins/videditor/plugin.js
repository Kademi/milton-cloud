/* 
 *  FCKPlugin.js for VidEditor
 *  ------------
 */

CKEDITOR.plugins.add( 'videditor', {
    init: function( editor ) {
        editor.addCommand( 'insertVideoDialog', new CKEDITOR.dialogCommand( 'videoDialog' ) );
        //        editor.addCommand( 'insertVidEditor',
        //        {
        //            exec : function( editor )
        //            {    
        //                editor.insertHtml( "<image src='' class='vidplayer' >" );
        //            }
        //        });
        editor.ui.addButton( 'Video', {
            label: 'Insert Video',
            command: 'insertVideoDialog',
            icon: this.path + 'filmreel.gif'
        } );        
        
        console.log("add toolbar");
        
        CKEDITOR.dialog.add( 'videoDialog', function ( editor ) {
            var previewImageId = "previewImageId1";
            return {
                title : 'Video Properties',
                minWidth : 800,
                minHeight : 450,
                onShow : function() {
                    console.log("on show");
                    this.setupContent();
                    console.log("done on show");
                },
 
                contents : [
                {
                    id : 'source',
                    label : 'Basic Settings',
                    elements :
                    [
                    {
                        type : 'hbox',
                        padding : 0,
                        children : [
                        {
                            id : 'txtUrl',
                            type : 'text',
                            label : editor.lang.common.url,
                            required: true,
                            onChange : function()
                            {
                                console.log("onchange");
                                var dialog = this.getDialog(),
                                newUrl = this.getValue();

                                //Update original image
                                if ( newUrl.length > 0 )	//Prevent from load before onShow
                                {
                                    dialog = this.getDialog();
                                    this.preview = CKEDITOR.document.getById( previewImageId );
                                    var original = dialog.originalElement;

                                    dialog.preview.removeStyle( 'display' );

                                    original.setCustomData( 'isReady', 'false' );
                                    // Show loader
                                    var loader = CKEDITOR.document.getById( "" );
                                    if ( loader )
                                        loader.setStyle( 'display', '' );

                                    original.on( 'load', onImgLoadEvent, dialog );
                                    original.on( 'error', onImgLoadErrorEvent, dialog );
                                    original.on( 'abort', onImgLoadErrorEvent, dialog );
                                    original.setAttribute( 'src', newUrl );

                                    // Query the preloader to figure out the url impacted by based href.
                                    previewPreloader.setAttribute( 'src', newUrl );
                                    dialog.preview.setAttribute( 'src', previewPreloader.$.src );
                                    updatePreview( dialog );
                                }
                                // Dont show preview if no URL given.
                                else if ( dialog.preview )
                                {
                                    dialog.preview.removeAttribute( 'src' );
                                    dialog.preview.setStyle( 'display', 'none' );
                                }
                            },
                            setup : function( element )
                            {
                                console.log("setup");
                                var url = element.data( 'videditor-saved-src' ) || element.getAttribute( 'src' );
                                var field = this;

                                this.getDialog().dontResetSize = true;

                                field.setValue( url );		// And call this.onChange()
                                // Manually set the initial value.(#4191)
                                field.setInitValue();
                            },
                            commit : function( type, element )
                            {
                                if ( ( this.getValue() || this.isChanged() ) )
                                {
                                    element.data( 'videditor-saved-src', this.getValue() );
                                    element.setAttribute( 'src', this.getValue() );
                                }
                            },
                            validate : CKEDITOR.dialog.validate.notEmpty( editor.lang.image.urlMissing )
                        },
                        {
                            type : 'button',
                            id : 'browse',
                            // v-align with the 'txtUrl' field.
                            // TODO: We need something better than a fixed size here.
                            style : 'display:inline-block;margin-top:10px;',
                            align : 'center',
                            label : editor.lang.common.browseServer,
                            hidden : true,
                            filebrowser : 'source:txtUrl'
                        }
                        ]
                    }
                    ,

                    {
                        type : 'html',
                        id : 'htmlPreview',
                        style : 'width:95%;',
                        html : "<div style='overflow: auto; width: 100%; height: 400px'><img id='vidEditorPreview' src=''/></div>"
                    }
                                        
                    ] // UI elements of the first tab	will be defined here 
                
                }
                ],
                onOk : function()
                {
                    console.log("onok");
                    var dialog = this;
 
                    //abbr.setAttribute( 'title', dialog.getValueOf( 'tab1', 'title' ) );
                    //abbr.setText( dialog.getValueOf( 'tab1', 'abbr' ) );
                    //var id = dialog.getValueOf( 'tab2', 'id' );
                    editor.insertHtml( '<h2>This is a sample header</h2><p>This is a sample paragraph = ' + dialog.getValueOf( 'tab1', 'title' ) + '</p>' );
                }
            };
        } );

    }
} );
