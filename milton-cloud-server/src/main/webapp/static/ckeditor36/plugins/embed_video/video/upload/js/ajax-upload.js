$(function () {
    $('#fileupload').fileupload({
        dataType: 'json',
        done: function (e, data) {
            log("done", e, data);
        },
        progressall: function (e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            log("progress", progress);
            //$('#progress .bar').css('width',progress + '%');
        }        
    });
});

window.locale = {
    "fileupload": {
        "errors": {
            "maxFileSize": "File is too big",
            "minFileSize": "File is too small",
            "acceptFileTypes": "Filetype not allowed",
            "maxNumberOfFiles": "Max number of files exceeded",
            "uploadedBytes": "Uploaded bytes exceed file size",
            "emptyResult": "Empty file upload result"
        },
        "error": "Error",
        "start": "Start",
        "cancel": "Cancel",
        "destroy": "Delete"
    }
};
