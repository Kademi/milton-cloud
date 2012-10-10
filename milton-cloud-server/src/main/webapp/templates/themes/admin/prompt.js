/** Start prompt.js */

var myPromptModal;

/**
 *  Show a styled prompt. On successful validation the callback function is called
 *  with the form as the argument. The callback should return true to close the
 *  form
 */
function myPrompt(id, url, title, instructions, caption, buttonName, buttonText, inputClass, inputPlaceholder, callback) {
    log("myPrompt");
    var existing = $("#" + id);
    if(existing ) {
        existing.remove();
    }
    var header = $("<header><h3></h3><a title='Close' href='#' class='Close'><span class='Hidden'>Close</span></a></header>");
    var inner = $("<div class='ModalContent'><form method='POST'><div class='pageMessage'></div></form></div>");
    var notes = $("<p></p>");
    var table = $("<table><tbody></tbody></table>");
    var row1 = $("<tr><th><label for=''></label></th><td><input type='text' class='required'/></td></tr>");
    var row2 = $("<tr><td class='BtnBar' colspan='2'><button class='Btn'>Save</button></td></tr>");
    
    myPromptModal = $("<div class='Modal'></div>");
    myPromptModal.attr("id", id);
    myPromptModal.append(header);
    myPromptModal.append(inner);
    
    notes.html(instructions);
    inner.find("form").append(notes).append(table);
    table.append(row1);
    table.append(row2);
    
    header.find("h3").text(title);
    var inputId = id + "_";
    row1.find("input").addClass(inputClass);    
    row1.find("input").attr("name", buttonName).attr("id", inputId).attr("placeholder", inputPlaceholder);    
    row1.find("label").attr("for", inputId).text(caption);
    row2.find("button").text(buttonText);
    
    var form = inner.find("form");
    form.attr("action", url);
    form.submit(function(e) {
        e.preventDefault();
        resetValidation(form);
        if( checkRequiredFields(form)) {
            var newName = form.find("input").val();
            if( callback(newName, form) ) {
                $.tinybox.close();
                myPromptModal.remove();
            }
        }
    });

    $("body").append(myPromptModal);

    $.tinybox.show(myPromptModal, {
        overlayClose: false,
        opacity: 0
    });
}

function closeMyPrompt() {
    $.tinybox.close();
    myPromptModal.remove();
}

/** End theme.js */