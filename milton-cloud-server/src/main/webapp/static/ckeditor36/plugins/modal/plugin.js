(function(CKEDITOR) {
var required_string = '<sup style="color: #ff4000">*</sup>';

CKEDITOR.plugins.add('modal', {
	init: function(editor) {
		editor.addCommand('insertModalLink', new CKEDITOR.dialogCommand('modalLinkDialog'));
			
		editor.ui.addButton('Modal', {
			label: 'Insert modal',
			command: 'insertModalLink',
			icon: this.path + 'images/modal.png'
		});
		
		 editor.on('selectionChange', function(evt) {
			if(editor.readOnly) {
				return;
			}
			
			var command = editor.getCommand('insertModalLink'),
				element = evt.data.path.lastElement && evt.data.path.lastElement.getAscendant( 'a', true );
			
			if(element && element.getName() === 'a' && element.getAttribute('href') && element.getChildCount() && element.$.className === 'anchor-modal') {
				command.setState(CKEDITOR.TRISTATE_ON);
			} else {
				command.setState(CKEDITOR.TRISTATE_OFF);
			}
		});
		
		editor.on('doubleclick', function(evt) {
			var element = CKEDITOR.plugins.link.getSelectedLink(editor) || evt.data.element;

			if(!element.isReadOnly()) {
				if (element.is('a') && element.$.className === 'anchor-modal') {
					evt.data.dialog = 'modalLinkDialog';
					editor.getSelection().selectElement(element);
				}
			}
		});
		
		CKEDITOR.dialog.add('modalLinkDialog', function(editor) {
			var default_width = 350;
			
			var parseModalLink = function(editor, element) {
				var href = element.getAttribute('href').replace('#',''),
					modal = editor.document.getById(href);
				
				this._.selectedElement = element;
				
				return {
					text: element.getHtml(),
					content: modal.getHtml(),
					width: modal.getStyle('width').replace('px', ''),
					height: modal.getStyle('height').replace('px', '')
				};
			};
			
			return {
				title: 'Modal Properties',
				minWidth: 400,
				minHeight: 250,
				contents: [{
					id: 'general',
					label: 'Settings',
					elements: [{
						type: 'text',
						id: 'text',
						label: 'Displayed Text' + required_string,
						validate: CKEDITOR.dialog.validate.notEmpty('The Displayed Text cannot be empty!'),
						required: true,
						setup: function(data) {
							if(data.text) {
								this.setValue(data.text);
							}
						},
						commit: function(data) {
							data.text = this.getValue();
						}
					}, {
						type: 'textarea',
						id: 'content',
						label: 'Modal Content' + required_string,
						validate: CKEDITOR.dialog.validate.notEmpty('The Modal Content cannot be empty!'),
						required: true,
						setup: function(data) {
							if(data.content) {
								this.setValue(data.content);
							}
						},
						commit: function(data) {
							data.content = this.getValue();
						}
					}, {
						type: 'text',
						id: 'width',
						label: 'Width',
						setup: function(data) {
							if(data.width) {
								this.setValue(+data.width + 50); // Add 50px of padding left and right
							}
						},
						validate: function() {
							var value = this.getValue();
							
							if(this.getValue()) {
								if(isNaN(value)) {
									alert('The Width must be digits!');
									return false;
								}
							}
						},
						commit: function(data) {
							data.width = this.getValue();
						}
					}, {
						type: 'text',
						id: 'height',
						label: 'Height',
						setup: function(data) {
							if(data.height) {
								this.setValue(data.height);
							}
						},
						validate: function() {
							var value = this.getValue();
							
							if(this.getValue()) {
								if(isNaN(value)) {
									alert('The Height must be digits!');
									return false;
								}
							}
						},
						commit: function(data) {
							data.height = this.getValue();
						}
					}]
				}],
				onShow: function() {
					var editor = this.getParentEditor(),
						selection = editor.getSelection(),
						element = null,
						text = selection.getSelectedText();
					
					if((element = CKEDITOR.plugins.modalLink.getSelectedLink(editor)) && element.hasAttribute('href')) {
						selection.selectElement( element );
					} else {
						element = null;		
					}
					
					if(element) {					
						this.setupContent(parseModalLink.apply(this,[editor,element]));
					} else {
						if(text) {
							this.setupContent({
								text: text
							})
						}
					}
				},
				onOk: function() {
					var dialog = this,
						data = {},
						id;						
					
					this.commitContent(data);					
					
					if(this._.selectedElement) {
						var target = this._.selectedElement,
							modal = editor.document.getById(target.getAttribute('href').replace('#', '')),
							style = 'display: none;',
							width = (data.width || default_width) - 50; // Subtract 25px from padding left and right
						
						style += 'width: ' + width + 'px;';
						
						if(data.height) {
							style += 'height: ' + data.height + 'px;';
						}
						
						// Set html for anchor
						target.setHtml(data.text);
						
						modal.setHtml(data.content);
						modal.setAttribute('style', style);						
					} else {					
						var link = editor.document.createElement('a'),
							div = editor.document.createElement('div');
							
						id = 'modal_' + Math.round(Math.random() * 1000000).toString() + '_' + Date.now();
							
						link.setHtml(data.text);	
						link.setAttributes({
							'href': '#' + id,
							'class': 'anchor-modal'
						});
						
						var style = 'display: none;',
							width = (data.width || this.defaults.width) - 50; // Subtract 25px from padding left and right						
						style += 'width: ' + width + 'px;';						
						if(data.height) {
							style += 'height: ' + data.height + 'px;';
						}						
						div.setHtml(data.content);	
						div.setAttributes({
							'id': id,
							'style': style,
							'class': 'linked-modal'
						});
						
						editor.insertElement(link);
						editor.insertElement(div);
					}
				}
			};
		});
	}
});

CKEDITOR.plugins.modalLink = {
	getSelectedLink: function(editor) {
		try {
			var selection = editor.getSelection();
			if ( selection.getType() == CKEDITOR.SELECTION_ELEMENT ) {
				var selectedElement = selection.getSelectedElement();
				if ( selectedElement.is( 'a' ) && selectedElement.$.className === 'anchor-modal' )
					return selectedElement;
			}

			var range = selection.getRanges( true )[ 0 ];
			range.shrink( CKEDITOR.SHRINK_TEXT );
			var root = range.getCommonAncestor();
			return root.getAscendant( 'a', true );
		}
		catch( e ) { return null; }
	}
};


}(CKEDITOR));