/** register.js **/

function createAccount() {
    try {
        doCreateAccount();
        return false;
    } catch(e) {
        log("exception", e);
        alert('There was an error creating your account: ' + e);
        return false;
    }
}
function doCreateAccount() {
    resetValidation();
    var container = $("#registerForm");

    if( !checkRequiredFields(container) ) {
        log('validation failed');
        alert("Some fields havent been entered correctly, please check and try again.");
        return;
    }
    
    // Little hack to give users a nice filename
    log("maybe set title", $("input[type=hidden]#title", container));
    if( $("input[type=hidden]#title", container).length > 0  ) {
        $("#title", container).val($("#nickName", container).val()); // copy the nickName field into name for a nice autoname
    }
 
    ajaxLoadingOn();
    $.ajax({
        type: 'POST',
        url: container.attr("action"),
        data: container.serialize(),
        dataType: "json",
        success: function(resp) {
            ajaxLoadingOff();
            log('success');
            if( resp ) {
                log('result');
                if( resp.status ) {                    
                    //window.location = resp.nextHref;
                    window.location = "/dashboard";
                    //alert('would redirect: ' + resp.data.url);
                    return;
                } else {
                    if( Object.keys ) {
                        var msg = "Couldnt create your account for the following reason(s):\n";
                        keys = Object.keys(resp);
                        for( i=0; i<keys.length; i++) {
                            var key = keys[i];
                            if( key != "result") {
                                var m = resp[key];
                                msg += key + " - " + m + "\n";
                            }
                        }
                        alert(msg);
                    } else {
                        alert("We're sorry, but there was an error creating your account. Please check what you've entered and try again");
                    }
                }
            } else {
                alert("An error has occured and your application might not have been submitted");
            }

        },
        error: function() {
            ajaxLoadingOff();
            alert('There was an error creating your application. This might be because of a problem with your internet connection');
        }
    });
}


/** end register.js **/