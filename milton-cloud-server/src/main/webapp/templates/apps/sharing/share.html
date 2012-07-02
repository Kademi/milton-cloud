<!-- Home page for the spliffy instance -->
<#import "defaultLayout.ftl" as layout>
<@layout.myLayout "Share" false>

<h1>Folder share invitation</h1>

<#if page.currentUser??>
<form id="acceptForm" action="." method="POST" onsubmit="return doAccept()">
    <fieldset>
        <legend>Accept folder share invitation</legend>
        <label for="recipEntity">Recipient</label>
        <input type="text" name="recipEntity" id="recipEntity" value="${user.name}" />
        <label for="sharedAsName">Name of folder</label>
        <input type="text" name="sharedAsName" id="sharedAsName" value="${page.share.sharedFrom.name}" />
        <button>Accept</button>
    </fieldset>
</form>
<#else>
<a href="${page.name}?login">Please click here to login to accept the invitation</a>
</#if>

<script type="text/javascript">
    function doAccept() {
        $.ajax({
            type: 'POST',
            url: "${page.name}",
            data: $("#acceptForm").serialize(),
            dataType: "json",
            success: function(resp) {
                log("resp", resp);
                if( resp.status ) {
                    alert("Share accepted ok");
                    $("#shareModal").dialog("close");
                }
            },
            error: function() {
                alert('There was a problem submitting your share invitations');
            }
        });  
        return false;
    }
</script>

</@layout.myLayout>