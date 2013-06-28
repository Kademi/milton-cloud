(function(CKEDITOR) {
var required_string = '<sup style="color: #ff4000">*</sup>',
	placeholder = '<div class="button-placeholder">Please right click to add button at here.</div>';

CKEDITOR.plugins.add('classifer', {
	init: function(editor) {		
		// Add Classifer button
		editor.ui.addButton('Classifer', {
			label: 'Insert classifer',
			command: 'insertClassifer',
			icon: this.path + 'images/classifer.png'
		});
		editor.addCommand('insertClassifer', {
			exec: function(editor) {				
				var classifer = editor.document.createElement('div'),
					id = 'classifer_' + Math.round(Math.random() * 1000000).toString() + '_' + Date.now();
					
					classifer.setAttributes({
						'id': id,
						'class': 'classifer clearfix'
					});
					classifer.setHtml('\
						<div class="controller-wrapper">' + placeholder + '</div>\
						<div class="content-wrapper">\
							<div class="content">\
								<div class="main-content">\
									<h3 class="notice">Roll over a button on the left for more information.</h3>\
								</div>\
							</div>\
						</div>\
					');
					
					editor.insertElement(classifer);
			}
		});
		
		// If the "menu" plugin is loaded, register the menu items.
		if(editor.addMenuItems) {
			editor.addMenuGroup('classifer');
			
			editor.addMenuItems({
				addClassiferItem: {
					label : 'Add Classifer Item',
					command : 'addClassiferItem',
					group : 'classifer',
					icon: this.path + 'images/add_item.png'
				},
				editClassiferItem : {
					label : 'Edit Classifer Item',
					command : 'editClassiferItem',
					group : 'classifer',
					icon: this.path + 'images/edit_item.png'
				},
				deleteClassiferItem : {
					label : 'Delete Classifer Item',
					command : 'deleteClassiferItem',
					group : 'classifer',
					icon: this.path + 'images/delete_item.png'
				}
			});
		}
		
		var selected_classifer_id = '',
			index_selected_controller, 
			index_selected_content,
			is_selected_item = false;

		// If the "contextmenu" plugin is loaded, register the listeners.
		if(editor.contextMenu) {
			editor.contextMenu.addListener(function(element,selection){				
				if(!element || element.isReadOnly()) {
					return null;
				}
				
				is_selected_item = false;

				var elementPath = new CKEDITOR.dom.elementPath(element),
					blockLimit = elementPath.blockLimit;

				if(blockLimit && blockLimit.getAscendant('div', true) && (blockLimit.$.className === 'controller-wrapper' || blockLimit.$.className === 'content-wrapper')) {
					// Id of classifer container
					selected_classifer_id = blockLimit.getParent().getId();	

					return {
						addClassiferItem: CKEDITOR.TRISTATE_OFF
					};
				}			
				
				if(blockLimit && blockLimit.getAscendant('div', true) && blockLimit.$.className === 'controller') {
					// Id of classifer container
					selected_classifer_id = blockLimit.getParent().getParent().getId();
					// Index of selected controller
					index_selected_controller = blockLimit.getIndex();
					
					is_selected_item = true;

					return {
						editClassiferItem: CKEDITOR.TRISTATE_OFF,
						deleteClassiferItem: CKEDITOR.TRISTATE_OFF
					};
				}
				
				if(blockLimit && blockLimit.getAscendant('div', true) && blockLimit.$.className === 'content') {
					// Id of classifer container
					selected_classifer_id = blockLimit.getParent().getParent().getId();
					// Index of selected content
					index_selected_content = blockLimit.getIndex();
					
					is_selected_item = true;

					return {
						editClassiferItem: CKEDITOR.TRISTATE_OFF,
						deleteClassiferItem: CKEDITOR.TRISTATE_OFF
					};
				}
			});
		}		
		
		// Add Classifer Item function
		editor.addCommand('addClassiferItem', new CKEDITOR.dialogCommand('addClassiferItemDialog'));
		CKEDITOR.dialog.add('addClassiferItemDialog', function(editor) {
			return {
					title: 'Classifer Item Properties',
					minWidth: 400,
					minHeight: 300,
					contents: [{
						id: 'general',
						label: 'Settings',
						elements: [{
							type: 'text',
							id: 'title',
							label: 'Title' + required_string,
							validate: CKEDITOR.dialog.validate.notEmpty('The Title cannot be empty!'),
							required: true,
							commit: function(data) {
								data.title = this.getValue();
							}
						}, {
							type: 'text',
							id: 'annotation1',
							label: 'Annotation #1',
							commit: function(data) {
								data.annotation1 = this.getValue();
							}
						}, {
							type: 'text',
							id: 'annotation2',
							label: 'Annotation #2',
							commit: function(data) {
								data.annotation2 = this.getValue();
							}
						}, {
							type: 'file',
							id: 'image',
							label: 'Image background',
							commit: function(data) {
								data.image = this.getValue();
							}
						}, {
							type: 'textarea',
							id: 'content',
							label: 'Content' + required_string,
							validate: CKEDITOR.dialog.validate.notEmpty('The Content cannot be empty!'),
							required: true,
							commit: function(data) {
								data.content = this.getValue();
							}
						}]
					}],
					onShow: function() {
						var editor = this.getParentEditor(),
							selection = editor.getSelection(),
							element = null;
					},
					onOk: function() {
						var dialog = this,
							data = {},
							classifer_container = editor.document.getById(selected_classifer_id),
							classifer_controller_wrapper = classifer_container.getChildren().getItem(0),
							first_classifer_controller = classifer_controller_wrapper.getChildren().getItem(0),
							classifer_content_wrapper = classifer_container.getChildren().getItem(1);
						
						this.commitContent(data);
						
						// Button
						var new_button = editor.document.createElement('div');
						new_button.setAttribute('class', 'controller');
						// Title button markup
						var title_button = '<h3>' + data.title + '</h3>'
						// Annotation markup
						var annotation1 = data.annotation1 || '',
							annotation2 = data.annotation2 ||'',
							annotation = '<p>' + annotation1 + '<br />' + annotation2 + '</p>';
						// Arrow markup
						var arrow = '<span class="arrow hide-text">&gt;</span>';						
						// Fill markup into button
						new_button.setHtml(title_button + annotation + arrow);
						
						// If have a placeholder, will remove the placeholder
						if(first_classifer_controller.getAttribute('class') === 'button-placeholder') {
							classifer_controller_wrapper.setHtml('');
						}
						
						classifer_controller_wrapper.append(new_button);
						
						// Content
						var new_content = editor.document.createElement('div');
						new_content.setAttribute('class', 'content');
						// Image background markup
						var image_background = '';
						if(data.image) {
							image_background = '<img src="' + data.image + '" />';
						}
						// Set content
						new_content.setHtml(image_background + '<div class="main-content">' + data.content + '</div>');
						
						classifer_content_wrapper.append(new_content);						
					}
				};
		});
		
		//	
		editor.addCommand('editClassiferItem', {
			exec: function() {
				
			}
		});
		
		//	
		editor.addCommand('deleteClassiferItem', {
			exec: function(editor) {
				if(is_selected_item) {
					// Index of content is always greater than index of controller `1`
					var _content_index = -1, 
						_controller_index = -1;
					if(!isNaN(index_selected_content)) {
						_content_index = +index_selected_content;
						_controller_index = _content_index - 1;
					}
					if(!isNaN(index_selected_controller)) {
						_controller_index = +index_selected_controller;
						_content_index = _controller_index + 1;
					}
					
					var classifer_container = editor.document.getById(selected_classifer_id),
						classifer_controller_wrapper = classifer_container.getChildren().getItem(0),
						classifer_content_wrapper = classifer_container.getChildren().getItem(1);
					
					// Remove selected controller and  selected content
					classifer_controller_wrapper.getChildren().getItem(_controller_index).remove();
					
					classifer_content_wrapper.getChildren().getItem(_content_index).remove();
					
					var first_classifer_controller = classifer_controller_wrapper.getChildren().getItem(0);
					if(!first_classifer_controller) {
						classifer_controller_wrapper.setHtml(placeholder);
					}
				}
			}
		});
	}
});


}(CKEDITOR));