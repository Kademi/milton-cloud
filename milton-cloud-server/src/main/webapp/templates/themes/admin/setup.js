$(function() {
    $(".createWebsite").click(function() {
        log("click");
        myPrompt("createWebsite", "websites/", "Create website", "Enter a short identifier (not the domain name) for the website." ,"Enter a name","newName", "Create", "simpleChars", "Enter a simple name for the website, eg myweb", function(newName, form) {
            log("create website", newName, form);
            postForm(form, ".pageMessage", "Please enter a valid name", function() {
                closeMyPrompt();
                window.location.reload();
            });
            return false;
        });
    });
});