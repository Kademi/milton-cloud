
(function($) {

    var methods = {
        init: function(options) {
            var container = this;

            var config = $.extend({
                group: "everyone",
                optedInClass: "opted-in",
                optedOutClass: "opted-out",
                events: "change switch-change",
                subscribeEvents: null,
                unsubscribeEvents: null,
                onSuccess: function(newSelectedVal) {},
                onError: function(resp) {
                    alert("Error updating subscription details");
                }
            }, options);

            // set initial state
            initSubscribeInitialState(container, config);

            log("init group subscribe", container);
            if (config.events !== null && config.events.length > 0) {
                container.on(config.events, function(e) {
                    log("change", e);
                    onChangeSubscribeState(container, config);
                });
            }
            if( config.subscribeEvents !== null ) {
                log("listen for subcribe events", config.subscribeEvents);
                container.on(config.subscribeEvents, function(e) {
                    log("do subscribe");
                    changeSubscribeState(container, config, true);
                });                
            }
            if( config.unsubscribeEvents !== null ) {
                log("listen for unsubcribe events", config.unsubscribeEvents);
                container.on(config.unsubscribeEvents, function(e) {
                    log("do unsubscribe");
                    changeSubscribeState(container, config, false);
                });                
            }            
            log("done group-subscribe init");
        },
        subscribe: function( ) {
            log("subscribe", this);
            changeSubscribeState(container, config, true);
        },
        unsubscribe: function( ) {
            log("subscribe", this);
            changeSubscribeState(container, config, false);
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
                    container.removeClass(config.optedOutClass);
                    container.addClass(config.optedInClass);                    
                } else {
                    container.removeAttr("checked");
                    container.removeClass(config.optedInClass);
                    container.addClass(config.optedOutClass);                        
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

function onChangeSubscribeState(container, config) {
    var newSelectedVal = container.is(":selected") || container.is(":checked");
    changeSubscribeState(container, config, newSelectedVal);
}

function changeSubscribeState(container, config, newSelectedVal) {
    try {
        container.addClass("ajax-loading");
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
                if( newSelectedVal ) {
                    container.removeClass(config.optedOutClass);
                    container.addClass(config.optedInClass);
                } else {
                    container.removeClass(config.optedInClass);
                    container.addClass(config.optedOutClass);                    
                }
                config.onSuccess(newSelectedVal);
            },
            error: function(resp) {
                ajaxLoadingOff();
                container.removeClass("ajax-loading");                
                config.onError(resp);
            }
        });
    } catch (e) {
        ajaxLoadingOff();
        log("exception", e);
    }
}