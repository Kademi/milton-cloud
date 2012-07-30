var editorSkin = "office2003";

var toolbarSets = {};

toolbarSets["Full"] = [
	['Source','DocProps','-'],
	['Cut','Copy','Paste','PasteText','PasteFromWord','-','Print','SpellChecker'],
	['Undo','Redo','-','Find','Replace','-','SelectAll','RemoveFormat'],
	['Form','Checkbox','Radio','TextField','Textarea','Select','Button','ImageButton','HiddenField'],
	'/',
	['Bold','Italic','Underline','StrikeThrough','-','Subscript','Superscript'],
	['NumberedList','BulletedList','-','Outdent','Indent','Blockquote','CreateDiv'],
	['JustifyLeft','JustifyCenter','JustifyRight','JustifyFull'],
	['Link','Unlink','Anchor'],
	['Image','Flash','Table','Rule','Smiley','SpecialChar','PageBreak'],
	'/',
	['Style','FontFormat','FontName','FontSize'],
	['TextColor','BGColor'],
	['Maximize','ShowBlocks','-','About']		// No comma for the last row.
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
