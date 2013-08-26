/**
 *  jqExtras.js - this is for functions which work with jquery. Generally should be loaded
 *  after jquery
 */

$.extend({
    URLEncode:function(c){
        var o='';
        var x = 0;
        c=c.toString();
        var r=/(^[a-zA-Z0-9_.]*)/;
        while(x<c.length){
            var m=r.exec(c.substr(x));
            if(m!=null && m.length>1 && m[1]!=''){
                o+=m[1];
                x+=m[1].length;
            } else {
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

function edify(container, callback, validateCallback) {
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
            
        $("#edifyForm").submit(function(e) {
            log("edifyForm submit");
            e.preventDefault();
            e.stopPropagation();
            submitEdifiedForm(callback, validateCallback);
        });
        log("done hide, now show again");
        container.animate({
            opacity: 1
        },500);
    });
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

function suffixSlash(href) {
    if( href.endsWith("/")) {
        return href;
    }
    return href + "/";
}

function showCreateFolder(parentHref, title, text, callback, validatorFn) {
    log("showCreateFolder");
    var s = text;
    if( !s ) {
        s = "Please enter a name for the new folder";
    }    
    myPrompt("createFolder", parentHref, title, text, "Enter a name","newName", "Create", "", "Enter a name to create", function(newName, form) {
        log("create folder", form);
        var msg = null;
        if( validatorFn ) {
            msg = validatorFn(newName);
        }
        if( msg == null ) {
            createFolder(newName, parentHref,function() {
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
        success: function(resp) {
            $("body").trigger("ajaxLoading", {
                loading: false
            });
            if( callback ) {
                callback(name, resp);
            }
        },
        error: function(resp) {
            log("error", resp);
            $("body").trigger("ajaxLoading", {
                loading: false
            });
            if( resp.status == 200 ) {
                if( callback ) {
                    callback(name, resp);
                }
                return;
            }
            
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
    if( newName ) {
        newName = newName.trim();
        if( newName.length > 0 && currentName != newName ) {        
            var currentFolder = getFolderPath(sourceHref);
            var dest = currentFolder;
            if( !dest.endsWith("/")) {
                dest += "/";
            }
            dest += newName;
            move(sourceHref, dest, callback);
        }
    }
}

function move(sourceHref, destHref, callback) {
    //    ajaxLoadingOn();    
    var url = "_DAV/MOVE";
    if( sourceHref ) {
        var s = sourceHref;
        log("s", s);
        if( !s.endsWith("/")) {
            s+="/";
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
            if( callback ) {
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
        if( href ) {
            log("initActiveNav, check", url, href);
            if( href.startsWith(url) ) {
                node.addClass("active");
            }
        }
    });
}


// http://stackoverflow.com/questions/1134976/how-may-i-sort-a-list-alphabetically-using-jquery
function asc_sort(a, b){
    return ($(b).text()) < ($(a).text());    
}

// decending sort
function dec_sort(a, b){
    return ($(b).text()) > ($(a).text());    
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
