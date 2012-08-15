pageInitFunctions.push(function() {
    var inputs = $('input.color');
    log("initColorPicker", inputs);
    inputs.miniColors();
    log("done init colors");    
});

