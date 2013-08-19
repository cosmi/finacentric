if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function (str){
    return this.slice(0, str.length) == str;
  };
}

function currentPath() {
    return window.location.pathname;
}

$(document).ready( function() {
    $(".async-block").each(function () {
        var $this = $(this);
        if($this.data("url")) {
            $.ajax($this.data("url"), {
                error: function () {
                    $this.html($this.find(".on-error").removeClass("hide").html());
                }, 
                success: function (data) {
                    $this.html(data);
                }, 
            });
        }
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

    $(".url-tab").each(function() {
        var $this = $(this);
        var path = $this.data("path");
        if(currentPath().startsWith(path)) {
            $this.click();
        }
    });



    $("body").on("click", ".ajaxify, .ajaxify-get, .ajaxify-put, .ajaxify-delete", function(e) {
        var $this = $(this);
        var url = $this.attr("href") || $this.data("url");
        var onSuccess = $this.hasClass("ajaxify-ignore")?"ignore":$this.hasClass("ajaxify-show")?"show":"reload";
        var method = 
            $this.hasClass("ajaxify-get")?"get":
            $this.hasClass("ajaxify-put")?"put":
            $this.hasClass("ajaxify-delete")?"delete":"post";
        var confirmQuestion = $this.data("confirm");

        if(!confirmQuestion || confirm(confirmQuestion)) {
            $.ajax(url, {method:method, 
                         success: function(data) {
                             switch(onSuccess) {
                             case "ignore" : return;
                             case "show" : document.body.innerHtml(data); return;
                             case "reload" : window.location.reload();
                             }
                         },
                         error: function(xhr, status, msg) {
                             alert(msg);
                         }});
        }


        e.preventDefault();
    })


});
