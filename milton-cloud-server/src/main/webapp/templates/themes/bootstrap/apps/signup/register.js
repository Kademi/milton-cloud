function initRegister(afterRegisterHref) {
    log("init labels")
    var form = $("#registerForm");
    var lastTabIndex = 0;
    jQuery("#registerForm label.collapse").each(function(i, n) {
        var label = jQuery(n);
        var title = label.text();
        var input = form.find("#" + label.attr("for"));
        input.attr("title", title);
        input.attr("tabindex", i + 1);
        log("set tab index", input, i + 1);
        label.text(i + 1);
        lastTabIndex = i + 2;
    });
    form.find("button[type=submit]").attr("tabindex", lastTabIndex);
    initRegisterForms(afterRegisterHref);
}

function initRegisterForms(afterRegisterHref, callback) {
    log("initRegisterForms", jQuery("#registerForm"));
    jQuery("#registerForm").forms({
        validationFailedMessage: "Please enter your details below.",
        callback: function(resp, form) {
            if (resp.messages && resp.messages[0] == "pending") {
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


    $("#orgName").typeahead({
        minLength: 1,
        //source: ["phx1", "phx2"],
        source: function(query, process) {
            doOrgSearch(query, process);
        },
        updater: function(item) {                        
            var org = mapOfOrgs[item];
            log("item: ", item,org );
            $("#orgId").val(org.orgId);
            return item;
        }
    });

    var mapOfOrgs = {};
    function doOrgSearch(query, callback) {
        $.ajax({
            type: 'GET',
            url: "signup?jsonQuery=" + query,
            dataType: "json",
            success: function(data) {
                log("success", data)
                mapOfOrgs = {};
                orgNames = [];
                $.each(data.data, function(i, state) {
                    log("found: ", state, state.title);
                    orgNames.push(state.title);
                    mapOfOrgs[state.title] = state;
                });
                callback(orgNames);
            },
            error: function(resp, textStatus, errorThrown) {
                log("error", resp, textStatus, errorThrown);
                alert("Error querying the list of organisations");
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
