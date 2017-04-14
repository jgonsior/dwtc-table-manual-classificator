function toggleClasses(className) {
    $('table').removeClass('ENTITY');
    $('table').removeClass('RELATION');
    $('table').removeClass('MATRIX');
    $('table').removeClass('OTHER');
    $('table').addClass(className);
}
$(document).ready(function() {
    $('#classificationButtons button:contains(' + $('h2').attr('type') +')').addClass('btn-primary');
    var originalClass
    $('#classificationButtons button').hover(function() {
        $('table').addClass($(this).text() + '-hover');
        originalClass = $('h2').attr('type');
    }, function() {
        $('table').removeClass($(this).text() + '-hover');
        $('table').addClass(originalClass);
    });
});
