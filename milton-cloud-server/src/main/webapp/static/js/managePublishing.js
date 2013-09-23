
function initPublishingMenu(managePage) {
    log("initPublishingMenu");
    $(".publishing .branches").on("click", "a.copy", function(e) {
        e.preventDefault();
        e.stopPropagation();
        var node = $(e.target);
        var link = node.closest("li").find("a").not(".copy");
        showCopyBranch(link, function(newName, resp) {
            var srcName = link.text().trim();
            node.closest("ul").append("<li><a href='#' class='copy'><span>Copy</span></a><a href='" + resp.nextHref + "/" + managePage + "'>" + newName + "</a></li>")
        });
    });
}

function showCopyBranch(link, callback) {
    var srcHref = link.attr("href");
    srcHref = toFolderPath(srcHref);
    myPrompt("copyVersion", "", "Copy website version", "Make a copy of this version of the website" ,"Enter a name","newName", "Copy", "simpleChars", "Enter a simple name for the version, eg version2", function(newName, form) {
        newName = newName.toLowerCase();
        log("create version", newName, form);
        createBranch(srcHref, newName, function(newName, resp) {
            closeMyPrompt();
            callback(newName, resp);
        });
        return false;
    });
}

function createBranch(src, destName, callback) {
    log("createBranch", src, destName);
    $.ajax({
        type: 'POST',
        url: src,
        data: {
            copyToName: destName
        },
        dataType: "json",
        success: function(resp) {
            log("created branch", resp);
            callback(destName, resp);
        },
        error: function(resp) {
            log("error", resp);
            alert("Sorry couldnt create new new version: " + resp );
        }
    });       
}