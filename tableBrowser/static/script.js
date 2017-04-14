function toggleClasses(className) {
  $('table').removeClass('ENTITY');
  $('table').removeClass('RELATION');
  $('table').removeClass('MATRIX');
  $('table').removeClass('OTHER');
  $('table').addClass(className);
}
 $(document).ready(function() {
     $('#entBtn').hover(function() {
         toggleClasses('ENTITY');
         originalClass = $('h2').attr('type');
     }, function() {
         $('table').removeClass('ENTITY');
         $('table').addClass(originalClass);
     });
 });
