$(document).ready ->
  $('body').on("click", ".ajaxify",
    (e) -> $.post(
      $(this).data('url'),
      (data) -> window.location.href = unescape(window.location.pathname)));