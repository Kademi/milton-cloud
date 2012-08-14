function endsWith(str, suffix) {
    return str.match(suffix+"$")==suffix;
}
function startsWith(str, prefix) {
    return str.indexOf(prefix) === 0;
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
    return this.match(suffix+"$")==suffix;
};

function ensureObject(target) {
    if( typeof target == "string") {
        target = $("#" + target);
    }
    return target;
}

$.extend({
    URLEncode:function(c){
        var o='';
        var x=0;
        c=c.toString();
        var r=/(^[a-zA-Z0-9_.]*)/;
        while(x<c.length){
            var m=r.exec(c.substr(x));
            if(m!=null && m.length>1 && m[1]!=''){
                o+=m[1];
                x+=m[1].length;
            }else{
                var d=c.charCodeAt(x);
                var h=d.toString(16);
                o+='%'+(h.length<2?'0':'')+h.toUpperCase();

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
    URLDecode:function(s){
        var o=s;
        var binVal,t;
        var r=/(%[^%]{2})/;
        while((m=r.exec(o))!=null && m.length>1 && m[1]!=''){
            b=parseInt(m[1].substr(1),16);
            t=String.fromCharCode(b);
            o=o.replace(m[1],t);
        }
        return o;
    }
});


/**
 * varargs function to output to console.log if console is available
 */
function log() {
    if( typeof(console) != "undefined" ) {
        console.log(arguments);
    }
}

function pad2(i) {
    var j = i - 0; // force to be a number
    if( j < 10 ) {
        return "0" + j;
    } else {
        return i;
    }
}


function toFileSize(num) {
    if( num > 1000000 ) {
        return Math.round(num/1000000) + 'Mb';
    } else {
        return Math.round(num/1000) + 'Kb';
    }
}

function toDisplayDateNoTime(dt) {
    return (dt.date) + "/" + (dt.month+1) + "/" + (dt.year+1900);
}

function now() {
    var dt = new Date();
    return {
        day: dt.getDay(),
        month: dt.getMonth(),
        year: dt.getYear()
    };
}



function reverseDateOrd(post1,post2){
    return dateOrd(post1,post2) * -1;
}

function dateOrd(post1,post2){
    var n = post1.date;
    if( n == null ) {
        if( m == null ) {
            return 0;
        } else {
            return -1;
        }
    }
    var m = post2.date;

    if( n.year < m.year ) {
        return -1;
    } else if( n.year > m.year ) {
        return 1;
    }
    if( n.month < m.month ) {
        return -1;
    } else if( n.month > m.month ) {
        return 1;
    }
    if( n.day < m.day ) {
        return -1;
    }
    else if( n.day > m.day ) {
        return 1;
    }
    if( n.hours < m.hours ) {
        return -1;
    } else if( n.hours > m.hours ) {
        return 1;
    }
    if( n.minutes < m.minutes ) {
        return -1;
    } else if( n.minutes> m.minutes ) {
        return 1;
    }
    if( n.seconds < m.seconds ) {
        return -1;
    } else if( n.seconds> m.seconds ) {
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
var dtCh= "/";
var minYear=1900;
var maxYear=2100;

function isInteger(s){
    var i;
    for (i = 0; i < s.length; i++){
        // Check that current character is number.
        var c = s.charAt(i);
        if (((c < "0") || (c > "9"))) return false;
    }
    // All characters are numbers.
    return true;
}

function stripCharsInBag(s, bag){
    var i;
    var returnString = "";
    // Search through string's characters one by one.
    // If character is not in bag, append to returnString.
    for (i = 0; i < s.length; i++){
        var c = s.charAt(i);
        if (bag.indexOf(c) == -1) returnString += c;
    }
    return returnString;
}

function daysInFebruary (year){
    // February has 29 days in any year evenly divisible by four,
    // EXCEPT for centurial years which are not also divisible by 400.
    return (((year % 4 == 0) && ( (!(year % 100 == 0)) || (year % 400 == 0))) ? 29 : 28 );
}
function DaysArray(n) {
    for (var i = 1; i <= n; i++) {
        this[i] = 31
        if (i==4 || i==6 || i==9 || i==11) {
            this[i] = 30
        }
        if (i==2) {
            this[i] = 29
        }
    }
    return this
}

function isDate(dtStr){
    var daysInMonth = DaysArray(12)
    var pos1=dtStr.indexOf(dtCh)
    var pos2=dtStr.indexOf(dtCh,pos1+1)
    var strDay=dtStr.substring(0,pos1)
    var strMonth=dtStr.substring(pos1+1,pos2)
    var strYear=dtStr.substring(pos2+1)
    strYr=strYear
    if (strDay.charAt(0)=="0" && strDay.length>1) strDay=strDay.substring(1)
    if (strMonth.charAt(0)=="0" && strMonth.length>1) strMonth=strMonth.substring(1)
    for (var i = 1; i <= 3; i++) {
        if (strYr.charAt(0)=="0" && strYr.length>1) strYr=strYr.substring(1)
    }
    month=parseInt(strMonth)
    day=parseInt(strDay)
    year=parseInt(strYr)
    if (pos1==-1 || pos2==-1){
        log("The date format should be : dd/mm/yyyy");
        return false
    }
    if (strMonth.length<1 || month<1 || month>12){
        log("Please enter a valid month");
        return false
    }
    if (strDay.length<1 || day<1 || day>31 || (month==2 && day>daysInFebruary(year)) || day > daysInMonth[month]){
        log("Please enter a valid day");
        return false
    }
    if (strYear.length != 4 || year==0 || year<minYear || year>maxYear){
        log("Please enter a valid 4 digit year between "+minYear+" and "+maxYear);
        return false
    }
    if (dtStr.indexOf(dtCh,pos2+1)!=-1 || isInteger(stripCharsInBag(dtStr, dtCh))==false){
        log("Please enter a valid date");
        return false
    }
    return true
}

function ValidateForm(){
    var dt=document.frmSample.txtDate
    if (isDate(dt.value)==false){
        dt.focus()
        return false
    }
    return true
}

function getFileName(path) {
    var arr = path.split('/');
    var name = arr[arr.length-1];
    if( name.length == 0 ) { // might be empty if trailing slash
        name = arr[arr.length-2];
    }    
    if( name.contains("#")) {
        var pos = name.lastIndexOf("#");
        name = name.substring(0, pos);
    }
    return name;
}

function getFolderPath(path) {
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
    if( !$("body").hasClass("edifyIsEditMode")) {
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
    $form.each(function(){
        this.reset();
    });
}

function edify(container, cssFiles, callback) {
    log("edify", container, callback);
    $("body").removeClass("edifyIsViewMode");
    $("body").addClass("edifyIsEditMode");
        
    if( !callback ) {
        callback = function(resp) {
            if( resp.nextHref) {
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
            
        $("#edifyForm").submit(function() {
            submitEdifiedForm();
        });
        log("done hide, now show again");
        container.animate({
            opacity: 1
        },500);
    });

}

function edifyPage(container, cssFiles, callback) {
    log("edify", container, callback);
    $("body").removeClass("edifyIsViewMode");
    $("body").addClass("edifyIsEditMode");
        
    if( !callback ) {
        callback = function(resp) {
            if( resp.nextHref) {
            //window.location = resp.nextHref;
            } else {
            //window.location = window.location.pathname;
            }            
        };
    }
    
    container.animate({
        opacity: 0
    }, 200, function() {
        initHtmlEditors(container.find(".htmleditor"), cssFiles);
    
        $(".inputTextEditor").each(function(i, n) {
            var $n = $(n);
            var s = $n.text();
            $n.replaceWith("<input name='" + $n.attr("id") + "' type='text' value='" + s + "' />");
        });        
        container.wrap("<form id='edifyForm' action='" + window.location + "' method='POST'></form>");
        $("#edifyForm").append("<input type='hidden' name='body' value='' />");
        var buttons = $("<div class='buttons'></div>");
        $("#edifyForm").prepend(buttons);
        var title = $("<input type='text' name='title' id='title' title='Enter the page title here' />");
        title.val(document.title);
        buttons.append(title);
        buttons.append("<button title='Save edits to the page' class='save' type='submit'>Save</button>");
        buttons.append("<button title='View page history' class='history' type='button'>History</button>");
        var btnCancel = $("<button title='Return to view mode without saving. Any changes will be lost' class='cancel' type='button'>Cancel</button>");
        btnCancel.click(function() {
            window.location.reload()
        });
        buttons.append(btnCancel);
            
        $("#edifyForm").submit(function() {
            submitEdifiedForm();
        });
        log("done hide, now show again");
        container.animate({
            opacity: 1
        },500);
    });

}


function submitEdifiedForm(callback) {
    var form = $("#edifyForm");
    log("submit form", form);
    for( var key in CKEDITOR.instances) {
        var editor = CKEDITOR.instances[key];
        var content = editor.getData();
        var inp = $("input[name=" + key + "], textarea[name=" + key + "]");
        if( inp.length > 0 ) {
            inp.val(content);
        } else {
            inp = $("<input type='hidden' name='" + key + "/>");
            form.append(inp);
            inp.val(content);
        }
        log("copied html editor val to:", inp, "for", key);
    }
        
    var data = form.serialize();

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
                if( callback ) {
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
    } catch(e) {
        log("exception", e);
    }
    return false;    
}

function confirmDelete(href, name, callback) {
    if( confirm("Are you sure you want to delete " + name + "?")) {
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
            if( callback ) {
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

function showCreateFolder() {
    log("showCreateFolder");
    $("#createFolderModal").dialog({
        modal: true,
        width: 500,
        title: "Create folder",
        buttons: { 
            "Ok": function() { 
                var name = $("#createFolderModal input[type=text]").val();
                if( name && name.length > 0 ) {
                    createFolder(name);
                    $(this).dialog("close");                     
                } else {
                    alert("Please enter a name to create");
                }
            } ,
            "Cancel": function() {
                $(this).dialog("close"); 
            }
        }
    });
}

function createFolder(name, parentHref, callback) {
    var encodedName = name; //$.URLEncode(name);
    //    ajaxLoadingOn();
    var url = "_DAV/MKCOL";
    if( parentHref ) {
        var s = parentHref;
        if( !s.endsWith("/")) {
            s+="/";
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
            //            ajaxLoadingOff();
            //window.location = encodedName + "/index.html";
            if( callback ) {
                callback(name, resp);
            }
        },
        error: function() {
            //            ajaxLoadingOff();
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
    log("initActiveNav", url);
    $(containerSelector + " a").each(function(i, n) {
        var node = $(n);
        var href = node.attr("href");
        if( href ) {
            if( href.startsWith(url) ) {
                node.addClass("active");
            }
        }
    });
}