function toggleClasses(className) {
  $('table').removeClass('ENTITY');
  $('table').removeClass('RELATION');
  $('table').removeClass('MATRIX');
  $('table').removeClass('OTHER');
  $('table').addClass(className);
}
$(document).ready(function() {
  var originalClass = $('h2').attr('type');
  var id = window.location.href.substr(window.location.href.lastIndexOf('/') + 1);
  $('#classificationButtons button:contains(' + $('h2').attr('type') + ')').addClass('btn-primary');
  $('#classificationButtons button').hover(function() {
    $('table').addClass($(this).text() + '-hover');
  }, function() {
    $('table').removeClass($(this).text() + '-hover');
    $('table').addClass(originalClass);
  });

  $(document).keydown(function(e) {
    if (e.keyCode === 37) { // left arrow
      e.preventDefault();
      window.location = $('#prev').attr('href');
    } else if (e.keyCode === 39) { // right arrow
      e.preventDefault();
      window.location = $('#next').attr('href');
    }
  });

  $('#classificationButtons button').click(function() {
    var newClass = $(this).text();
    var jqxhr = $.ajax("/changeClass/" + id + '/' + newClass)
      .success(function() {
        console.log("Marked " + id + " as " + newClass);
        window.location = $('#next').attr('href');
      }).fail(function() {
        alert("error");
      })
  });

});
