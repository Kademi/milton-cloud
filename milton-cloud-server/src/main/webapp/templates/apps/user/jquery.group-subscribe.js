
(function($) {

    var methods = {
        init: function(options) {
            var container = this;

            var config = $.extend({
                group: "everyone"
            }, options);

            // set initial state
            initSubscribeInitialState(container, config);

            log("init group subscribe", container);
            container.on("change switch-change", function(e) {
                log("change", e);
                changeSubscribeState(container, config);
            });
            log("done fileupload init");
        },
        subscribe: function( ) {
            log("subscribe", this);
        },
        unsubscribe: function( ) {
            log("subscribe", this);
        }
    };

    $.fn.subscribe = function(method) {
        log("subscribe", this);
        if (methods[method]) {
            return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
        } else if (typeof method === 'object' || !method) {
            return methods.init.apply(this, arguments);
        } else {
            $.error('Method ' + method + ' does not exist on jQuery.subscribe');
        }
    };
})(jQuery);

function initSubscribeInitialState(container, config) {
    try {
        container.addClass("ajax-loading");
        $.ajax({
            type: 'GET',
            url: "/profile",
            dataType: "json",
            success: function(resp) {
                ajaxLoadingOff();
                container.removeClass("ajax-loading");
                log(resp, config.group);
                var optin = resp.data.optins[config.group];
                log("found", optin);
                if (optin.selected) {
                    container.attr("checked", "true");
                } else {
                    container.removeAttr("checked");
                }
            },
            error: function(resp) {
                ajaxLoadingOff();
                container.removeClass("ajax-loading");
                alert("Error loading subscription details");
            }
        });
    } catch (e) {
        ajaxLoadingOff();
        log("exception", e);
    }
}

function changeSubscribeState(container, config ) {
    try {
        container.addClass("ajax-loading");
        var newSelectedVal = container.is(":selected") || container.is(":checked");
        $.ajax({
            type: 'POST',
            url: "/profile",
            data: {
                enableOptin: newSelectedVal,
                group: config.group
            },
            dataType: "json",
            success: function(resp) {
                ajaxLoadingOff();
                container.removeClass("ajax-loading");
            },
            error: function(resp) {
                ajaxLoadingOff();
                container.removeClass("ajax-loading");
                alert("Error loading subscription details");
            }
        });
    } catch (e) {
        ajaxLoadingOff();
        log("exception", e);
    }    
}