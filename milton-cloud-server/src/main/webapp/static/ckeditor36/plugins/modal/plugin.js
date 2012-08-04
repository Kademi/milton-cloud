(function(CKEDITOR) {
var defaults_width = 350;

CKEDITOR.plugins.add('modal', {
	init: function(editor) {
		editor.addCommand('modalLinkDialog', new CKEDITOR.dialogCommand('modalLinkDialog'));
			
		editor.ui.addButton('Modal', {
			label: 'Insert modal',
			command: 'modalLinkDialog',
			icon: this.path + 'images/modal.png'
		});		
		
		CKEDITOR.dialog.add('modalLinkDialog', function(editor) {
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
						label: 'Displayed Text<sup style="color: #ff4000">*</sup>',
						validate: CKEDITOR.dialog.validate.notEmpty('The Displayed Text cannot be empty!'),
						required: true,
						commit: function(data) {
							data.text = this.getValue();
						}
					}, {
						type: 'textarea',
						id: 'content',
						label: 'Modal Content<sup style="color: #ff4000">*</sup>',
						validate: CKEDITOR.dialog.validate.notEmpty('The Modal Content cannot be empty!'),
						required: true,
						commit: function(data) {
							data.content = this.getValue();
						}
					}, {
						type: 'text',
						id: 'width',
						label: 'Width',
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
				onOk: function() {
					// Create a link element and an object that will store the data entered in the dialog window.
					// http://docs.cksource.com/ckeditor_api/symbols/CKEDITOR.dom.document.html#createElement
					var dialog = this,
						data = {},
						id = 'modal_' + Math.round(Math.random() * 1000000).toString() + '_' + Date.now(),
						link = editor.document.createElement('a'),
						div = editor.document.createElement('div');
					
					this.commitContent(data);
					
					link.setHtml(data.text);	
					link.setAttributes({
						'href': '#' + id,
						'class': 'anchor-modal'
					});
					
					var style = 'display: none;',
						width = (data.width || defaults_width) - 50; // Subtract 25px from padding left and right
					
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
			};
		} );
	}
});


}(CKEDITOR));