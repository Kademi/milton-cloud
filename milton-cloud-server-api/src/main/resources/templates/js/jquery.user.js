/**
 *
 *  jquery.login.js
 *  
 *  Depends on user.js and common.js
 *  
 *  The target should be a div containing
 *  - a form
 *  - <p> with id validationMessage
 *  - input type text for the username
 *  - input type password for the password
 *
 * Config:
 * urlSuffix: is appended to the current page url to make the url to POST the login request to. Default /.ajax
 * afterLoginUrl: the page to redirect to after login. Default index.html.  3 possibilities 
 *      null = no redirect
 *      "something" or "" = a relative path, will be avaluated relative to the user's url (returned in cookie)
 *      "/dashboard" = an absolute path, will be used exactly as given
 * 
 */

(function( $ ) {
    $.fn.user = function(options) {
        log("init login plugin2", this);
        initUser();
        var config = $.extend( {
            urlSuffix: "/.ajax",
            afterLoginUrl: "index.html",
            logoutSelector: ".logout",
            valiationMessageSelector: "#validationMessage",
            loginFailedMessage: "Sorry, those login details were not recognised"
        }, options);  
  
        $(config.logoutSelector).click(function() {
            doLogout();
        });
  
        var container = this;
        $("form", this).submit(function() {
            log("login", window.location);
            
            $(config.valiationMessageSelector, this).hide(100);
            try {
                $.ajax({
                    type: 'POST',
                    url: config.urlSuffix,
                    data: {
                        _loginUserName: $("input[type=text]", container).val(),
                        _loginPassword: $("input[type=password]", container).val()
                    },
                    dataType: "json",
                    success: function(resp) {
                        log("login success", resp)
                        initUser();                
                        if( userUrl ) {
                            if( config.afterLoginUrl == null) {
                                // do nothing
                            } else if( config.afterLoginUrl.startsWith("/")) {
                                //alert("would redirect to: " + config.afterLoginUrl);
                                //return;
                                window.location = config.afterLoginUrl;
                            } else {
                                //alert("would redirect to: " + userUrl + config.afterLoginUrl);
                                //return;
                                window.location = userUrl + config.afterLoginUrl;
                            }
                        } else {
                            // null userurl, so login was not successful
                            $(config.valiationMessageSelector, container).text(config.loginFailedMessage);
                            log("set message", $(config.valiationMessageSelector, this), config.loginFailedMessage);
                            $(config.valiationMessageSelector, container).show(100);                            
                        }
                        //window.location = "/index.html";
                    },
                    error: function(resp) {
                        alert("err");
                        $(config.valiationMessageSelector, container).text(config.loginFailedMessage);
                        log("set message", $(config.valiationMessageSelector, this), config.loginFailedMessage);
                        $(config.valiationMessageSelector, container).show(100);
                    }
                });                
            } catch(e) {
                log("exception sending forum comment", e);
            }            
            return false;
        });    
    };
})( jQuery );


var userUrl = null;
var userName = null;

/**
 * returns true if there is a valid user
 */
function initUser() {	
    if( userUrl ) {
        return true; // already done
    }    
    initUserCookie();
    log("initUser");
    if( isEmpty(userUrl) ) {
        // no cookie, so authentication hasnt been performed.
        log('initUser: no userUrl');
        $(".requiresuser").hide();
        $(".sansuser").show();        
        return false;
    } else {
        log("userUrl", userUrl);
        userName = userUrl.substr(0, userUrl.length-1); // drop trailing slash
        var pos = userUrl.indexOf("users");
        userName = userName.substring(pos+6);
        $("#currentuser").attr("href", userUrl);        
        $(".requiresuser").show();
        $(".sansuser").hide();        
        $("a.relativeToUser").each(function(i, node) {
            var oldHref = $(node).attr("href");
            $(node).attr("href", userUrl + oldHref);
        });
        return true;
    }
}

function initUserCookie() {    
    userUrl = $.cookie('_clydeUser');
    if( userUrl && userUrl.length > 1 ) {
        userUrl = dropQuotes(userUrl);
        userUrl = dropHost(userUrl);
        userName = userUrl.substr(0, userUrl.length-1); // drop trailing slash
        var pos = userUrl.indexOf("users");
        userName = userName.substring(pos+6);
        log('initUserCookie: user:',userUrl, userName);
    } else {
        log("initUserCookie: no user cookie");
        userName = null;
    }
}

function isEmpty(s) {
    return s == null || s.length == 0;
}

function doLogout() {
    $.ajax({
        type: 'POST',
        url: "/index.html",
        data: "_clydelogout=true",
        dataType: "text",
        success: function() {
            window.location = "/index.html";
        },
        error: function() {
            alert('There was a problem logging you out');
        }
    });    
}

function doLogin(form) {
    $.ajax({
        type: 'POST',
        url: window.location.href + ".ajax",
        data: {
            _loginUserName: $("input[type=text]", form).val(),
            _loginPassword: $("input[type=password]", form).val()
        },
        dataType: "text",
        success: function() {
            alert('logged in ok');
        },
        error: function() {
            alert('There was a problem logging you in');
        }
    });
}

function initUsage() {
    initUser();

    var url;
    if( jsonDev ) {
        url = "/sites/" + accountName + "/files/DAV/PROPFIND.txt?fields=quota-available-bytes>available,quota-used-bytes>used&depth=0";
    } else {
        url = "/sites/" + accountName + "/files/_DAV/PROPFIND?fields=quota-available-bytes>available,quota-used-bytes>used&depth=0";
    }

    $.getJSON(url, function(response) {
        var root = response[0];
        var total = root.available + root.used;
        if( total > 0 ) {
            var perc = Math.round(root.used * 100 / total);
            var totalGigs = total / 1000000000
            $(".labelTypeData").html(totalGigs + "GB");
            $(".labelDataAmount").html(perc + "%");
        } else {
            $(".labelDataAmount").html("Unknown");
        }
    });
}

function getParameter( name ) {
    name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
    var regexS = "[\\?&]"+name+"=([^&#]*)";
    var regex = new RegExp( regexS );
    var results = regex.exec( window.location.href );
    if( results == null )
        return "";
    else
        return results[1];
}

// Assumes that the current page is the user
function setAccountDisabled(isDisabled, container) {
    log('setAccountDisabled', isDisabled, container);
    ajaxLoadingOn();
    $.ajax({
        type: 'POST',
        url: "_DAV/PROPPATCH",
        data: "clyde:accountDisabled=" + isDisabled,
        dataType: "json",
        success: function(resp) {
            ajaxLoadingOff();
            if( resp.length == 0 ) {
                var newClass = isDisabled ? "disabled" : "enabled";
                log('update state:', $("div", container), newClass);
                $("div", container).attr("class",newClass);
            } else {
                alert("The user could not be updated because: " + resp[0].description);
            }
        },
        error: function(resp) {
            ajaxLoadingOff();
            log("failed to enable account", resp);
            alert("Sorry, the account could not be updated. Please check your internet connection");
        }
    });
} 

function dropQuotes(s) {
    if( s.startsWith("\"") ) {
        s = s.substr(1);
    }
    if( s.endsWith("\"") ) {
        s = s.substr(0, s.length-1);
    }    
    return s;
}

function dropHost(s) {
    var pos = s.indexOf("/",8);
    log("pos",pos);
    s = s.substr(pos);
    return s;
}


/** End jquery.login.js */