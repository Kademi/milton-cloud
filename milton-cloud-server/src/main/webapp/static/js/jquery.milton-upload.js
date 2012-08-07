


(function( $ ) {
    
    var methods = {
        init : function( options ) { 
            var container = this;
            var config = $.extend( {
                oncomplete: function(data) {
                    log("finished upload", data);
                }
                }, options);  
            log("init milton uploads", container);
            var form = $("<form action='_DAV/PUT' method='POST' enctype='multipart/form-data'></form>");
            var buttonBar = $("<div class='row fileupload-buttonbar'></div>");
            form.append(buttonBar);
            var fileInputContainer = $("<div class='muploadBtn'></div>");
            fileInputContainer.append($("<span>Add files...</span>"));
            var fileInput = $("<input type='file' name='files[]' id='fileupload' style='opacity: 0; width: 100%' />");
            fileInputContainer.append(fileInput);
            buttonBar.append(fileInputContainer);
            buttonBar.append("<div class='progress'><div class='bar'></div></div>");
            container.append(form);

            log("init fileupload", fileInput);
            fileInput.fileupload({
                dataType: 'json',
                progressInterval: 10,
                done: function (e, data) {
                    log("done", e, data);
                    var name = data.files[0].name;
                    var parentHref = form.attr("action").replace("_DAV/PUT", "");
                    var href = parentHref;
                    if( href.length > 0 && !href.endsWith("/")) href += "/";
                    href += name;
                    config.oncomplete(data, name, href);
                },
                progressall: function (e, data) {
                    log("progress", e, data)
                    var progress = parseInt(data.loaded / data.total * 100, 10);
                    log("progress", progress);
                    $('.progress').show();
                    $('.progress .bar', buttonBar).css('width',progress + '%');
                }        
            });
            log("done fileupload init");
        },
        setUrl : function( url ) {
            log("setUrl", this, url);
            var newAction = url + "_DAV/PUT";
            this.find("form").attr("action", newAction);
        }
    };    
    
    $.fn.mupload = function(method) {        
        log("mupload", this);
        if ( methods[method] ) {
            return methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
        } else if ( typeof method === 'object' || ! method ) {
            return methods.init.apply( this, arguments );
        } else {
            $.error( 'Method ' +  method + ' does not exist on jQuery.tooltip' );
        }           
    };
})( jQuery );
