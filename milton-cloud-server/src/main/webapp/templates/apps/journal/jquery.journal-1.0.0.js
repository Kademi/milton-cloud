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
            journalBaseUrl: userUrl + "journal/",
            journalName: getFileName(getFolderPath(window.location.pathname)),
            journalItem: getFileName(window.location.pathname)
        }, options);
        var journalDiv = container.find("div.journal");
        var journalNotes;
        if (journalDiv.length === 0) {
            journalDiv = $("<div class='journal'><button class='journal-show' ><span>Journal</span></button></div>");
            container.append(journalDiv);
            journalNotes = $("<div class='journal-notes'></div");
            journalDiv.append(journalNotes);
        } else {
            var journalNotes = journalDiv.find("journal-notes");
        }
        journalDiv.find("button.journal-show").click(function(e) {
            e.preventDefault();
            showJournalNotes(journalNotes, config);
        });
        // add css
        if( $("link[href=\"/templates/apps/journal/journal.css\"]").length ===0 ) {
            var link = $("<link rel='stylesheet' type='text/css' media='screen' href='/templates/apps/journal/journal.css'/>");
            $("head").append(link);
            log("added journal css", link);
        }
    };
})(jQuery);



var isLoaded = false;

function showJournalNotes(div, config) {
    div.show(200);
    if (isLoaded) {
        return;
    }
    isLoaded = true;

    var url = config.journalBaseUrl + config.journalName;
    url += "/_DAV/PROPFIND?fields=name&depth=0";

    div.html("");
    $.ajax({
        type: 'GET',
        url: url,
        dataType: "json",
        success: function() {
            ajaxLoadingOff();
            // todo
        },
        error: function(resp) {            
            ajaxLoadingOff();
            if( resp.status === 404 ) {
                log("no journal");
                var entryDiv = $("<div class='journal-entry'><textarea style='width:100%; height: 50px'></textarea></div>");
                div.append(entryDiv);
            } else {           
                log("failed to get journal", resp);
                alert('Sorry, we could not load journal entries');
            }
        }
    });
}

