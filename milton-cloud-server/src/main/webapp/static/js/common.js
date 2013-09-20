function endsWith(str, suffix) {
    return str.match(suffix + "$") == suffix;
}
function startsWith(str, prefix) {
    return str.indexOf(prefix) === 0;
}

Date.prototype.formatMMDDYYYY = function() {
    return (this.getMonth() + 1) +
            "/" + this.getDate() +
            "/" + this.getFullYear();
}

Date.prototype.formatDDMMYYYY = function() {
    return this.getDate() +
            "/" + (this.getMonth() + 1) +
            "/" + this.getFullYear();
}

/**
 * Adds a contains function to String objects
 */
String.prototype.contains = function(it) {
    return this.indexOf(it) != -1;
};

String.prototype.startsWith = function(prefix) {
    return this.indexOf(prefix) === 0;
};
String.prototype.endsWith = function(suffix) {
    return this.match(suffix + "$") == suffix;
};

function ensureObject(target) {
    if (typeof target == "string") {
        target = $("#" + target);
    }
    return target;
}

$.extend({
    URLEncode: function(c) {
        var o = '';
        var x = 0;
        c = c.toString();
        var r = /(^[a-zA-Z0-9_.]*)/;
        while (x < c.length) {
            var m = r.exec(c.substr(x));
            if (m != null && m.length > 1 && m[1] != '') {
                o += m[1];
                x += m[1].length;
            } else {
                var d = c.charCodeAt(x);
                var h = d.toString(16);
                o += '%' + (h.length < 2 ? '0' : '') + h.toUpperCase();

                //                if(c[x]==' ')o+='+';
                //                else{
                //                    var d=c.charCodeAt(x);
                //                    var h=d.toString(16);
                //                    o+='%'+(h.length<2?'0':'')+h.toUpperCase();
                //                }
                x++;
            }
        }
        return o;
    },
    URLDecode: function(s) {
        var o = s;
        var binVal, t;
        var r = /(%[^%]{2})/;
        while ((m = r.exec(o)) != null && m.length > 1 && m[1] != '') {
            b = parseInt(m[1].substr(1), 16);
            t = String.fromCharCode(b);
            o = o.replace(m[1], t);
        }
        return o;
    }
});

// just a nice little function to get classes
$.fn.classes = function(f) {
    var c = [];
    $.each(this, function(i, v) {
        var _ = v.className.split(/\s+/);
        for (var j in _)
            '' === _[j] || c.push(_[j]);
    });
    c = $.unique(c);
    if ("function" === typeof f)
        for (var j in c)
            f(c[j]);
    return c;
};

/**
 * varargs function to output to console.log if console is available
 */
function log() {
    if (typeof(console) != "undefined") {
        if (navigator.appName == 'Microsoft Internet Explorer') {
            // BM: Previous used JSON, but that crashed IE sometimes. So this is pretty crap, but at least safer
            if (arguments.length == 1) {
                console.log(arguments[0]);
            } else if (arguments.length == 2) {
                console.log(arguments[0], arguments[1]);
            } else if (arguments.length > 2) {
                console.log(arguments[0], arguments[1], arguments[2]);
            }
        } else {
            console.log(arguments);
        }
    }
}

function pad2(i) {
    var j = i - 0; // force to be a number
    if (j < 10) {
        return "0" + j;
    } else {
        return i;
    }
}


function toFileSize(num) {
    if (num > 1000000) {
        return Math.round(num / 1000000) + 'Mb';
    } else {
        return Math.round(num / 1000) + 'Kb';
    }
}

function toDisplayDateNoTime(dt) {
    return (dt.date) + "/" + (dt.month + 1) + "/" + (dt.year + 1900);
}

function now() {
    var dt = new Date();
    return {
        day: dt.getDay(),
        month: dt.getMonth(),
        year: dt.getYear()
    };
}



function reverseDateOrd(post1, post2) {
    return dateOrd(post1, post2) * -1;
}

function dateOrd(post1, post2) {
    var n = post1.date;
    if (n == null) {
        if (m == null) {
            return 0;
        } else {
            return -1;
        }
    }
    var m = post2.date;

    if (n.year < m.year) {
        return -1;
    } else if (n.year > m.year) {
        return 1;
    }
    if (n.month < m.month) {
        return -1;
    } else if (n.month > m.month) {
        return 1;
    }
    if (n.day < m.day) {
        return -1;
    }
    else if (n.day > m.day) {
        return 1;
    }
    if (n.hours < m.hours) {
        return -1;
    } else if (n.hours > m.hours) {
        return 1;
    }
    if (n.minutes < m.minutes) {
        return -1;
    } else if (n.minutes > m.minutes) {
        return 1;
    }
    if (n.seconds < m.seconds) {
        return -1;
    } else if (n.seconds > m.seconds) {
        return 1;
    }
    return 0;
}

function isNumber(n) {
    return !isNaN(parseFloat(n)) && isFinite(n);
}

function ajaxLoadingOn(sel) {
    log('ajax ON', sel);
}

function ajaxLoadingOff(sel) {
    log('ajax OFF', sel);

}

/** Displays a modal with a title and message
 */
function showThankyou(title, message) {
    log("showThankyou");
    $(".modal").dialog("close");
    $('#thankyou h3').html(title);
    $('#thankyou p').html(message);
    $('#thankyou').dialog({
        modal: true,
        width: 500
    });
}

/**
 * DHTML date validation script. Courtesy of SmartWebby.com (http://www.smartwebby.com/dhtml/)
 */
// Declaring valid date character, minimum year and maximum year
var dtCh = "/";
var minYear = 1900;
var maxYear = 2100;

function isInteger(s) {
    var i;
    for (i = 0; i < s.length; i++) {
        // Check that current character is number.
        var c = s.charAt(i);
        if (((c < "0") || (c > "9")))
            return false;
    }
    // All characters are numbers.
    return true;
}

function stripCharsInBag(s, bag) {
    var i;
    var returnString = "";
    // Search through string's characters one by one.
    // If character is not in bag, append to returnString.
    for (i = 0; i < s.length; i++) {
        var c = s.charAt(i);
        if (bag.indexOf(c) == -1)
            returnString += c;
    }
    return returnString;
}

function daysInFebruary(year) {
    // February has 29 days in any year evenly divisible by four,
    // EXCEPT for centurial years which are not also divisible by 400.
    return (((year % 4 == 0) && ((!(year % 100 == 0)) || (year % 400 == 0))) ? 29 : 28);
}
function DaysArray(n) {
    for (var i = 1; i <= n; i++) {
        this[i] = 31
        if (i == 4 || i == 6 || i == 9 || i == 11) {
            this[i] = 30
        }
        if (i == 2) {
            this[i] = 29
        }
    }
    return this
}

function isDate(dtStr) {
    var daysInMonth = DaysArray(12)
    var pos1 = dtStr.indexOf(dtCh)
    var pos2 = dtStr.indexOf(dtCh, pos1 + 1)
    var strDay = dtStr.substring(0, pos1)
    var strMonth = dtStr.substring(pos1 + 1, pos2)
    var strYear = dtStr.substring(pos2 + 1)
    strYr = strYear
    if (strDay.charAt(0) == "0" && strDay.length > 1)
        strDay = strDay.substring(1)
    if (strMonth.charAt(0) == "0" && strMonth.length > 1)
        strMonth = strMonth.substring(1)
    for (var i = 1; i <= 3; i++) {
        if (strYr.charAt(0) == "0" && strYr.length > 1)
            strYr = strYr.substring(1)
    }
    month = parseInt(strMonth)
    day = parseInt(strDay)
    year = parseInt(strYr)
    if (pos1 == -1 || pos2 == -1) {
        log("The date format should be : dd/mm/yyyy");
        return false
    }
    if (strMonth.length < 1 || month < 1 || month > 12) {
        log("Please enter a valid month");
        return false
    }
    if (strDay.length < 1 || day < 1 || day > 31 || (month == 2 && day > daysInFebruary(year)) || day > daysInMonth[month]) {
        log("Please enter a valid day");
        return false
    }
    if (strYear.length != 4 || year == 0 || year < minYear || year > maxYear) {
        log("Please enter a valid 4 digit year between " + minYear + " and " + maxYear);
        return false
    }
    if (dtStr.indexOf(dtCh, pos2 + 1) != -1 || isInteger(stripCharsInBag(dtStr, dtCh)) == false) {
        log("Please enter a valid date");
        return false
    }
    return true
}

function ValidateForm() {
    var dt = document.frmSample.txtDate
    if (isDate(dt.value) == false) {
        dt.focus()
        return false
    }
    return true
}

function getFileName(path) {
    var arr = path.split('/');
    if (arr.length === 1) {
        return "";
    }
    var name = arr[arr.length - 1];
    if (name == null || name.length == 0) { // might be empty if trailing slash
        name = arr[arr.length - 2];
    }
    if (name.contains("#")) {
        var pos = name.lastIndexOf("#");
        name = name.substring(0, pos);
    }

    path = path.replaceAll(" ", "%20"); // safari bug. path is returned encoded from window.location.pathname
    return name;
}

/**
 * 
 * Get the path of the resource which contains the given path
 * 
 */
function getFolderPath(path) {
    path = stripFragment(path); // remove any fragment like #section
    if( path.endsWith("/")) {
        path = path.substring(0, path.length-1);
    }
    var pos = path.lastIndexOf("/");
    return path.substring(0, pos);
}

/**
 * just removed the server portion of the href
 */
function getPathFromHref(href) {
    // eg http://blah.com/a/b -->> /a/b
    var path = href.substring(8); // drop protocol
    var pos = path.indexOf("/");
    path = path.substring(pos);
    return path;
}


function initEdify() {
    if (!$("body").hasClass("edifyIsEditMode")) {
        $("body").addClass("edifyIsViewMode");
    }
    $("body").on("click", ".edifyDelete", function() {
        var href = window.location.href;
        var name = getFileName(href);
        confirmDelete(href, name, function() {
            alert("Page deleted");
            var folderHref = getFolderPath(href);
            log("load", folderHref);
            window.location = folderHref + '/';
        });
    });
}

function resetForm($form) {
    $form.each(function() {
        this.reset();
    });
}

function edify(container, callback, validateCallback) {
    log("edify", container, callback);
    $("body").removeClass("edifyIsViewMode");
    $("body").addClass("edifyIsEditMode");

    if (!callback) {
        callback = function(resp) {
            if (resp.nextHref) {
                //window.location = resp.nextHref;
            } else {
                //window.location = window.location.pathname;
            }
        };
    }

    container.animate({
        opacity: 0
    }, 200, function() {
        //initHtmlEditors(cssFiles);
        initHtmlEditors();

        $(".inputTextEditor").each(function(i, n) {
            var $n = $(n);
            var s = $n.text();
            $n.replaceWith("<input name='" + $n.attr("id") + "' type='text' value='" + s + "' />");
        });
        container.wrap("<form id='edifyForm' action='" + window.location + "' method='POST'></form>");
        $("#edifyForm").append("<input type='hidden' name='body' value='' />");

        $("#edifyForm").submit(function(e) {
            log("edifyForm submit");
            e.preventDefault();
            e.stopPropagation();
            submitEdifiedForm(callback, validateCallback);
        });
        log("done hide, now show again");
        container.animate({
            opacity: 1
        }, 500);
    });
}




function submitEdifiedForm(callback, validateCallback) {
    var form = $("#edifyForm");
    log("trigger event..");
    form.trigger("submitEdified");
    log("submit form", form);
    for (var key in CKEDITOR.instances) {
        var editor = CKEDITOR.instances[key];
        var content = editor.getData();
        var inp = $("input[name=" + key + "], textarea[name=" + key + "]");
        if (inp.length > 0) {
            inp.val(content);
        } else {
            inp = $("<input type='hidden' name='" + key + "/>");
            form.append(inp);
            inp.val(content);
        }
        log("copied html editor val to:", inp, "for", key);
    }

    resetValidation(form);
    if (!checkRequiredFields(form)) {
        return false;
    }

    if (validateCallback) {
        if (!validateCallback(form)) {
            log("validation callback reported false");
            return false;
        }
    }


    var data = form.serialize();
    log("serialied", data);

    try {
        //$("#edifyForm input[name=body]").attr("value", CKEDITOR.instances["editor1"].getData() );
        $.ajax({
            type: 'POST',
            url: $("#edifyForm").attr("action"),
            data: data,
            dataType: "json",
            success: function(resp) {
                ajaxLoadingOff();
                log("common.js: edify: save success", resp, window.location.path);
                if (callback) {
                    log("call callback", callback);
                    callback(resp);
                } else {
                    log("no callback");
                }
            },
            error: function(resp) {
                ajaxLoadingOff();
                alert("err");
            }
        });
    } catch (e) {
        log("exception", e);
    }
    return false;
}

function confirmDelete(href, name, callback) {
    if (confirm("Are you sure you want to delete " + name + "?")) {
        deleteFile(href, callback);
    }
}
function deleteFile(href, callback) {
    ajaxLoadingOn();
    $.ajax({
        type: 'DELETE',
        url: href,
        dataType: "json",
        success: function(resp) {
            log('deleted', href);
            ajaxLoadingOff();
            if (callback) {
                callback();
            }
        },
        error: function(resp) {
            log("failed", resp);
            ajaxLoadingOff();
            alert("Sorry, an error occured deleting " + href + ". Please check your internet connection");
        }
    });
}

function proppatch(href, data, callback) {
    ajaxLoadingOn();
    href = suffixSlash(href);
    $.ajax({
        type: 'POST',
        url: href + "_DAV/PROPPATCH",
        data: data,
        dataType: "json",
        success: function(resp) {
            ajaxLoadingOff();
            if (callback) {
                callback();
            }
        },
        error: function(resp) {
            log("failed", resp);
            ajaxLoadingOff();
            alert("Sorry, an error occured deleting " + href + ". Please check your internet connection");
        }
    });
}

function suffixSlash(href) {
    if (href.endsWith("/")) {
        return href;
    }
    return href + "/";
}

function showCreateFolder(parentHref, title, text, callback, validatorFn) {
    log("showCreateFolder");
    var s = text;
    if (!s) {
        s = "Please enter a name for the new folder";
    }
    myPrompt("createFolder", parentHref, title, text, "Enter a name", "newName", "Create", "", "Enter a name to create", function(newName, form) {
        log("create folder", form);
        var msg = null;
        if (validatorFn) {
            msg = validatorFn(newName);
        }
        if (msg == null) {
            createFolder(newName, parentHref, function() {
                callback(newName);
                closeMyPrompt();
            });
        } else {
            alert(msg);
        }
        return false;
    });
}

function createFolder(name, parentHref, callback) {
    log("createFolder: name=", name, "parentHref=", parentHref);
    var encodedName = name; //$.URLEncode(name);
    //    ajaxLoadingOn();
    var url = "_DAV/MKCOL";
    if (parentHref) {
        var s = parentHref;
        if (!s.endsWith("/")) {
            s += "/";
        }
        s += url;
        url = s;
    }
    $.ajax({
        type: 'POST',
        url: url,
        data: {
            name: encodedName
        },
        dataType: "json",
        success: function(resp) {
            $("body").trigger("ajaxLoading", {
                loading: false
            });
            if (callback) {
                callback(name, resp);
            }
        },
        error: function() {
            $("body").trigger("ajaxLoading", {
                loading: false
            });
            alert('There was a problem creating the folder');
        }
    });
}

/**
 *  Prompts the user for a new name, and the does a rename (ie move)
 */
function promptRename(sourceHref, callback) {
    log("promptRename", sourceHref);
    var currentName = getFileName(sourceHref);
    var newName = prompt("Please enter a new name for " + currentName, currentName);
    if (newName) {
        newName = newName.trim();
        if (newName.length > 0 && currentName != newName) {
            var currentFolder = getFolderPath(sourceHref);
            log("promptRename: currentFolder", currentFolder);
            var dest = currentFolder;
            if (!dest.endsWith("/")) {
                dest += "/";
            }
            dest += newName;
            log("promptRename: dest", dest);
            move(sourceHref, dest, callback);
        }
    }
}

function move(sourceHref, destHref, callback) {
    //    ajaxLoadingOn();    
    var url = "_DAV/MOVE";
    if (sourceHref) {
        var s = sourceHref;
        log("s", s);
        if (!s.endsWith("/")) {
            s += "/";
        }
        url = s + url;
    }
    log("move", sourceHref, destHref, "url=", url);
    $("body").trigger("ajaxLoading", {
        loading: true
    });
    $.ajax({
        type: 'POST',
        url: url,
        data: {
            destination: destHref
        },
        dataType: "json",
        success: function(resp) {
            $("body").trigger("ajaxLoading", {
                loading: false
            });
            if (callback) {
                callback(resp);
            }
        },
        error: function() {
            $("body").trigger("ajaxLoading", {
                loading: false
            });
            alert('There was a problem creating the folder');
        }
    });
}

/**
 * Find links which begin with the current url (or are equal) within the given
 * containerSelector, and add the 'active' class to them
 */
function initActiveNav(containerSelector) {
    var url = window.location.pathname;
    var container = $(containerSelector);
    log("initActiveNav", url, "container:", container);
    container.find("a").each(function(i, n) {
        var node = $(n);
        var href = node.attr("href");
        if (href) {
            log("initActiveNav, check", url, href);
            if (href.startsWith(url)) {
                node.addClass("active");
            }
        }
    });
}

function profileImg(user) {
    var profileHref;
    if (user.photoHref) {
        profileHref = user.photoHref;
    } else if (user.photoHash) {
        profileHref = "/_hashes/files/" + user.photoHash + "";
    } else {
        profileHref = "/templates/apps/user/profile.png";
    }
    var profilePic = "<img src='" + profileHref + "' alt='' />";
    return profilePic;
}

// http://stackoverflow.com/questions/1134976/how-may-i-sort-a-list-alphabetically-using-jquery
function asc_sort(a, b) {
    return ($(b).text()) < ($(a).text());
}

// decending sort
function dec_sort(a, b) {
    return ($(b).text()) > ($(a).text());
}

/**
 * ReplaceAll by Fagner Brack (MIT Licensed)
 * Replaces all occurrences of a substring in a string
 */
String.prototype.replaceAll = function(token, newToken, ignoreCase) {
    var str, i = -1, _token;
    if ((str = this.toString()) && typeof token === "string") {
        _token = ignoreCase === true ? token.toLowerCase() : undefined;
        while ((i = (
                _token !== undefined ?
                str.toLowerCase().indexOf(
                _token,
                i >= 0 ? i + newToken.length : 0
                ) : str.indexOf(
                token,
                i >= 0 ? i + newToken.length : 0
                )
                )) !== -1) {
            str = str.substring(0, i)
                    .concat(newToken)
                    .concat(str.substring(i + token.length));
        }
    }
    return str;
};

/**
 * Evaluate a relative path from an absolute path to get an absolute path to he relative path from the absolute path
 * 
 */
function evaluateRelativePath(startFrom, relPath) {
    var arr = relPath.split("/");
    var href = startFrom;
    for (i = 0; i < arr.length; i++) {
        var part = arr[i];
        if (part == "..") {
            href = getFolderPath(href);
        } else if (part == ".") {
            // do nothing
        } else {
            href += "/" + part;
        }
    }
    return href;
}

function replaceSpecialChars(nameToUse) {
    nameToUse = nameToUse.replace("/", "");
    nameToUse = nameToUse.replace("'", "");
    nameToUse = nameToUse.replace("\"", "");
    nameToUse = nameToUse.replace("@", "-");
    nameToUse = nameToUse.replace(" ", "-");
    nameToUse = nameToUse.replace("?", "-");
    nameToUse = nameToUse.replace(":", "-");
    nameToUse = nameToUse.replace("--", "-");
    nameToUse = nameToUse.replace("--", "-");
    return nameToUse;
}

function pulseBorder(node) {
    node.animate({
        boxShadow: '0 0 30px #ED9DAE'
    }, 2000, function() {
        setTimeout(function() {
            node.animate({
                boxShadow: '0 0 0 #FFF'
            }, 1000);
        }, 5000);
    });
}

/**
 * This hacky piece of shit was brought to you by IE8. Expanding an element
 * (eg a lightbulb div) will in some cases not trigger an IE reflow, so surrounding
 * elements will remain where they were, instead of getting pushed down. To work
 * around this we need to cause a reflow, which can be done by setting a css
 * property on the element which needs to be reflowed. In the case of nested lightbulbs
 * its the parent.parent of the dropdown
 * 
 * https://github.com/FuseLMS/Client-3DN/issues/12
 * 
 * @param {type} element
 * @returns {undefined}
 */
function refreshIE8Layout(element) {
    if (isIE() == 8) {
    	var p = element.parent().parent();
        p.css('height', 'auto');
        var height = p.height();
        p.css('height', height);
        p.css('height', 'auto');
        
//        var contentForm = element.closest(".contentForm");
//        contentForm
    }
}

function isIE() {
    var myNav = navigator.userAgent.toLowerCase();
    return (myNav.indexOf('msie') != -1) ? parseInt(myNav.split('msie')[1]) : false;
}

function stripFragment(href) {
    var i = href.indexOf("#");
    if( i > 0 ) {
        href = href.substring(0, i-1);
    }
    return href;
}

/*
 * http://javascriptbase64.googlecode.com/svn/trunk/base64.js
 * 
Copyright (c) 2008 Fred Palmer fred.palmer_at_gmail.com

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.
*/
function StringBuffer()
{ 
    this.buffer = []; 
} 

StringBuffer.prototype.append = function append(string)
{ 
    this.buffer.push(string); 
    return this; 
}; 

StringBuffer.prototype.toString = function toString()
{ 
    return this.buffer.join(""); 
}; 

var Base64 =
{
    codex : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

    encode : function (input)
    {
        var output = new StringBuffer();

        var enumerator = new Utf8EncodeEnumerator(input);
        while (enumerator.moveNext())
        {
            var chr1 = enumerator.current;

            enumerator.moveNext();
            var chr2 = enumerator.current;

            enumerator.moveNext();
            var chr3 = enumerator.current;

            var enc1 = chr1 >> 2;
            var enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            var enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            var enc4 = chr3 & 63;

            if (isNaN(chr2))
            {
                enc3 = enc4 = 64;
            }
            else if (isNaN(chr3))
            {
                enc4 = 64;
            }

            output.append(this.codex.charAt(enc1) + this.codex.charAt(enc2) + this.codex.charAt(enc3) + this.codex.charAt(enc4));
        }

        return output.toString();
    },

    decode : function (input)
    {
        var output = new StringBuffer();

        var enumerator = new Base64DecodeEnumerator(input);
        while (enumerator.moveNext())
        {
            var charCode = enumerator.current;

            if (charCode < 128)
                output.append(String.fromCharCode(charCode));
            else if ((charCode > 191) && (charCode < 224))
            {
                enumerator.moveNext();
                var charCode2 = enumerator.current;

                output.append(String.fromCharCode(((charCode & 31) << 6) | (charCode2 & 63)));
            }
            else
            {
                enumerator.moveNext();
                var charCode2 = enumerator.current;

                enumerator.moveNext();
                var charCode3 = enumerator.current;

                output.append(String.fromCharCode(((charCode & 15) << 12) | ((charCode2 & 63) << 6) | (charCode3 & 63)));
            }
        }

        return output.toString();
    }
}


function Utf8EncodeEnumerator(input)
{
    this._input = input;
    this._index = -1;
    this._buffer = [];
}

Utf8EncodeEnumerator.prototype =
{
    current: Number.NaN,

    moveNext: function()
    {
        if (this._buffer.length > 0)
        {
            this.current = this._buffer.shift();
            return true;
        }
        else if (this._index >= (this._input.length - 1))
        {
            this.current = Number.NaN;
            return false;
        }
        else
        {
            var charCode = this._input.charCodeAt(++this._index);

            // "\r\n" -> "\n"
            //
            if ((charCode == 13) && (this._input.charCodeAt(this._index + 1) == 10))
            {
                charCode = 10;
                this._index += 2;
            }

            if (charCode < 128)
            {
                this.current = charCode;
            }
            else if ((charCode > 127) && (charCode < 2048))
            {
                this.current = (charCode >> 6) | 192;
                this._buffer.push((charCode & 63) | 128);
            }
            else
            {
                this.current = (charCode >> 12) | 224;
                this._buffer.push(((charCode >> 6) & 63) | 128);
                this._buffer.push((charCode & 63) | 128);
            }

            return true;
        }
    }
}

function Base64DecodeEnumerator(input)
{
    this._input = input;
    this._index = -1;
    this._buffer = [];
}

Base64DecodeEnumerator.prototype =
{
    current: 64,

    moveNext: function()
    {
        if (this._buffer.length > 0)
        {
            this.current = this._buffer.shift();
            return true;
        }
        else if (this._index >= (this._input.length - 1))
        {
            this.current = 64;
            return false;
        }
        else
        {
            var enc1 = Base64.codex.indexOf(this._input.charAt(++this._index));
            var enc2 = Base64.codex.indexOf(this._input.charAt(++this._index));
            var enc3 = Base64.codex.indexOf(this._input.charAt(++this._index));
            var enc4 = Base64.codex.indexOf(this._input.charAt(++this._index));

            var chr1 = (enc1 << 2) | (enc2 >> 4);
            var chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            var chr3 = ((enc3 & 3) << 6) | enc4;

            this.current = chr1;

            if (enc3 != 64)
                this._buffer.push(chr2);

            if (enc4 != 64)
                this._buffer.push(chr3);

            return true;
        }
    }
};
