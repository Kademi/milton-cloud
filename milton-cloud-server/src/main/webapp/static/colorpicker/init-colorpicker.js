pageInitFunctions.push(function() {
    var inputs = $('input.color');
    log("initColorPicker", inputs);
    inputs.wrap("<div class='colorContainer'>");
    var colorDemo = "<div class='colorDemo'></div>";
    inputs.after(colorDemo);
    inputs.each(function(i ,n) {
        $n = $(n);
        log("set initial color: ", $n);
        $n.parent().find(".colorDemo").css("background-color", $n.val());
    })
    var colorContainers = $(".colorContainer");
    colorContainers.ColorPicker({
        onSubmit: function(hsb, hex, rgb, el) {
            var $el = $(el);
            log("onSubmit", hex, el);
            $el.find("input").val("#" + hex);
            $el.find(".colorDemo").css("background-color", "#" + hex);
            $el.ColorPickerHide();
        },
        onBeforeShow: function () {
            $(this).ColorPickerSetColor($(this).find("input").val());
        }
    });
    inputs.bind('keyup', function(){
        $(this).ColorPickerSetColor(this.value);
    });    
    log("done init colors");    
});

