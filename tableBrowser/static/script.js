function toggleClasses(className) {
    $('table').removeClass('ENTITY');
    $('table').removeClass('RELATION');
    $('table').removeClass('MATRIX');
    $('table').removeClass('OTHER');
    $('table').addClass(className);
}
$(document).ready(function() {
    $('#classificationButtons button:contains(' + $('h2').attr('type') + ')').addClass('btn-primary');
    var originalClass
    $('#classificationButtons button').hover(function() {
        $('table').addClass($(this).text() + '-hover');
        originalClass = $('h2').attr('type');
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

});
