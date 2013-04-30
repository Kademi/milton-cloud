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
 *  Additionally, will prompt the user to register or login for any links of class requiresUser
 *
 * Config:
 * urlSuffix: is appended to the current page url to make the url to POST the login request to. Default /.ajax
 * afterLoginUrl: the page to redirect to after login. Default index.html.  3 possibilities 
 *      null = do a location.reload()
 *      "none" - literal value "none" means no redirect
 *      "something" or "" = a relative path, will be avaluated relative to the user's url (returned in cookie)
 *      "/dashboard" = an absolute path, will be used exactly as given
 *  logoutSelector
 *  valiationMessageSelector
 *  requiredFieldsMessage
 *  loginFailedMessage
 *  userNameProperty: property name to use in sending request to server
 *  passwordProperty
 *  loginCallback: called after successful login
 * 
 */

(function($) {
    $.fn.user = function(options) {
        log("init user plugin", this);
        initUser();
        var config = $.extend({
            urlSuffix: "/.dologin",
            afterLoginUrl: null,
            logoutSelector: ".logout",
            valiationMessageSelector: ".email span",
            requiredFieldsMessage: "Please enter your credentials.",
            loginFailedMessage: "Sorry, those login details were not recognised.",
            userNameProperty: "_loginUserName",
            passwordProperty: "_loginPassword",
            loginCallback: function() {

            }
        }, options);

        $(config.logoutSelector).click(function() {
            doLogout();
        });

        var container = this;
        log("init login form", $("form", this));
        $("form", this).click(function(e) {
            log("click", e);
        });
        $("form", this).submit(function(e) {
            log("login", window.location);
            e.stopPropagation();
            e.preventDefault();

            $("input", container).removeClass("errorField");
            $(config.valiationMessageSelector, this).hide(100);
            try {
                var userName = $("input[name=email]", container).val();
                var password = $("input[type=password]", container).val();
                if (userName === null || userName.length === 0) {
                    $("input[type=text]", container).closest(".control-group")
                            .addClass("error")
                            .find("span").text(config.requiredFieldsMessage);

                    return false;
                }
                doLogin(userName, password, config, container);
            } catch (e) {
                log("exception sending forum comment", e);
            }
            return false;
        });
        log("init requiresUser links");        
        // use a body class to ensure is only inited once
        $("body").not("body.requiresUserDone").addClass("requiresUserDone").on("click", "a.requiresUser, button.requiresUser", function(e) {
            var target = $(e.target);
            log("check required user", target, userUrl);
            if (userUrl === null || userUrl === "") {
                e.preventDefault();
                showRegisterOrLoginModal(function() {
                    //target.click();
                    //log("going to", target.attr("href"));
                    //window.location.href = target.attr("href");
                    target.trigger("click");
                });
            }
            log("all good, carry on...");
        });
    };
})(jQuery);


function doLogin(userName, password, config, container) {
    log("doLogin", userName, config.urlSuffix);
    $(config.valiationMessageSelector).hide();
    var data = new Object();
    var userNameProperty;
    if (config.userNameProperty) {
        userNameProperty = config.userNameProperty;
    } else {
        userNameProperty = "_loginUserName";
    }
    var passwordProperty;
    if (config.passwordProperty) {
        passwordProperty = config.passwordProperty;
    } else {
        passwordProperty = "_loginPassword";
    }

    data[userNameProperty] = userName;
    data[passwordProperty] = password;
    $.ajax({
        type: 'POST',
        url: config.urlSuffix,
        data: data,
        dataType: "json",
        acceptsMap: "application/x-javascript",
        success: function(resp) {
            log("login success", resp)
            initUser();
            if (resp.status) {
                if (config.loginCallback) {
                    config.loginCallback();
                }
                if (config.afterLoginUrl == null) {
                    // If not url in config then use the next href in the response, if given, else reload current page
                    if (resp.nextHref) {
                        window.location.href = resp.nextHref;
                    } else {
                        window.location.reload();
                    }
                } else if (config.afterLoginUrl.startsWith("/")) {
                    // if config has an absolute path the redirect to it
                    log("redirect to: " + config.afterLoginUrl);
                    //return;
                    window.location = config.afterLoginUrl;
                } else {
                    if (config.afterLoginUrl === "none") {
                        log("Not doing redirect because afterLoginUrl=='none'");
                    } else if( config.afterLoginUrl === "reload") {
                        window.location.reload();
                        
                    } else {
                        // if config has a relative path, then evaluate it relative to the user's own url in response
                        log("redirect to2: " + userUrl + config.afterLoginUrl);
                        //return;
                        window.location = userUrl + config.afterLoginUrl;
                    }
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
    if (userUrl) {
        return true; // already done
    }
    initUserCookie();
    log("initUser");
    if (isEmpty(userUrl)) {
        // no cookie, so authentication hasnt been performed.
        log('initUser: no userUrl');
        $(".requiresuser").hide();
        $(".sansuser").show();
        $("body").addClass("notLoggedIn");
        return false;
    } else {
        log("userUrl", userUrl);
        $("body").addClass("isLoggedIn");
        userName = userUrl.substr(0, userUrl.length - 1); // drop trailing slash
        var pos = userUrl.indexOf("users");
        userName = userName.substring(pos + 6);
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
    if (userUrl && userUrl.length > 1) {
        userUrl = dropQuotes(userUrl);
        userUrl = dropHost(userUrl);
        userName = userUrl.substr(0, userUrl.length - 1); // drop trailing slash
        var pos = userUrl.indexOf("users");
        userName = userName.substring(pos + 6);
        log('initUserCookie: user:', userUrl, userName);
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
    if (s.startsWith("\"")) {
        s = s.substr(1);
    }
    if (s.endsWith("\"")) {
        s = s.substr(0, s.length - 1);
    }
    return s;
}

function dropHost(s) {
    if (!s.startsWith("http")) {
        return s;
    }
    var pos = s.indexOf("/", 8);
    log("pos", pos);
    s = s.substr(pos);
    return s;
}

function showRegisterOrLoginModal(callbackOnLoggedIn) {
    var modal = $("#registerOrLoginModal");
    if (modal.length === 0) {
        modal = $("<div class='modal hide fade'><div class='modal-header'><button type='button' class='close' data-dismiss='modal' aria-hidden='true'>&times;</button><h3>Login or Register</h3></div><div class='modal-body'>   </div></div>");
        $("body").append(modal);
        modal.find(".close").click(function() {
            closeModals();
        });
    }
    log("showRegisterOrLoginModal2");
    $.getScript("/templates/apps/signup/register.js", function() {
        $.ajax({
            type: 'GET',
            url: "registerOrLogin",
            dataType: "html",
            success: function(resp) {
                var page = $(resp);
                var r = page.find(".registerOrLoginCont");
                log("content", page, "r", r);
                modal.find(".modal-body").html(r);
                log("modal", modal);
                $(".loginCont").user({
                    afterLoginUrl: "none",
                    loginCallback: function() {
                        closeModals();                        
                        log("logged in ok, process callback", callbackOnLoggedIn);
                        $('body').trigger('userLoggedIn', [userUrl, userName]);
                        callbackOnLoggedIn();
                    }
                });
                initRegisterForms("none", function() {
                    closeModals();                     
                    log("registered and logged in ok, process callback");
                    $('body').trigger('userLoggedIn', [userUrl, userName]);
                    callbackOnLoggedIn();
                });
            },
            error: function(resp) {
                modal.find(".modal-body").html("<p>Sorry, there was a problem loading the form. Please refresh the page and try again</p>");
            }
        });

    });
    log("showModal...", showModal);
    showModal(modal);
    modal.css("top", "20%");
}

/** End jquery.login.js */