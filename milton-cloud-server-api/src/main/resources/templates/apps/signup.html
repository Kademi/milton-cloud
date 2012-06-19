<#import "defaultLayout.ftl" as layout>
<@layout.myLayout "Signup" "register">

<h1>Register a spliffy account</h1>

<form id="signup" method="POST" action="${page.name}" onsubmit="doSignup(); return false;" style="float: left; width: 400px">
    <fieldset>
        <legend>Please enter your login details</legend>
        <div class="pageMessage"></div>
        <label for="name">User name</label>
        <input type="text" name="name" id="name" class="required" />
        <br/>

        <label for="password">Password</label>
        <input type="password" name="password" id="password" class="required" />
        <br/>

        <label for="confirmPassword">Confirm password</label>
        <input type="password" name="confirmPassword" id="confirmPassword" class="required" />
        <br/>

        <label for="email">Email</label>
        <input type="text" name="email" id="email" class="required" />
        <br/>

        <button>Signup now!</button>

        <p>Disclamier: Spliffy is quite new, a bit flaky and I'm not really sure if it works properly yet. Use it at your own risk!</p>
    </fieldset>

</form>

<div style="float: right; width: 300px;">
    <h1 id="site-title"><span><a href="http://spliffy.org" title="Yellow!" rel="home">Spliffy!</a></span></h1>
    <h2 id="site-description">My personal cloud server</h2>
</div>

<div id="signupNext" style="display: none">
    <p>Click next to login to your dashboard..</p>
    <a href="#" class="button" style="float: right">Next</a>
</div>

<script type="text/javascript">
    function doSignup() {
        resetValidation();
        var container = $("#signup");
        if( !checkRequiredFields(container)) {
            return false;
        }
        try {
            $.ajax({
                type: 'POST',
                url: "${page.name}",
                data: container.serialize(),
                dataType: "json",
                success: function(resp) {
                    if( resp.status ) {
                        log("save success", resp)
                        showNext(resp.nextHref);
                    } else {
                        alert("Failed: " + resp.messages);
                    }
                },
                error: function(resp) {
                    alert("err");
                    $(config.valiationMessageSelector, container).text(config.loginFailedMessage);
                    log("set message", $(config.valiationMessageSelector, this), config.loginFailedMessage);
                    $(config.valiationMessageSelector, container).show(100);
                }
            });                
        } catch(e) {
            ajaxLoadingOff();
            log("exception sending forum comment", e);
        }        
        return false;
    }
    function showNext(href) {
        $("#signupNext a").attr("href", href);
        $("#signupNext").dialog({
            title: "You're all signed up - thanks!",
            modal: true 
        });
    
    }
</script>

</@layout.myLayout>