
function initAjaxUploads(ajaxUploadCompleteHandler) {
    var button = $('#doUpload');
    log("initAjaxUploads", button, ajaxUploadCompleteHandler);
    try {
        new AjaxUpload(button,{
            action: '_DAV/PUT',
            name: 'picd',
            autoSubmit: true,
            responseType: 'json',
            onSubmit : function(file, ext){
                log("ajaxUploads:onSubmit", file, ext);
                if( !userUrl ) {
                    alert('Please login to upload files');
                    return;
                }
                $("span", button).text('Upload...');
                this.disable();
            },
            onComplete: function(file, response){
                log("ajaxUploads:onComplete", file, response);
                $("span", button).text('Upload again');
                this.enable();
                for( i=0; i<response.length; i++ ) {
                    var f = response[i];
                    log(" - response file", f, i);
                    if( ajaxUploadCompleteHandler ) {
                        log(" - calling callback");
                        var f2 = {
                            name: f.originalName, 
                            type: f.contentType
                        };
                        ajaxUploadCompleteHandler(f2);
                    } else {
                        log(" no callback");
                    }
                //      	     $("#cvHidden").attr("value",file.href);
                //      	     $("#cvLink").html(file.originalName + "(" + file.length + ")");
                //      	     $("#cvLink").attr("href",file.href);
                }
            }          
        });
    } catch(e) {
        log("exception", e);
    }
}

function showUploadModal() {
    $("#uploads").dialog({
        modal: true,
        width: 600,
        title: "Upload"
    });
}

// onUploaded callback
(function($){
    $.fn.dragUploadable = function(postURL, fieldName, options) {

        var defaults = {
            dragenterClass: "",
            dragleaveClass: "",
            dropListing: "#dropListing",
            loaderIndicator: "#fileDropContainer .progress"
        };
        var options = $.extend(defaults, options);
		
        //initAjaxUploads(options.onUploaded);
        log(" - options.onUploaded", options.onUploaded);

        $("#filemanUpload").click(function() {
            log("upload clicked");
            showUploadModal();
            initAjaxUploads(options.onUploaded);
            log("upload clicked - done");
        });
	
        return this.each(function() {
            obj = $(this);
            obj.bind("dragenter", function(event){
                obj.removeClass(options.dragleaveClass);
                obj.addClass(options.dragenterClass);
                event.stopPropagation();
                event.preventDefault();
            }, false);
            obj.bind("dragover", function(event){
                event.stopPropagation();
                event.preventDefault();
            }, false);
            obj.bind("dragleave", function(event){
                obj.removeClass(options.dragenterClass);
                obj.addClass(options.dragleaveClass);
                event.stopPropagation();
                event.preventDefault();
            }, false);
            obj.bind("drop", function(event){
                var data = event.originalEvent.dataTransfer;
                event.stopPropagation();
                event.preventDefault();
                addToList(event.originalEvent.dataTransfer, options.dropListing);
                upload(postURL, fieldName, data, options.loaderIndicator, options.onUploaded);
            }, false);
        });
    };
})(jQuery);

function dropSetup() {
    var dropContainer = document.getElementById("output");

    dropContainer.addEventListener("dragenter", function(event){
        dropContainer.innerHTML = 'DROP';
        event.stopPropagation();
        event.preventDefault();
    }, false);
    dropContainer.addEventListener("dragover", function(event){
        event.stopPropagation();
        event.preventDefault();
    }, false);
    dropContainer.addEventListener("drop", upload, false);
};

function upload(postURL, fieldName, dataTransfer, loaderIndicator, onUploaded) {
    if( dataTransfer.files ) {
        log("upload", postURL, fieldName, dataTransfer.files.length);
        $(loaderIndicator).text("Starting...");
        $(loaderIndicator).show();
        $.each(dataTransfer.files, function ( i, file ) {
            log("send file", file.fileName);
            var xhr    = new XMLHttpRequest();
            var fileUpload = xhr.upload;
            fileUpload.addEventListener("progress", function(event) {
                if (event.lengthComputable) {
                    var percentage = Math.round((event.loaded * 100) / event.total);				
                    if (percentage < 100 && loaderIndicator) {
                        log("percent complete", percentage);
                        $(loaderIndicator).show();
                        $(loaderIndicator).css("width", percentage + "%");
                        if(percentage >= 99 ) {
                            $(loaderIndicator).text("Uploaded, now processing thumbnails..");
                        } else {
                            $(loaderIndicator).text(percentage + "%");
                        }
                    }
                }
            }, false);
				
            fileUpload.addEventListener("load", function(event) {
                log("upload: finished");
                $(loaderIndicator).css("width", "100%");
                $(loaderIndicator).text("Finished");
                if( onUploaded ) {
                    onUploaded(file);
                }
            }, false);
				
            fileUpload.addEventListener("error", function(event) {
                $(loaderIndicator).text("Error");
            }
            , false);
            xhr.open('PUT', postURL + "/" + file.fileName, true);
            xhr.setRequestHeader('X-Filename', file.fileName);
 
            xhr.send(file);
        });
    } else {
        log("upload: no files to upload");
    }
}

function addToList(dataTransfer, dropListing) {	
    log("addToList");
    var files = dataTransfer.files;
    if( files ) {
        for (i = 0; i < files.length; i++) {
            var f = files[i];
            var reader = new FileReader();
            reader.onload = (function(theFile) {
                return function(e) {
                    var li = $("<li>");
                    var img = $("<img>");
                    $(dropListing).append(li);
                    li.append(img);
	
                    var data = e.target.result;
                    img.attr("src", data); // base64 encoded string of local file(s)
                    img.attr("width", 150);
                    img.attr("height", 150);		
                    log("done addToList");	
                };
            })(f);
            reader.readAsDataURL(f);
        }	
    } else {
        log("no files droppped? must be IE...");
    }
}

