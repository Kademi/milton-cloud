/**
 *
 *  jquery.journal.js
 *  Version: 1.0.0
 * 
 */

$(document).ready(function() {
    $("body").journal();
});

(function($) {
    $.fn.journal = function(options) {
        var container = this;
        var config = $.extend({
            journalBaseUrl: userUrl + "journal",
            journalName: getFileName(getFolderPath(window.location.pathname)),
            journalItem: getFileName(window.location.pathname)
        }, options);
        var journalDiv = container.find("div.journal");
        var journalNotes;
        if (journalDiv.length === 0) {
            journalDiv = $("<div class='journal'><img class='ajax-loader' src='/static/common/loading.gif'/><button class='journal-show' ><span>Journal</span></button></div>");
            container.append(journalDiv);
            journalNotes = $("<div class='journal-notes'></div");
            journalDiv.append(journalNotes);
        } else {
            var journalNotes = journalDiv.find("journal-notes");
        }
        journalDiv.find("button.journal-show").click(function(e) {
            e.preventDefault();
            var target = $(e.target);
            var cont = target.closest(".journal").find(".journal-notes");
            if (cont.is(":visible")) {
                cont.hide(100);
                journalDiv.removeClass("active");
            } else {
                journalDiv.addClass("active");
                showJournalNotes(journalNotes, config);                
            }
        });
        // add css
        if ($("link[href=\"/templates/apps/journal/journal.css\"]").length === 0) {
            var link = $("<link rel='stylesheet' type='text/css' media='screen' href='/templates/apps/journal/journal.css'/>");
            $("head").append(link);
            log("added journal css", link);
        }
    };
})(jQuery);



var isLoaded = false;

function showJournalNotes(div, config) {
    try {
        div.show(200);
        if (isLoaded) {
            return;
        }
        isLoaded = true;

        var baseUrl = config.journalBaseUrl + "/" + config.journalName;
        var url = baseUrl + "/_DAV/PROPFIND?fields=name,milton:textContent&depth=1";

        div.html("");

        var thisPage = getFileName(window.location.pathname);
        log("thisPage", thisPage);
        var current = addJournalEntry(div, thisPage);

        $.ajax({
            type: 'GET',
            url: url,
            dataType: "json",
            success: function(resp) {
                try {
                    log("got jounrals", resp.length);
                    ajaxLoadingOff();
                    for (var i = 1; i < resp.length; i++) {
                        var r = resp[i];
                        if (!r.name.startsWith(".") && r.name.length > 0) {
                            var textarea = addJournalEntry(div, r.name, r.title);
                            textarea.val(r.textContent);
                            log("r", r, textarea, r.textContent);
                        }
                    }
                    autoSaveJournals(div, config);
                    log("set focus", current);
                    current.focus();
                } catch (e) {
                    log("exception showing jounral entries", e);
                }
            },
            error: function(resp) {
                ajaxLoadingOff();
                if (resp.status === 404) {
                    log("no journal, create it", config);
                    var parentPath = getFolderPath(config.journalBaseUrl);
                    var baseName = getFileName(config.journalBaseUrl);
                    createFolder(baseName, parentPath, function() {
                        createFolder(config.journalName, config.journalBaseUrl);
                    });
                    autoSaveJournals(div, config);
                    current.focus();
                } else {
                    log("failed to get journal", resp);
                    alert('Sorry, we could not load journal entries');
                }
            }
        });
    } catch (e) {
        log("exception", e);
    }
}


function autoSaveJournals(container, config) {
    log("autoSaveJournals", config);
    sortJournalEntries(container);
    textareas = container.find("textarea");
    container.on("keyup", "textarea", function(e) {
        typewatch(function() {
            log("do save", config);
            try {
                saveJournal($(e.target), config);
            } catch (e) {
                log("exception doing save", e);
            }
        }, 500);
    });
    container.on("focus", "textarea", function(e) {
        log("focus", e.target);
        textareas.parent().removeClass("active");
        $(e.target).parent().addClass("active");
    });
    $(document).on("pjaxComplete", function() {
        try {
            textareas.parent().removeClass("active");
            var thisPage = getFileName(window.location.pathname);
            var newSelected = textareas.parent().find("[name='" + thisPage + "']").parent();
            if (newSelected.length === 0) {
                var newTextArea = addJournalEntry(container, thisPage);
                sortJournalEntries(container);
                newSelected = newTextArea.closest(".journal-entry");
            }
            newSelected.addClass("active");
            log("container", container.scrollTop());
            log("newSelected top: ", newSelected.scrollTop());
            container.scrollTop(newSelected.position().top);
        } catch (e) {
            log("Exception in pjaxComplete", e);
        }
    });
}

/**
 * Add a new journal entry, or locate an existing one with the given name
 * 
 * @param {div} container
 * @param {string} name
 * @returns {textarea}
 */
function addJournalEntry(container, name) {
    log("addJournalEntry", name);
    var textarea = container.find("textarea[name='" + name + "']")
    var entryDiv;
    if (textarea.length === 0) {
        entryDiv = $("<div class='journal-entry'><a href='" + name + "'>" + name + "</a><br/><textarea style='width:100%; height: 50px'></textarea></div>");
        textarea = entryDiv.find("textarea");
        textarea.attr("name", name);
        container.append(entryDiv);
        window.setTimeout(function() {
            textarea.height(textarea[0].scrollHeight);
        }, 1);
    } else {
        entryDiv = textarea.closest("div");
    }
    return textarea;
}

function saveJournal(textarea, config) {
    var notes = textarea.val();
    var url = config.journalBaseUrl + "/" + config.journalName + "/_DAV/PUT";
    log("saveJournal", config, notes, url);
    var pageName = textarea.attr("name");
    var journalDiv = textarea.closest(".journal");
    journalDiv.addClass("ajax-processing");
    $.ajax({
        type: 'POST',
        url: url,
        data: {
            content: notes,
            name: pageName,
            overwrite: "true"
        },
        dataType: "json",
        success: function(resp) {
            log("saveJournal: success", resp);
            ajaxLoadingOff();
            journalDiv.removeClass("ajax-processing");
            // todo
        },
        error: function(resp) {
            ajaxLoadingOff();
            journalDiv.removeClass("ajax-processing");
            if (resp.status === 404) {
                log("no journal");
                var entryDiv = $("<div class='journal-entry'><textarea style='width:100%; height: 50px'></textarea></div>");
                div.append(entryDiv);
                entryDiv.find("textarea").keyup(function() {
                    typewatch(function() {
                        log("do save journal");
                        saveJournal(entryDiv.find("textarea"), baseUrl);
                    }, 500);
                });
            } else {
                log("failed to get journal", resp);
                alert('Sorry, we could not load journal entries');
            }
        }
    });

}

function sortJournalEntries(div) {
    function sortAlpha(a, b) {
        var t1 = $(a).find("textarea");
        var t2 = $(b).find("textarea");
        return t1.attr("name") > t2.attr("name") ? 1 : -1;
    }
    ;

    var arr = $('.journal-entry').sort(sortAlpha);
    div.html("<h3>Journal</h3><p>Just jot down whatever notes you want to keep, these will be saved automatically in a seperate box for each page.</p><hr/>");
    div.append(arr);
}
