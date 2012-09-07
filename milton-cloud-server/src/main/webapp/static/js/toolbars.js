var editorSkin = "office2003";

var toolbarSets = {};

toolbarSets["Full"] = [
	{ name: 'document', items : [ 'Source','-' ] },
	{ name: 'clipboard', items : [ 'Cut','Copy','Paste','PasteText','PasteFromWord','-','Undo','Redo' ] },
	{ name: 'editing', items : [ 'Find','Replace','-','SelectAll','-','SpellChecker' ] },
	{ name: 'forms', items : [ 'Checkbox', 'Radio', 'TextField', 'Textarea', 'Select'] },
        { name: 'links', items : [ 'Link','Unlink','Anchor','Modal' ] },
	{ name: 'insert', items : [ 'Video', 'Image','Table','HorizontalRule','SpecialChar' ] },        
	{ name: 'tools', items : [ 'Maximize', 'ShowBlocks' ] },
	'/',
	{ name: 'styles', items : [ 'Styles','Format','FontSize','Templates' ] },        
	{ name: 'basicstyles', items : [ 'Bold','Italic','Underline','Strike','Subscript','Superscript','-','RemoveFormat' ] },
	{ name: 'paragraph', items : [ 'NumberedList','BulletedList','-','Outdent','Indent','-','Blockquote',
	'-','JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock' ] },
] ;

 

toolbarSets["Balanced"] = [
	['Paste','PasteText','PasteFromWord','-','Print','SpellChecker'],
	['Undo','Redo','-','Find','Replace'],
	['Bold','Italic','Underline','-','Subscript','Superscript','RemoveFormat'],
	['NumberedList','BulletedList','-','Outdent','Indent','Blockquote'],
	['FontFormat','FontSize'],
	['Link','Image','Table','SpecialChar'] // No comma for the last row.
			
] ;

toolbarSets["Default"] = [
	['Source','-'],
	['Cut','Copy','Paste','PasteText','PasteFromWord','-','Print','SpellCheckerer'],
	['Undo','Redo','-','Find','Replace','-','SelectAll','RemoveFormat'],
	['Video','Image','Table','Rule','SpecialChar','PageBreak'],
	['Bold','Italic','Underline','StrikeThrough','-','Subscript','Superscript'],
	['NumberedList','BulletedList','-','Outdent','Indent','Blockquote'],
	['JustifyLeft','JustifyCenter','JustifyRight','JustifyFull'],
	['Link','Unlink','Anchor'],
	['Maximize','ShowBlocks','-','Templates'],		// No comma for the last row.
	['Styles','Format']
] ;



toolbarSets["Lite"] = [
	['Bold','Italic','-','Image','Link','Unlink']
] ;

toolbarSets["BasicAndStyle"] = [
	['FontFormat'],
    '/',
    ['Bold','Italic','-','NumberedList','BulletedList','-','Image','Link','Unlink','-','About']
] ;

toolbarSets["Image"] = [
	['Bold','Italic','-','Link','Unlink','-','Image','Flash']
] ;

toolbarSets["Logo"] = [
	['Bold','Italic','-','Image','-','Source']
] ;
