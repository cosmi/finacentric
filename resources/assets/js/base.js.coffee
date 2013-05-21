
handleResponse = ($el, response, status, xhr) ->
  console.log $el, response, status, xhr
  type = xhr.getResponseHeader "Content-Type"
  if type? and type.indexOf("json") != -1
    for entry in response
      switch entry.method
        when "log"
          console.log entry.content
        when "reload"
          window.location.reload false
  reload = if $el.hasClass('no-reload') then false else true;
  if reload then window.location.reload false  

$(document).ready ->
  $('body').on "click", ".ajaxify", (e) ->
    e.preventDefault();
    url = $(this).data "url";
    formSelector = $(this).data "form"
    data = if formSelector? then $(formSelector).serialize() else {}
    $element = $(this);
    $.post(url, data, (data, status, xhr) ->
      handleResponse($element, data, status, xhr)).fail (xhr, status, error) ->
      handleResponse($element, xhr.responseText, status, xhr);
