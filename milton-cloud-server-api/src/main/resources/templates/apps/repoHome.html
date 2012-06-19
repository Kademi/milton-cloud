<#import "defaultLayout.ftl" as layout>
<@layout.dirLayout "Repository home" "dashboard">
<a class="share" href="javascript:showShareModal()">Share this folder</a>
<script type="text/javascript">
    function showShareModal() {
        $("#shareModal").dialog({
            modal: true,
            title: "Share this folder",
            width: "400px",
            resizable: false,
            dialogClass: "noTitle"
        });        
    }
    function submitShare() {
        $.ajax({
            type: 'POST',
            url: ".",
            data: $("#shareModal").serialize(),
            dataType: "json",
            success: function(resp) {
                log("resp", resp);
                if( resp.status ) {
                    alert("Share invitations sent");
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
<#list page.children as x>
  
</#list>  

<form action="." method="POST" onsubmit="return submitShare()" style="display: none" id="shareModal">
    <fieldset>
        <legend style="display: none">Share this folder</legend>
        <label for="shareWith">Who to invite</label>
        <textarea id="shareWith" name="shareWith"></textarea>
        <br/>
        <label for="message">Message</label>
        <br/>
        <textarea id="message" name="message"></textarea>
        <br/>
        <label for="priviledge">Access</label>
        <select name="priviledge" id="priviledge">
            <option value="READ">Read only</option>
            <option value="WRITE">Read and write</option>
        </select>
        <button>Send invitations</button>
    </fieldset>
</form>

</@layout.dirLayout>