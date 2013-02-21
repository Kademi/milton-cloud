function initRegister(afterRegisterHref) {
    log("init labels")
    var form = $("#registerForm");
    var lastTabIndex = 0;
    jQuery("#registerForm label.collapse").each(function(i, n) {
        var label = jQuery(n);
        var title = label.text();
        var input = form.find("#" + label.attr("for"));
        input.attr("title", title);
        input.attr("tabindex", i+1);
        log("set tab index", input, i+1);
        label.text(i+1);
        lastTabIndex = i+2;
    });
    form.find("button[type=submit]").attr("tabindex", lastTabIndex);
    initRegisterForms(afterRegisterHref);
}

function initRegisterForms(afterRegisterHref, callback) {
    log("initRegisterForms", jQuery("#registerForm"));
    jQuery("#registerForm").forms({
        validationFailedMessage: "Please enter your details below.",
        callback: function(resp, form) {
            if( resp.messages && resp.messages[0] == "pending" ) {
                showPendingMessage();
            } else {
                log("created account ok, logging in...", resp, form);
                var userName = form.find("input[name=email]").val();
                var password = form.find("input[name=password]").val();
                doLogin(userName, password, {
                    afterLoginUrl: afterRegisterHref,
                    urlSuffix: "/.dologin",
                    loginCallback: callback
                }, this);
            }
        }
    });
    $("#orgName").on("focus click", function() {
        jQuery.tinybox.show($("#findOrg"), {
            overlayClose: false,
            opacity: 0
        });
        $("#findOrg").find("input").focus();
    });
    $("#orgQuery").keyup(function () {
        typewatch(function () {
            log("do search");
            doOrgSearch();
        }, 500);
    });         
    $("#findOrg").on("click", "a", function(e) {
        e.preventDefault();
        e.stopPropagation();
        log("clicked", e.target);
        var orgLink = $(e.target);
        $("#orgName").val(orgLink.text());
        $("#orgId").val(orgLink.attr("href"));
        $.tinybox.close();
    });
    function doOrgSearch() {
        $.ajax({
            type: 'GET',
            url: window.location.pathname + "?q=" + $("#orgQuery").val(),
            success: function(data) {
                log("success", data)
                
                var $fragment = $(data).find("#orgSearchResults");
                $("#orgSearchResults").replaceWith($fragment);
                $fragment.show();
            },
            error: function(resp) {
                alert("err");
            }
        });      
    }            
    function showPendingMessage() {
        jQuery.tinybox.show($("#pending"), {
            overlayClose: false,
            opacity: 0
        });
    }
}