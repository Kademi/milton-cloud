

function initBootstrapStyleModals() {
    $(document).on("click", "a[data-toggle=modal]", function(e) {
        e.preventDefault();
        e.stopPropagation();
        var a = $(e.target);
        var contSel = a.attr("data-target");
        var cont = $(contSel);
        cont.load(a.attr("href"), function() {
            showModal(cont);
        });
    });

}
