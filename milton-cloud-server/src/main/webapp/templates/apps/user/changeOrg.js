$(function() {
    $('#myModal').on('shown', function() {
        initModal();
    })
});
function initModal() {
    log("initModal", $("#changeMemberOrgForm"));
    $("#changeMemberOrgForm").forms({
        callback: function(resp, form) {
            window.location.reload();
        }
    });

    $("#orgTitle").typeahead({
        minLength: 2,
        source: function(query, process) {
            doOrgSearch(query, process);
        },
        updater: function(item) {
            var org = mapOfOrgs[item];
            log("item: ", item, org);
            $("#orgId").val(org.id);
            return item;
        }
    });

    var mapOfOrgs = {};
    function doOrgSearch(query, callback) {
        $.ajax({
            type: 'GET',
            url: window.location.pathname + "?changeMemberOrg=" + $("#changeMemberOrg").val() + "&orgSearchQuery=" + query,
            dataType: "json",
            success: function(data) {
                log("success", data)
                mapOfOrgs = {};
                orgNames = [];
                $.each(data.data, function(i, state) {
                    //log("found: ", state, state.title);
                    orgNames.push(state.title);
                    mapOfOrgs[state.title] = state;
                });
                callback(orgNames);
            },
            error: function(resp, textStatus, errorThrown) {
                log("error", resp, textStatus, errorThrown);
                alert("Error querying the list of organisations");
            }
        });
    }
}