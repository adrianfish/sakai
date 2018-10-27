$(document).ready(function () { 

  $("#sortableTable").tablesorter({ 
    // sort on the first column
    sortList: [[0,0]]
  }); 
  $("#reset-order-link").click(function () { 
    // set sorting column and direction, this will sort on the first column index starts at zero
    //Highlight effect to notify user of that action has been performed
    //$("#reset-order-link").parent().parent().effect("highlight", {}, 1000);
    var sorting = [[0,0]]; 
    // sort on the first column 
    $("#sortableTable").trigger("sorton",[sorting]); 
    // return false to stop default link action 
    return false; 
  }); 

  //check cookie to see if user has a preference and show that chart 
  //also set the select list to match
  //otherwise show bar chart by default, hide others
  var preferredChartType = $.cookie("polls-chart-type");
  if (preferredChartType) {
    hideAllCharts();
    $('select#chart-type-selection').val(preferredChartType);
    $('#poll-chart-' + preferredChartType).show();
  } else {
    $('#poll-chart-pie').hide();
    $('#poll-chart-bar').show();
  }

  //when option changes, hide all and show the one they chose.
  //then set pref in the cookie	
  $('select#chart-type-selection').change(function(event){
    var type=$(this).val();
    hideAllCharts();
    $('#poll-chart-' + type).show();
    $.cookie("polls-chart-type", type);
  });

  function hideAllCharts() {
    $('#poll-chart-bar').hide();
    $('#poll-chart-pie').hide();
  }
});
