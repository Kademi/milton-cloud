/**
 *
 *  jquery.forms.js
 *  
 *  Depends on common.js
 *  
 *  The target should be a 
 * 
 */

(function( $ ) {
    $.fn.forms = function(options) {
        log("init forms plugin", this);
        
        var config = $.extend( {
            callback: function() {
                
            }
        }, options);  
  
        var container = this;
        $(this).submit(function(e) {
            log("form submit", form);
            e.preventDefault();
            var form = $(this);            
            resetValidation(container);
            if( checkRequiredFields(container) ) {
                try {                    
                    $.ajax({
                        type: 'POST',
                        url: form.attr("id"),
                        data: form.serialize(),
                        dataType: "json",
                        success: function(resp) {
                            ajaxLoadingOff();
                            log("save success", resp)
                            config.callback(resp)
                        },
                        error: function(resp) {
                            ajaxLoadingOff();
                            alert("err");
                            $(config.valiationMessageSelector, container).text(config.loginFailedMessage);
                            log("set message", $(config.valiationMessageSelector, this), config.loginFailedMessage);
                            $(config.valiationMessageSelector, container).show(100);
                        }
                    });                
                } catch(e) {
                    log("exception sending forum comment", e);
                }            
            } else {
                log("form is not valid");
            }
            return false;
        });    
    };
})( jQuery );


function resetValidation(container) {
    $(".validationError", container).remove();
    $(".pageMessage", container).hide(300);
    $(".error > *", container).unwrap();
    $(".errorField", container).removeClass("errorField");
}

function checkRequiredFields(container) {
    log('checkRequiredFields', container);
    var isOk = true;

    // Check mandatory
    $(".required", container).each( function(index, node) {
        var val = $(node).val();

        var title = $(node).attr("title");
        if( !val || (val == title) ) { // note that the watermark can make the value == title
            log('error field', node);
            showErrorField($(node));
            isOk = false;
        }
    });

    if( !checkRequiredChecks(container) ) {
        log('missing required checkboxs');
        isOk = false;
    }

    if( isOk ) {
        isOk = checkValidEmailAddress(container);
        if( !checkDates(container)) {
            isOk = false;
        }
        if( !checkValidPasswords(container) ) {
            isOk = false;
        }
        log('will check length');
        if( !checkValueLength($("#firstName", container), 1, 15, "First name" ) ) {
            isOk = false;
        }
    } else {
        showMessage("Please enter all required values", container);
    }
    return isOk;
}
function checkRequiredChecks(container) {
    var isOk = true;
    $("input.required:checkbox", container).not(":checked").each( function(index, node) {
        node = $(node);
        node = $("label[for=" + node.attr("id") + "]");
        showErrorField($(node));
        isOk = false;
    });
    return isOk;
}


function checkRadio(radioName, container) {
    log('checkRadio', radioName, container);
    if( $("input:radio[name=" + radioName + "]:checked", container).length == 0 ) {
        var node = $("input:radio[name=" + radioName + "]", container)[0];
        node = $(node);
        node = $("label[for=" + node.attr("id") + "]");
        log('apply error to label', node);
        showValidation(node, "Please select a value for " + radioName, container );
        return false;
    } else {
        return true;
    }
}

// depends on common.js
function checkDates(container) {
    isOk = true;
    $("input", container).each( function(index, node) {
        var id = $(node).attr("id");
        if( id && id.contains("Date")) {
            var val = $(node).val();
            log('val');
            if( val && val.length > 0) {
                if( !isDate(val)) {
                    showValidation($(node), "Please enter a valid date", container );
                    isOk = false;
                }
            }
        }
    });
    return isOk;
}
/**
 *  If password is present, checks for validity
 */
function checkValidPasswords(container) {
    var target = $("#password",container);
    var p1 = target.val();
    if( p1 ) {
        var passed = validatePassword(p1, {
            length:   [6, Infinity],
            numeric:  1,
            badWords: ["password"],
            badSequenceLength: 6
        });        
        if( !passed ) {
            showValidation(target, "Your password must be at least 6 characters, it must contain numbers and letters and must not contain the word 'password'", container);
            return false;
        } else {
            return checkPasswordsMatch(container);
        }
    }
    return true;
}

function checkPasswordsMatch(container) {
    if( $("#confirmPassword").length == 0 ) {
        return true; // there is no confirmation field
    }
    var p1 = $("#password",container).val();
    var p2 = $("#confirmPassword",container).val();
    if( p1 != p2 ) {
        showValidation("password", "Your password's don't match. Please try again",container);
        return false;
    }
    return true;
}

/**
 * We assume the field to validate has an id of "email"
 */
function checkValidEmailAddress(container) {
    var target = $("#email, input.email", container); // either with id of email, or with class email
    var emailAddress = target.val();
    if( emailAddress ) {
        var pattern = new RegExp(/^(("[\w-\s]+")|([\w-]+(?:\.[\w-]+)*)|("[\w-\s]+")([\w-]+(?:\.[\w-]+)*))(@((?:[\w-]+\.)*\w[\w-]{0,66})\.([a-z]{2,6}(?:\.[a-z]{2})?)$)|(@\[?((25[0-5]\.|2[0-4][0-9]\.|1[0-9]{2}\.|[0-9]{1,2}\.))((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[0-9]{1,2})\.){2}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[0-9]{1,2})\]?$)/i);
        if( !pattern.test(emailAddress) ) {
            showValidation(target, "Please check the format of your email address, it should read like ben@somewhere.com", container);
            return false;
        }
    }
    return true;
}

function checkValueLength(target, minLength, maxLength, lbl) {
    log('checkValueLength', target, minLength, maxLength, lbl);
    target = ensureObject(target);
    if( target.length == 0 ) {
        return true;
    }
    var value = target.val();
    log('length', value.length);
    if( minLength ) {
        if( value.length < minLength ) {
            showValidation(target, lbl + " must be at least " + minLength + " characters");
            return false;
        }
    }
    if( maxLength ) {
        log('check max length: ' + (value.length > maxLength));
        if( value.length > maxLength ) {
            showValidation(target, lbl + " must be no more then " + maxLength + " characters");
            return false;
        } else {
            log('check max length ok: ' + (value.length > maxLength));
        }

    }
    log('length ok');
    return true;
}

function checkExactLength(target, length) {
    target = ensureObject(target);
    var value = target.val();
    if( value.length != length ) {
        showValidation(target, "Must be at " + length + " characters");
        return false;
    }
    return true;
}



// Passes if one of the targets has a non-empty value
function checkOneOf(target1, target2, message) {
    target1 = ensureObject(target1);
    target2 = ensureObject(target2);
    if( target1.val() || target2.val() ) {
        return true;
    } else {
        showValidation(target1, message);
        return false
    }
}


// Passes if target's value is either empty or a number. Spaces etc are not allowed
function checkNumeric(target) {
    if( typeof target == "string") {
        target = $("#" + target);
    }
    var n = target.val();
    if( n ) {
        if( !isNumber(n)) {
            showValidation(target, "Please enter only numeric digits");
            return false;
        } else {
            return true;
        }
    } else {
        return true;
    }
}

function checkTrue(target, message, container) {
    var n = $("#" + target + ":checked").val();
    if( n ) {
        return true;
    } else {
        showValidation($("label[for='" + target + "']"), message, container);
        return false;
    }
}



/**
* target can be id or a jquery object
* text is the text to display
*/
function showValidation(target, text, container) {
    if( text ) {
        log("showValidation", target, text);
        showMessage(text, container);
        if( target ) {
            var t = ensureObject(target);
            showErrorField(t);
        }
    }
}

function showMessage(text, container) {
    $(".pageMessage",container).append("<p class='validationError'>" + text + "</p>");
    $(".pageMessage",container).show(500);
}

function showErrorField(target) {
    target.addClass("errorField");
}

/*
	Password Validator 0.1
	(c) 2007 Steven Levithan <stevenlevithan.com>
	MIT License
*/

function validatePassword (pw, options) {
    // default options (allows any password)
    var o = {
        lower:    0,
        upper:    0,
        alpha:    0, /* lower + upper */
        numeric:  0,
        special:  0,
        length:   [0, Infinity],
        custom:   [ /* regexes and/or functions */ ],
        badWords: [],
        badSequenceLength: 0,
        noQwertySequences: false,
        noSequential:      false
    };

    for (var property in options)
        o[property] = options[property];

    var	re = {
        lower:   /[a-z]/g,
        upper:   /[A-Z]/g,
        alpha:   /[A-Z]/gi,
        numeric: /[0-9]/g,
        special: /[\W_]/g
    },
    rule, i;

    // enforce min/max length
    if (pw.length < o.length[0] || pw.length > o.length[1])
        return false;

    // enforce lower/upper/alpha/numeric/special rules
    for (rule in re) {
        if ((pw.match(re[rule]) || []).length < o[rule])
            return false;
    }

    // enforce word ban (case insensitive)
    for (i = 0; i < o.badWords.length; i++) {
        if (pw.toLowerCase().indexOf(o.badWords[i].toLowerCase()) > -1)
            return false;
    }

    // enforce the no sequential, identical characters rule
    if (o.noSequential && /([\S\s])\1/.test(pw))
        return false;

    // enforce alphanumeric/qwerty sequence ban rules
    if (o.badSequenceLength) {
        var	lower   = "abcdefghijklmnopqrstuvwxyz",
        upper   = lower.toUpperCase(),
        numbers = "0123456789",
        qwerty  = "qwertyuiopasdfghjklzxcvbnm",
        start   = o.badSequenceLength - 1,
        seq     = "_" + pw.slice(0, start);
        for (i = start; i < pw.length; i++) {
            seq = seq.slice(1) + pw.charAt(i);
            if (
                lower.indexOf(seq)   > -1 ||
                upper.indexOf(seq)   > -1 ||
                numbers.indexOf(seq) > -1 ||
                (o.noQwertySequences && qwerty.indexOf(seq) > -1)
                ) {
                return false;
            }
        }
    }

    // enforce custom regex/function rules
    for (i = 0; i < o.custom.length; i++) {
        rule = o.custom[i];
        if (rule instanceof RegExp) {
            if (!rule.test(pw))
                return false;
        } else if (rule instanceof Function) {
            if (!rule(pw))
                return false;
        }
    }

    // great success!
    return true;
}

