


(function( $ ) {
    
    var methods = {
        init : function( options ) { 
            var input = this;
            
            var config = $.extend( {
                btnClass: "Btn",
                modalTitle: "Select file",
                contentTypes: ["image"],
                excludedEndPaths: [".mil/"],
                basePath: "/",
                pagePath: window.location.pathname,
                showModal: function(div) {
                    $.tinybox.show(div, {
                        overlayClose: false,
                        opacity: 0
                    }); 
                },
                onSelectFile: function(selectedUrl) {}
            }, options);  
                
            log("init milton-file-select", config, input);
            var btn = $("<button type='button'>Select file</button>");            
            btn.addClass(config.btnClass);
            btn.click(function(e) {
                e.preventDefault();
                e.stopPropagation();
                modal = getModal(config);
                config.showModal(modal);
            });
            input.after(btn);
        },
        setUrl : function( url ) {
        }
    };    
    
    function getModal(config) {
        var modal = $("div.miltonFileSelect");
        if( modal.length == 0 ) {
            modal = $("<div class='Modal miltonImageSelect'><header><h3>" + config.modalTitle + "</h3><a class='Close' href='#'><span class='Hidden'>Close</span></a></header><div class='ModalContent'></div></div>");
            var modalContent = modal.find(".ModalContent");
            modalContent.html("<div class='miltonImageSelectContainer'><div class='tree'></div><div class='imageUpload'></div><div class='imageContainer'><img /></div><button type='button' class='" + config.btnClass + "'>OK</button></div></div>");
            $("body").append(modal);
            var tree = modalContent.find("div.tree");
            var previewImg = modalContent.find("img");
            tree.mtree({
                //basePath: config.basePath,
                basePath: config.basePath,
                pagePath: config.pagePath,
                excludedEndPaths: config.excludedEndPaths,
                includeContentTypes: config.contentTypes,
                onselectFolder: function(n) {
                },
                onselectFile: function(n, selectedUrl) {
                    previewImg.attr("src", selectedUrl);
                }
            });  
            modalContent.find(".imageUpload").mupload({
                buttonText: "Upload image",
                oncomplete: function(data, name, href) {
                    log("oncomplete", data);
                    tree.mtree("addFile", name, href);
                    url = href;
                }
            });   
            modalContent.find("button").click(function() {
                var url = previewImg.attr("src");
                var relUrl = url.substring(config.basePath.length, url.length)
                log("selected", url, relUrl);
                config.onSelectFile(relUrl);
                $.tinybox.close();
            });
            
        }    
        return modal;
    }
    
    $.fn.mselect = function(method) {        
        log("mselect", this);
        if ( methods[method] ) {
            return methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
        } else if ( typeof method === 'object' || ! method ) {
            return methods.init.apply( this, arguments );
        } else {
            $.error( 'Method ' +  method + ' does not exist on jQuery.tooltip' );
        }           
    };
})( jQuery );
