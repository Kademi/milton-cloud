CKEDITOR.plugins.add( 'embed_video',
{
	init: function( editor )
	{
		var iconPath = this.path + 'images/icon.png';
		var fileIcon = this.path + "images/file.png";
		var folderIcon = this.path + "images/folder.png";
		var rootIcon = this.path + "images/root.png";
		var initialSelect = '';
		var mtype = '';
		var url = '';
		var src = '';
		var jsPath = this.path + "video/js";
 
		editor.addCommand( 'videoDialog', new CKEDITOR.dialogCommand( 'videoDialog' ) );
 
		editor.ui.addButton( 'Video',
		{
			label: 'Insert Video',
			command: 'videoDialog',
			icon: iconPath
		} );
 
		if ( editor.contextMenu )
		{
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
 					return { videoItem : CKEDITOR.TRISTATE_ON };
				return null;
			});
		}

		var e=document.createElement('script');
		e.type='text/javascript';
		e.src=this.path + 'video/js/jquery-1.6.1.min.js';
		document.getElementsByTagName('head')[0].appendChild(e);
		e=document.createElement('script');
		e.type='text/javascript';
		e.src=this.path + 'video/_lib/jquery.js';
		document.getElementsByTagName('head')[0].appendChild(e);
		e=document.createElement('script');
		e.type='text/javascript';
		e.src=this.path + 'video/_lib/jquery.cookie.js';
		document.getElementsByTagName('head')[0].appendChild(e);
		e=document.createElement('script');
		e.type='text/javascript';
		e.src=this.path + 'video/_lib/jquery.hotkeys.js';
		document.getElementsByTagName('head')[0].appendChild(e);
		e=document.createElement('script');
		e.type='text/javascript';
		e.src=this.path + 'video/jquery.jstree.js';
		document.getElementsByTagName('head')[0].appendChild(e);
		e=document.createElement('script');
		e.type='text/javascript';
		e.src=this.path + 'video/js/jquery.jplayer.min.js';
		document.getElementsByTagName('head')[0].appendChild(e);

		editor.element.getDocument().appendStyleSheet(this.path + 'template.css');
 
		CKEDITOR.dialog.add( 'videoDialog', function( editor )
		{
			return {
				title : 'Insert/Edit Video',
				minWidth : 750,
				minHeight : 550,
				contents :
				[
					{
						id : 'video',
						label : 'Insert/Edit Video',
						elements :
						[
							{
								type : 'hbox',
								widths : [ '250px', '500px' ],
								align : 'right',
								children :
								[
									{
										type : 'vbox',
										heights : [ '20px', '450px' ],
										children :
										[
											{
												type : 'html',
												html : '<div>Select a video from server</div>'
											},
											{
												type : 'html',
												html : '<div id="serverTree" class="serverTree" style="height:300px;overflow-y:auto"></div>'
											}
										]
									},
									{
										type : 'vbox',
										heights : ['80px', '450px'],
										children :
										[
											{
												type : 'vbox',
												heights : ['10px', '40px', '40px'],
												children :
												[
													{
														type : 'html',
														html : '<div>Upload a new video</div>'
													},
													{
														type : 'text',
														id : 'filePath'
													},
													{
														type : 'hbox',
														widths : ['80px', '80px', '200px'],
														children :
														[
															{
																type : 'button',
																id : 'addfile',
																onClick : addFiles,
																style : '',
																label : 'Add Files'
															},
															{
																type : 'button',
																id : 'upload',
																onClick : uploadStart,
																style : '',
																label : 'Start Upload'
															},
															{
																type : 'html',
																html : ''
															}
														]
													}
												]
											},
											{
												type : 'html',
												html :	'<div>Preview</div>' +
														'<div id="jp_container_1" class="jp-video jp-video-360p" style="width:500px">' +
														'	<div class="jp-type-single">' +
														'		<div id="jquery_jplayer_1" class="jp-jplayer"></div>' +
														'		<div class="jp-gui">' +
														'			<div class="jp-video-play">' +
														'				<a href="javascript:;" class="jp-video-play-icon" tabindex="1">play</a>' +
														'			</div>' +
														'			<div class="jp-interface">' +
														'				<div class="jp-progress">' +
														'					<div class="jp-seek-bar">' +
														'						<div class="jp-play-bar"></div>' +
														'					</div>' +
														'				</div>' +
														'				<div class="jp-current-time"></div>' +
														'				<div class="jp-duration"></div>' +
														'				<div class="jp-controls-holder">' +
														'					<ul class="jp-controls">' +
														'						<li><a href="javascript:;" class="jp-play" tabindex="1">play</a></li>' +
														'						<li><a href="javascript:;" class="jp-pause" tabindex="1">pause</a></li>' +
														'						<li><a href="javascript:;" class="jp-stop" tabindex="1">stop</a></li>' +
														'						<li><a href="javascript:;" class="jp-mute" tabindex="1" title="mute">mute</a></li>' +
														'						<li><a href="javascript:;" class="jp-unmute" tabindex="1" title="unmute">unmute</a></li>' +
														'						<li><a href="javascript:;" class="jp-volume-max" tabindex="1" title="max volume">max volume</a></li>' +
														'					</ul>' +
														'					<div class="jp-volume-bar">' +
														'						<div class="jp-volume-bar-value"></div>' +
														'					</div>' +
														'					<ul class="jp-toggles">' +
														'						<li><a href="javascript:;" class="jp-full-screen" tabindex="1" title="full screen">full screen</a></li>' +
														'						<li><a href="javascript:;" class="jp-restore-screen" tabindex="1" title="restore screen">restore screen</a></li>' +
														'						<li><a href="javascript:;" class="jp-repeat" tabindex="1" title="repeat">repeat</a></li>' +
														'						<li><a href="javascript:;" class="jp-repeat-off" tabindex="1" title="repeat off">repeat off</a></li>' +
														'					</ul>' +
														'				</div>' +
														'				<div class="jp-title">' +
														'				</div>' +
														'			</div>' +
														'		</div>' +
														'		<div class="jp-no-solution">' +
														'			<span>Update Required</span>' +
														'			To play the media you will need to either update your browser to a recent version or update your <a href="http://get.adobe.com/flashplayer/" target="_blank">Flash plugin</a>.' +
														'		</div>' +
														'	</div>' +
														'</div>'
											},
										]
									}
								]
							},
						]
					}
				],
				onShow : function()
				{
					var sel = editor.getSelection(),
						element = sel.getStartElement();
					if ( element )
						element = element.getAscendant( 'img', true );
 
					if ( !element || element.getName() != 'img' || element.data( 'cke-realelement' ) )
					{
						element = editor.document.createElement( 'img' );
						this.insertMode = true;
					}
					else {
						this.insertMode = false;
						initialSelect = element.getAttribute("id");
						url = element.getAttribute("url");
						mtype = element.getAttribute("mtype");
						src = element.getAttribute("src");
					}
 
					this.element = element;
 
					this.setupContent( this.element );
					$("#jquery_jplayer_1").jPlayer({
						ready: function () {
							$(this).jPlayer("setMedia", {
								m4v: "",
								webmv: "",
								poster: ""
							});
						},
						swfPath: jsPath,
						supplied: "webmv, m4v, ogv",
						size: {
							width: "500px",
							height: "360px",
							cssClass: "jp-video-360p"
						}
					});
					$("#jquery_jplayer_1").jPlayer("play", 0);
					
					$("#serverTree")
						.bind("before.jstree", function (e, data) {
							
						})
						.jstree({ 
							// List of active plugins
							"plugins" : [ 
								"themes","ui","crrm","cookies","dnd","types","hotkeys","contextmenu" 
							],
							// Using types - most of the time this is an overkill
							// read the docs carefully to decide whether you need types
							"types" : {
								// I set both options to -2, as I do not need depth and children count checking
								// Those two checks may slow jstree a lot, so use only when needed
								"max_depth" : -2,
								"max_children" : -2,
								// I want only `drive` nodes to be root nodes 
								// This will prevent moving or creating any other type as a root node
								"valid_children" : [ "drive" ],
								"types" : {
									// The default type
									"file" : {
										// I want this type to have no children (so only leaf nodes)
										// In my case - those are files
										"valid_children" : "none",
										// If we specify an icon for the default type it WILL OVERRIDE the theme icons
										"icon" : {
											"image" : fileIcon
										}
									},
									// The `folder` type
									"folder" : {
										// can have files and other folders inside of it, but NOT `drive` nodes
										"valid_children" : [ "default", "folder" ],
										"icon" : {
											"image" : folderIcon
										}
									}
								}
							},
							// UI & core - the nodes to initially select and open will be overwritten by the cookie plugin

							// the UI plugin - it handles selecting/deselecting/hovering nodes
							"ui" : {
								// this makes the node with ID node_4 selected onload
								"initially_select" : [ initialSelect ]
							}
						})
						.bind("select_node.jstree", function (e, data) { 
							if ( data.rslt.obj[0].getAttribute('rel') == 'file')
							{
								initialSelect = data.rslt.obj[0].id;
								mtype = data.rslt.obj[0].getAttribute('mtype');
								url = data.rslt.obj[0].getAttribute('url');
								src = data.rslt.obj[0].getAttribute('src');
								methodInvoke(mtype, url, src);
							}
						});
				},
				onOk : function()
				{
					var dialog = this,
						img = this.element;
 
					img.setAttribute( "id", initialSelect );
					img.setAttribute( "mtype", mtype );
					img.setAttribute( "url", url );
					img.setAttribute( "src", src );
					if ( this.insertMode ) {
						editor.insertElement( img );
					}
					this.commitContent( img );
				}
			};
		} );
	}
} );

CKEDITOR.config.toolbar_Full =
[
	{ name: 'document',		items : [ 'Source','-','Save','NewPage','DocProps','Preview','Print','-','Templates' ] },
	{ name: 'clipboard',	items : [ 'Cut','Copy','Paste','PasteText','PasteFromWord','-','Undo','Redo' ] },
	{ name: 'editing',		items : [ 'Find','Replace','-','SelectAll','-','SpellChecker', 'Scayt' ] },
	{ name: 'forms',		items : [ 'Form', 'Checkbox', 'Radio', 'TextField', 'Textarea', 'Select', 'Button', 'ImageButton', 'HiddenField' ] },
	'/',
	{ name: 'basicstyles',	items : [ 'Bold','Italic','Underline','Strike','Subscript','Superscript','-','RemoveFormat' ] },
	{ name: 'paragraph',	items : [ 'NumberedList','BulletedList','-','Outdent','Indent','-','Blockquote','CreateDiv','-','JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock','-','BidiLtr','BidiRtl' ] },
	{ name: 'links',		items : [ 'Link','Unlink','Anchor' ] },
	{ name: 'insert',		items : [ 'Image','Flash','Table','HorizontalRule','Smiley','SpecialChar','PageBreak','Iframe' ] },
	'/',
	{ name: 'styles',		items : [ 'Styles','Format','Font','FontSize' ] },
	{ name: 'colors',		items : [ 'TextColor','BGColor' ] },
	{ name: 'tools',		items : [ 'Maximize', 'ShowBlocks','-','About' ] },
	{ name: 'video',		items : [ 'Video' ] }
];

CKEDITOR.config.toolbar = 'Full';

function methodInvoke(mtype, url, src) {
	switch ( mtype )
	{
	case 'm4v':
		$("#jquery_jplayer_1").jPlayer("setMedia", {
			m4v: url,
			poster: src
		});
		break;
	case 'ogv':
		$("#jquery_jplayer_1").jPlayer("setMedia", {
			ogv: url,
			poster: src
		});
		break;
	case 'webmv':
		$("#jquery_jplayer_1").jPlayer("setMedia", {
			webmv: url,
			poster: src
		});
		break;
	default:

	}
	$("#jquery_jplayer_1").jPlayer("play", 0);
}

function addFiles() {
	alert('Add Files');
}

function uploadStart() {
	alert('Start Upload');
}