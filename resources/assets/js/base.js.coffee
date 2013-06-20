
confirmationModal = ($el, fun) ->
  modalHeader = $el.data('modal-header');
  modalBody = $el.data('modal-body');
  modalConfirm = $el.data('modal-confirm');
  modalCancel = $el.data('modal-cancel');
  modalHtml = "<div class=\"modal hide fade\">
   <div class=\"modal-header\">
     <button type=\"button\" class=\"close\" data-dismiss=\"modal\" aria-hidden=\"true\">&times;</button>
     <h3>#{ modalHeader }</h3>
   </div>
   <div class=\"modal-body\">
     <p>#{ modalBody }</p>
   </div>
   <div class=\"modal-footer\">
     <a href=\"#\" class=\"btn\" data-dismiss=\"modal\">#{ modalCancel }</a>
     <a href=\"#\" class=\"btn btn-primary\" data-dismiss=\"modal\">#{ modalConfirm }</a>
   </div>
 </div>";
  $modal = $(modalHtml);
  $modal.on 'click', '.btn-primary', fun
  $modal.modal 'show'

handleResponse = ($el, response, status, xhr, requestUrl) ->
  console.log $el, response, status, xhr
  type = xhr.getResponseHeader "Content-Type"
  if type? and type.indexOf("json") != -1
    for entry in response
      switch entry.method
        when "log"
          console.log entry.content
        when "reload"
          window.location.reload false
        when "redirect"
          window.location.href = entry.url
  reload = if $el.hasClass('no-reload') then false else response.length==0;
  if reload then window.location.reload false

$(document).ready ->
  # Ajaxify
  $('body').on "click", ".ajaxify", (e) ->
    e.preventDefault();
    url = $(this).data "url";
    formSelector = $(this).data "form"
    data = if formSelector? then $(formSelector).serialize() else {}
    $element = $(this);
    process = () ->
      $.post(url, data, (data, status, xhr) ->
        handleResponse($element, data, status, xhr, url)).fail (xhr, status, error) ->
        handleResponse($element, xhr.responseText, status, xhr, url);
    if $element.data('modal-header')?
      confirmationModal($element, process);
    else
      process();

$(document).ready ->
  # Sorting
  $('body').on "click", ".sorting", (e) ->
    e.preventDefault();
    column = $(e.target);
    sorting = column.data('sorting')
    if sorting && document.URL.indexOf("?") != -1
      window.location = window.location + "&sorting=" + sorting;
    else if sorting
      window.location = window.location + "?sorting=" + sorting;
