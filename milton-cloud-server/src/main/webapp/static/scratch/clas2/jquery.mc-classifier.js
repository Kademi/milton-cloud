


(function( $ ) {
    
    var methods = {
        init : function( options ) { 
            var container = this;
            var displayDiv;
            var displaySpan;            
            var config = $.extend( {
    
            }, options
            );  
            $("tbody tr td", container).wrapInner("<div>");
            $("tbody tr td:first-child div", container).append("<span></span>");
                
            $("tbody tr td:last-child div", container).addClass("clasContent");
            $("div.clasContent", container).wrap("<div class='rule'>");
            $("div.rule", container).wrap("<div class='graph'>");
            var firstContentCell = $("tr:first-child td:last-child");
            firstContentCell.attr("rowspan", "4");
                                
            $("tbody tr td:last-child", container).not(firstContentCell).hide();
            var displayDiv = $("tr:first-child td:last-child > div", container);
                
            var graphDivs = $("tr td:last-child div.graph", container);
            graphDivs.each( function(i, n) {
                var div = $(n);
                var img = div.find("img");
                if( img.length > 0) {
                    var bg = "url(" + img.attr("src") + ")";
                    div.css("background-image", bg);
                    img.remove();
                }
                var switchFn = function(e) {
                    if( displayDiv == div ) {
                        return;
                    }
                    if( displaySpan ) {
                        displaySpan.css("background-position", "0 0");
                    } else {
                        $(".intro").hide();
                    }
                    displaySpan = $(this).closest("tr").find("td:first-child span");
                    var index = graphDivs.index(div);
                    var bg = "0 -" + (index+1) * 48 + "px";
                    displaySpan.css("background-position", bg);
                    displayDiv.animate({
                        opacity: 0
                    }, 150, function() {
                        displayDiv.hide();
                        div.css("opacity", "0");
                        div.show();
                        div.animate({
                            opacity: 1
                        }, 150);
                        displayDiv = div;
                    });
                }
                div.closest("tr").find("td:first-child").on("click mouseover", $.debounce(switchFn, 100));
            });           
            $("tbody tr td:last-child > div", container).not(displayDiv).insertAfter(displayDiv);
            $("tbody tr td:last-child > div", container).hide();
                
            $("thead td:last-child", container).wrapInner("<div class='graph intro'>");
            $("thead td:last-child div.graph", container).insertAfter(displayDiv);      
            $("thead", container).remove();
            $("div.intro", container).wrapInner("<div class='rule'>");
            
            // Add the default content from the thead
            var ruleImg = $(".intro img", container);
            if( ruleImg.length > 0 ) {
                $("div.rule", container).css("background-image", "url(" + ruleImg.attr("src") + ")");
                ruleImg.remove();
            }
            
        }
    };    
    
    $.fn.classifier = function(method) {        
        if ( methods[method] ) {
            return methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
        } else if ( typeof method === 'object' || ! method ) {
            return methods.init.apply( this, arguments );
        } else {
            $.error( 'Method ' +  method + ' does not exist on jQuery.tooltip' );
        }           
    };
})( jQuery );
