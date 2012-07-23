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
 *      null = do a location.reload()
 *      "something" or "" = a relative path, will be avaluated relative to the user's url (returned in cookie)
 *      "/dashboard" = an absolute path, will be used exactly as given
 * 
 */

(function( $ ) {
    $.fn.user = function(options) {
        log("init login plugin2", this);
        initUser();
        var config = $.extend( {
            urlSuffix: "/.login",
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
                var userName = $("input[type=text]", container).val();
                var password = $("input[type=password]", container).val();
                doLogin(userName, password, config);
            } catch(e) {
                log("exception sending forum comment", e);
            }            
            return false;
        });    
    };
})( jQuery );


function doLogin(userName, password, config) {
    $(config.valiationMessageSelector).hide();
    $.ajax({
        type: 'POST',
        url: config.urlSuffix,
        data: {
            _loginUserName: userName,
            _loginPassword: password
        },
        dataType: "json",
        acceptsMap: "application/x-javascript",
        success: function(resp) {
            log("login success", resp)
            initUser();                
            if( userUrl ) {
                if( config.afterLoginUrl == null) {
                    window.location.reload();
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
            $(config.valiationMessageSelector).text(config.loginFailedMessage);
            log("set message", $(config.valiationMessageSelector, this), config.loginFailedMessage);
            $(config.valiationMessageSelector).show(300);
        }
    });      
}

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
    userUrl = $.cookie('miltonUserUrl');
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
    log("doLogout");
    $.ajax({
        type: 'POST',
        url: "/.login",
        data: "miltonLogout=true",
        dataType: "text",
        success: function() {
            log("logged out ok, going to root...");
            window.location = "/";
        },
        error: function(resp) {
            log('There was a problem logging you out', resp);
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