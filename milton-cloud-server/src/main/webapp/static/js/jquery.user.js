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
            urlSuffix: "/.dologin",
            afterLoginUrl: null,
            logoutSelector: ".logout",
            valiationMessageSelector: "#validationMessage",
            requiredFieldsMessage: "Please enter your credentials.",
            loginFailedMessage: "Sorry, those login details were not recognised."
        }, options);  
  
        $(config.logoutSelector).click(function() {
            doLogout();
        });
  
        var container = this;
        $("form", this).submit(function() {
            log("login", window.location);
            
            $("input", container).removeClass("errorField");
            $(config.valiationMessageSelector, this).hide(100);
            try {
                var userName = $("input[name=email]", container).val();
                var password = $("input[type=password]", container).val();
                if( userName == null || userName.length == 0 ) {
                    $("input[type=text]", container).addClass("errorField");
                    $(config.valiationMessageSelector, container).text(config.requiredFieldsMessage);
                    $(config.valiationMessageSelector, container).show(200);
                    return false;
                }
                doLogin(userName, password, config, container);
            } catch(e) {
                log("exception sending forum comment", e);
            }            
            return false;
        });    
    };
})( jQuery );


function doLogin(userName, password, config, container) {
    log("doLogin", userName, config.urlSuffix);
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
            if( resp.status ) {
                if( config.afterLoginUrl == null) {
                    // If not url in config then use the next href in the response, if given, else reload current page
                    if( resp.nextHref ) {
                        window.location.href = resp.nextHref;
                    } else {
                        window.location.reload();
                    }                    
                } else if( config.afterLoginUrl.startsWith("/")) {
                    // if config has an absolute path the redirect to it
                    log("redirect to1: " + config.afterLoginUrl);
                    //return;
                    window.location = config.afterLoginUrl;
                } else {
                    // if config has a relative path, then evaluate it relative to the user's own url in response
                    log("redirect to2: " + userUrl + config.afterLoginUrl);
                    //return;
                    window.location = userUrl + config.afterLoginUrl;
                }
            } else {
                // null userurl, so login was not successful
                $(config.valiationMessageSelector, container).text(config.loginFailedMessage);
                log("null userUrl, so failed. Set validation message message", $(config.valiationMessageSelector, this), config.loginFailedMessage);
                $(config.valiationMessageSelector, container).show(200);
            }
        //window.location = "/index.html";
        },
        error: function(resp) {
            $(config.valiationMessageSelector).text(config.loginFailedMessage);
            log("error response from server, set message. msg output:", $(config.valiationMessageSelector, this), "config msg:", config.loginFailedMessage, "resp:", resp);
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
        url: "/.dologin",
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