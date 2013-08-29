function initManageEmail() {
    log("initManageEmail");
    stripList();
    initController();
    initList();
    initSortableButton();
    checkCookie();
    initAddJob();
}

function initShowRecips() {
    log("initshowrecips", $(".showRecipients button"));
    $(".showRecipients button").click(function(e) {
        e.preventDefault();
        var table = $(e.target).closest("div").find("table");
        table.show(200);
        showRecipients(table.find("tbody"));
    });
}


function initEditEmailPage() {
    initShowRecips();
    initRemoveRecipientGroup();
    addGroupBtn();
    eventForModal();
    initGroupCheckbox();

    checkPasswordResetVisible();

    jQuery("#passwordReset").change(function() {
        checkPasswordResetVisible();
    });


    initStatusPolling();
    $("button.send").click(function(e) {
        e.stopPropagation();
        e.preventDefault();
        if( $("body").hasClass("dirty") ) {
            alert("Please save your changes before sending the email");
        } else {
            sendMail();
        }
    });
    $("button.preview").click(function(e) {
        e.stopPropagation();
        e.preventDefault();
        if( $("body").hasClass("dirty") ) {
            alert("Please save your changes before sending the preview");
        } else {
            previewMail();
        }                
    });
    $("input, select, textarea").change(function() {
        $("body").addClass("dirty");
    });
}

function checkPasswordResetVisible() {
    log("checkPasswordResetVisible");
    var cont = $(".passwordResetContainer");
    var inp = cont.find("input[type=text]");
    if ($("#passwordReset:checked").length > 0) {
        cont.show(100);
        inp.addClass("required");
        if (inp.val() == "") {
            inp.val("Please click here to reset your password");
        }
    } else {
        cont.hide(100);
        inp.removeClass("required");
    }
}

function initRemoveRecipientGroup() {
    log("initRemoveRecipientGroup");
    $("ul.GroupList").on("click", "li a", function(e) {
        log("click", this);
        e.preventDefault();
        e.stopPropagation();
        if (confirm("Are you sure you want to remove this group from the recipient list?")) {
            var a = $(e.target);
            log("do it", a);
            var href = a.attr("href");
            deleteFile(href, function() {
                a.closest("li").remove();
                $("#modalGroup input[type=checkbox][name=" + href + "]").removeAttr("checked");
            });
        }
    });
}


function initGroupCheckbox() {
    $("#modalGroup input[type=checkbox]").click(function() {
        var $chk = $(this);
        log("checkbox click", $chk, $chk.is(":checked"));
        var isRecip = $chk.is(":checked");
        setGroupRecipient($chk.attr("name"), isRecip);
    });
}

function setGroupRecipient(name, isRecip) {
    log("setGroupRecipient", name, isRecip);
    try {
        $.ajax({
            type: 'POST',
            url: window.location.href,
            data: {
                group: name,
                isRecip: isRecip
            },
            success: function(data) {
                log("saved ok", data);
                if (isRecip) {
                    $(".GroupList").append("<li class=" + name + "><span>" + name + "</span><a href='" + name + "'>Delete</a></li>");
                    log("appended to", $(".GroupList"));
                } else {
                    $(".GroupList li." + name).remove();
                    log("removed from", $(".GroupList"));
                }
            },
            error: function(resp) {
                log("error", resp);
                alert("err");
            }
        });
    } catch (e) {
        log("exception in createJob", e);
    }
}

function showAddJob() {
    var _modal = $("#modalCreateJob");
    $.tinybox.show(_modal, {
        overlayClose: false,
        opacity: 0
    });
}

function initAddJob() {
    log("initAddJob", $("#moduleCreateJob form"));
    $("#modalCreateJob form").forms({
        callback: function(data) {
            log("saved ok", data);
            $.tinybox.close();
            window.location.href = data.nextHref;

        }
    });
}

function checkCookie() {
    var _sort_type = $.cookie("email-sort-type");
    if (_sort_type) {
        _sort_type = _sort_type.split("#");
        var _type = _sort_type[0];
        var _asc = _sort_type[1] === "asc" ? true : false;
        sortBy(_type, _asc);

        switch (_type) {
            case 'date':
                $("a.SortByDate").attr("rel", _asc ? "desc" : "asc");
                break;
            case 'name':
                $("a.SortByName").attr("rel", _asc ? "desc" : "asc");
                break;
            case 'status':
                $("a.SortByStatus").attr("rel", _asc ? "desc" : "asc");
                break;
        }
        ;
    }
}

function stripList() {
    $("#manageEmail .Content ul li").removeClass("Odd").filter(":odd").addClass("Odd");
}

function initController() {

    //Bind event for Delete email
    $("body").on("click", "a.DeleteEmail", function(e) {
        e.preventDefault();
        var a = $(e.target);
        log("do it", a);
        var href = a.attr("href");
        var name = getFileName(href);
        confirmDelete(href, name, function() {
            log("remove", a);
            a.closest("tr").remove();
            stripList();
        });
    });
}

function initList() {
    $("#manageEmail .Content ul li").each(function(i) {
        $(this).attr("rel", i);
    });
}
;

function initSortableButton() {
    // Bind event for Status sort button
    $("body").on("click", "a.SortByStatus", function(e) {
        e.preventDefault();

        var _this = $(this);
        var _rel = _this.attr("rel");

        if (_rel === "asc") {
            sortBy("status", true);
            $.cookie("email-sort-type", "status#asc");
            _this.attr("rel", "desc");
        } else {
            sortBy("status", false);
            $.cookie("email-sort-type", "status#desc");
            _this.attr("rel", "asc");
        }
    });

    // Bind event for Name sort button
    $("body").on("click", "a.SortByName", function(e) {
        e.preventDefault();

        var _this = $(this);
        var _rel = _this.attr("rel");

        if (_rel === "asc") {
            sortBy("name", true);
            $.cookie("email-sort-type", "name#asc");
            _this.attr("rel", "desc");
        } else {
            sortBy("name", false);
            $.cookie("email-sort-type", "name#desc");
            _this.attr("rel", "asc");
        }
    });

    // Bind event for Date sort button
    $("body").on("click", "a.SortByDate", function(e) {
        e.preventDefault();

        var _this = $(this);
        var _rel = _this.attr("rel");

        if (_rel === "asc") {
            sortBy("date", true);
            $.cookie("email-sort-type", "date#asc");
            _this.attr("rel", "desc");
        } else {
            sortBy("date", false);
            $.cookie("email-sort-type", "date#desc");
            _this.attr("rel", "asc");
        }
    });
}

function sortBy(type, asc) {
    var list = $("#manageEmail .Content ul li");
    var _list = {};
    var sortObject = function(obj) {
        var sorted = {},
                array = [],
                key,
                l;

        for (key in obj) {
            if (obj.hasOwnProperty(key)) {
                array.push(key);
            }
        }

        array.sort();
        if (!asc) {
            array.reverse();
        }

        for (key = 0, l = array.length; key < l; key++) {
            sorted[array[key]] = obj[array[key]];
        }
        return sorted;
    };

    switch (type) {
        case 'date':
            for (var i = 0, _item; _item = list[i]; i++) {
                _item = $(_item);
                var title = _item.find("span.Date").html();
                var rel = _item.attr("rel");
                _list[title + "#" + rel] = _item;
            }
            break;
        case 'name':
            for (var i = 0, _item; _item = list[i]; i++) {
                _item = $(_item);
                var title = _item.find("span.Name").html();
                var rel = _item.attr("rel");
                _list[title + "#" + rel] = _item;
            }
            break;
        case 'status':
            for (var i = 0, _item; _item = list[i]; i++) {
                _item = $(_item);
                var title = _item.find("span.Status").html();
                var rel = _item.attr("rel");
                _list[title + "#" + rel] = _item;
            }
            break;
    }

    _list = sortObject(_list);

    var _emailList = $("#manageEmail .Content ul");
    _emailList.html("");
    for (var i in _list) {
        _emailList.append(_list[i]);
    }

    stripList();
}


function addGroupBtn() {
    $('.AddGroup').on('click', function(e) {
        e.preventDefault();

        var _group = [];

        $("div.Recipient ul.GroupList li").each(function() {
            _group.push(this.getAttribute("data-group"));
        });

        showGroupModal(_group);
    });
}

function showGroupModal(group) {
    var _modal = $("#modalGroup");
    log("showGroupModal", _modal, group);
    $.tinybox.show(_modal, {
        overlayClose: false,
        opacity: 0
    });
}

function eventForModal() {
    var _modal = $("#modalGroup");

    // Bind close function to Close button
    _modal.find("a.Close").click(function(e) {
        e.preventDefault();
    });

}

function validateEmail() {
    // Check it has recipients
    if ($("ul.GroupList li").length == 0) {
        alert("Please enter at least one recipient");
        return false;
    }
    // Check it has a message
    var msg = $("textarea[name=html]").val();
    if (msg == null || msg.length == 0) {
        alert("Please enter a message to send");
        return false;
    }
    // Check subject
    var subject = $("input[name=subject]").val();
    if (subject == null || subject.length == 0) {
        alert("Please enter a subject for the email");
        return false;
    }
    // Check from address    
    var fromAddress = $("input[name=fromAddress]").val();
    if (fromAddress == null || fromAddress.length == 0) {
        alert("Please enter a from address for the email");
        return false;
    }
    // Check that if doing password reset then a theme is selected
    var sel = $("select[name=themeSiteId]");
    log("check reset", $("#passwordReset:checked"), sel);
    if ($("#passwordReset:checked").length > 0) {
        if (sel.val() == "") {
            alert("A theme is required for a password reset email. Please choose a theme on the Message tab");
            return false;
        }
    }

    return true;
}

function sendMail() {
    sendMailAjax(true);
}

function previewMail() {
    sendMailAjax(false);
}

function sendMailAjax(reallySend) {
    if (!validateEmail()) {
        return;
    }
    try {
        $.ajax({
            type: 'POST',
            url: window.location.href,
            data: {
                sendMail: "true",
                reallySend: reallySend
            },
            success: function(data) {
                log("send has been initiated", data);
                if (reallySend) {
                    alert("Email sending has been initiated. If there is a large number of users this might take some time. This screen will display progress");
                    $("a.statusTab").click();
                    $("#manageEmail button").hide();
                    $(".GroupList a").hide();
                    $("#status-tools").removeClass("Draft").addClass("Running");
                    
                    initStatusPolling();
                } else {
                    alert("The preview email has been sent to your email address. Please review it");
                }
            },
            error: function(resp) {
                log("error", resp);
                alert("Failed to start the send job. Please refresh the page");
            }
        });
    } catch (e) {
        log("exception in createJob", e);
    }
}


function initStatusPolling() {
    log("initStatusPolling");
    pollStatus();
}

function pollStatus() {
    //log("pollStatus");
    if ($("div.status:visible").length == 0) {
        //log("status page is not visible, so dont do poll");
        window.setTimeout(pollStatus, 2000);
        return;
    }
    try {
        $.ajax({
            type: 'GET',
            url: window.location.href,
            dataType: "json",
            data: {
                status: "true"
            },
            success: function(resp) {
                displayStatus(resp.data);
                if (resp.data.statusCode != "c") {
                    $("#status-tools").removeClass("Draft").addClass("Running");
                    window.setTimeout(pollStatus, 2000);
                } else {
                    log("job status is finished, so don't poll");
                    $("#status-tools").removeClass("Running").addClass("Complete");
                }
            },
            error: function(resp) {
                log("error", resp);
            }
        });
    } catch (e) {
        log("exception in createJob", e);
    }
}

function displayStatus(data) {
    log("displayStatus", data);
    var tbody = $("#emails tbody");
    if (data.statusCode) {
        $("div.status > div").hide();
        $("div.status").removeClass("status_c");
        $("div.status").removeClass("status_p");
        $("div.status").addClass("status_" + data.statusCode);
        $("div.status div.SendProgress").show();
        $("div.status div.Percent").css("width", data.percent + "%");
        var txtProgress = data.successful.length + " sent ok, ";

        if (data.failed) {
            txtProgress += data.failed.length + " failed, ";
        }
        if (data.retrying) {
            txtProgress += data.retrying.length + " retrying, ";
        }
        if (data.totalToSend) {
            txtProgress += data.totalToSend + " in total to send";
        }
        $("div.status div.stats").text(txtProgress);

        $.each(data.successful, function(i, emailId) {
            var tr = tbody.find("#" + emailId);
            log("remove", emailId, tr)
            tr.remove();
        });
        addRows(data.sending, "Sending..", tbody);
        addRows(data.retrying, "Retrying..", tbody);
        addRows(data.failed, "Failed", tbody);
    } else {
        $("div.status > div").hide();
        $("div.status div.NotSent").show();
        tbody.html("");


    }
}

function addRows(list, status, tbody) {
    $.each(list, function(i, e) {
        tr = getOrCreateEmailRow(e, tbody);
        if (e.lastError) {
            tr.find("td.status").html("<acronym title='" + e.lastError + "'>" + status + "</acronym>");
        } else {
            tr.find("td.status").text(status);
        }
        tr.find("td.attempt").text(e.retries);
    });
}

function getOrCreateEmailRow(e, tbody) {
    var tr = tbody.find("#" + e.emailId);
    if (tr.length == 0) {
        tr = $("<tr id='" + e.emailId + "'><td>" + e.email + "</td><td>" + e.fullName + "</td><td class='status'></td><td class='attempt'></td></td>");
        tbody.prepend(tr);
    }
    return tr;
}

function showRecipients(tableBody) {
    try {
        $.ajax({
            type: 'GET',
            url: window.location.pathname + "?recipients",
            dataType: "json",
            success: function(resp) {
                log("got results", resp.data.length, resp.data);
                tableBody.html("");
                if (resp.data.length > 0) {
                    $.each(resp.data, function(i, profile) {
                        var tr = $("<tr>").appendTo(tableBody);
                        $("<td>").html("<a href='/manageUsers/" + profile.userId + "'>" + profile.name + "</a>").appendTo(tr);
                        $("<td>").text(profile.email).appendTo(tr);
                        $("<td>").text(profile.firstName).appendTo(tr);
                        $("<td>").text(profile.surName).appendTo(tr);
                        log("appended", tr, tableBody);
                    });
                } else {
                    tableBody.html("<tr><td>No recipients</td></tr>");
                }
            },
            error: function(resp) {
                alert("Sorry, an error occured collecting the recipient list");
            }
        });
    } catch (e) {
        log("exception in createJob", e);
    }

}