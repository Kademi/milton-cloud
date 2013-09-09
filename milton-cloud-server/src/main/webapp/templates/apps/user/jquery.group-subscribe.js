/**
 * Enables buttons and checkboxes to display and update the subscription status
 * for the current user.
 * 
 * This is generally to control membership of opt-in user groups
 * 
 * To use it, initialise the plugin on the checkbox, button or checkbox/slider
 * <input class='myGroup' type='checkbox' />
 * 
 * $(".myGroup").subscribe({});
 * 
 * Config options and their defaults are:
 *     - group: "everyone",
 *       The group to control access to
 *       
 *     - optedInClass: "opted-in"
 *       The class to add to the control when the user is opted into the group
 *       
 *      - optedOutClass: "opted-out",
 *      The class to add to the control when the user is not opted into the group
 *      
 *      - parentContainer: null,
 *      When given optin/optout classes will be added to this parent of the control
 *        
 *      - events: default depends on container type
 *      The events to be listened to for changes to the control
 *      
 *      - subscribeEvents: null,
 *      If given, when these events fire the user will be subscribed
 *      
 *      - unsubscribeEvents: null,
 *      If given, when these events fire the user will be unsubscribed
 *      
 *      - onInit
 *      Called when we get the initial state. The state is passed as a boolean, where true=subscribed
 *      
 *      - onSuccess: function(newSelectedVal)
 *      Callback when the user has been subscribed or unsubscribed. 
 *      
 *      - onError: function(resp)
 *      Callback when an error occurs updating the subscription
 *      
 *      - isCheckbox: container.is(":checkbox")
 *      If true will treat the given control as a checkbox
 *      
 *      - isButton: container.is("button")
 *      Not really used
 * 
 * @param {type} $
 * @returns {undefined}
 */
(function($) {

    var methods = {
        init: function(options) {
            var container = this;

            var defaultEvents;
            if( container.is("button") ) {
                defaultEvents = "click";
            } else {
                defaultEvents = "change switch-change";
            }

            var config = $.extend({
                group: "everyone",
                optedInClass: "opted-in",
                optedOutClass: "opted-out",
                parentContainer: null,
                events: defaultEvents,
                subscribeEvents: null,
                unsubscribeEvents: null,
                onInit: function(selectedVal) {
                    
                },
                onSuccess: function(newSelectedVal) {
                },
                onError: function(resp) {
                    alert("Error updating subscription details");
                },
                isCheckbox: container.is(":checkbox"),
                isButton: container.is("button")
            }, options);
            log("checkbox?", config.isCheckbox, "button?", config.isButton);
            // set initial state
            initSubscribeInitialState(container, config);

            log("init group subscribe", container);
            if (config.events !== null && config.events.length > 0) {
                container.on(config.events, function(e) {
                    log("change", e);
                    onChangeSubscribeState(container, config);
                });
            }
            if (config.subscribeEvents !== null) {
                log("listen for subcribe events", config.subscribeEvents);
                container.on(config.subscribeEvents, function(e) {
                    log("do subscribe");
                    changeSubscribeState(container, config, true);
                });
            }
            if (config.unsubscribeEvents !== null) {
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
                displaySubscribeState(container, config, optin.selected);
                config.onInit(optin.selected);
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

function displaySubscribeState(container, config, selected) {
    if (selected) {
        if( config.isCheckbox) {
            container.attr("checked", "true");
        }
        container.removeClass(config.optedOutClass);
        container.addClass(config.optedInClass);
        if( config.parentContainer ) {
            var p = container.closest(config.parentContainer);
            p.removeClass(config.optedOutClass);
            p.addClass(config.optedInClass);            
        }
            
    } else {
        if( config.isCheckbox) {
            container.removeAttr("checked");
        }
        container.removeClass(config.optedInClass);
        container.addClass(config.optedOutClass);
        if( config.parentContainer ) {
            var p = container.closest(config.parentContainer);
            p.addClass(config.optedOutClass);
            p.removeClass(config.optedInClass);            
        }
        
    }
}

function onChangeSubscribeState(container, config) {
    var newSelectedVal;
    if( config.isCheckbox ) {
        newSelectedVal = container.is(":selected") || container.is(":checked");
    } else {
        var prevValue;
        if( container.hasClass(config.optedInClass) ) {
            prevValue = true;
        } else {
            prevValue = false;
        }
        newSelectedVal = !prevValue;
    }
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
                log("changeSubscribeState: finished", newSelectedVal, container);
                displaySubscribeState(container, config, newSelectedVal);
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