jQuery(document).ready(function() {
    $("#calendars").stupidtable();
    $("#myUploaded").mupload({
        url: window.location.pathname,
        buttonText: "Upload ical files(s)",
        useJsonPut: false,
        oncomplete: function(data, name, href) {
            // reload the file list
            log("uploaded ok, now reload file list")
            reload()
        }
    });
    $("#contacts a").click(function(e) {
        e.preventDefault();
        showEditContact($(e.target));
    });
    $(".newEvent").click(function() {
        showNew();
    });
    $("#editContact form").forms({
        callback: function(resp) {
            log("saved", resp);
            closeModals();
            reload();
        }
    });
    $("#editContact form a.button").click(function(e) {
        e.preventDefault();
        closeModals();
    });
    $("#editContact form button.delete").click(function(e) {
        e.preventDefault();
        e.stopPropagation();
        confirmDelete($("#editContact form").attr("action"), $("#givenName").val(), function() {
            closeModals();
            reloadContacts();
        });
    });
});
function reload() {
    $.get(window.location.pathname, "", function(resp) {
        var html = $(resp);
        $("#calendars").replaceWith(html.find("#calendars"));
        $("#calendars").stupidtable();
        initPseudoClasses();
    });

}

function showNew() {
    var modal = $("#edit");
    modal.find("input").val("");
    modal.find("form").attr("action", "");
    showModal(modal);
}

function showEditContact(link) {
    var modal = $("#edit");
    $.ajax({
        type: 'GET',
        url: link.attr("href") + "/_DAV/PROPFIND?fields=ldap:telephonenumber,ldap:mail,ldap:surName,ldap:givenName",
        dataType: "json",
        success: function(data) {
            log("response", data);
            $("#givenName").val(data[0].givenName);
            $("#surName").val(data[0].surName);
            $("#telephonenumber").val(data[0].telephonenumber);
            $("#mail").val(data[0].mail);
            modal.find("form").attr("action", link.attr("href"));
            showModal(modal);
        },
        error: function(resp) {
            log("error", resp);
            alert("err");
        }
    });


}