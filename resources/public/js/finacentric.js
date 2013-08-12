$(document).ready( function() {
    $(".async-block").each(function () {
        var $this = $(this);
        $.ajax($this.data("url"), {
            error: function () {
                $this.html($this.find(".on-error").removeClass("hide").html());
            }, 
            success: function (data) {
                $this.html(data);
            }, 
        });
    });
    $(".async-block").on("click", ".async-link", function(e) {
        var $this = $(this);
        var $parent = $this.closest('.async-block');
        $.ajax($this.data("url"), {
            success: function (data) {
                $parent.html(data);
            }, 
            error: function () {
                alert("Nastąpił błąd!");
            }
        });
        e.preventDefault();
    });

    $(".async-block").on("submit", ".async-form", function(e) {
        var $this = $(this);
        var $parent = $this.closest('.async-block');
        $.ajax($this.prop("action"), {
            method: $this.prop("method"),
            data: $this.serialize(),
            success: function (data) {
                $parent.html(data);
            }, 
            error: function () {
                alert("Nastąpił błąd!");
            } 
        });
        e.preventDefault();
    });
});
