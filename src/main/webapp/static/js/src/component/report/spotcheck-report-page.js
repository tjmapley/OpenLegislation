angular.module('open.spotcheck')
    .controller('ReportCtrl', ['$scope', ReportCtrl]);

function ReportCtrl($scope){
    $scope.billCategories = ['Status', 'Bill', 'Type', 'Date', 'Issue', 'Source'];
    $scope.calendarCategories = ['Status', 'Date', 'Error', 'Type', 'Nbr', 'Date/Time', 'Issue', 'Source'];
    $scope.agendaCategories = ['Status', 'Date', 'Error', 'Nbr', 'Committee', 'Date/Time', 'Issue', 'Source'];

    $scope.exampleData = ['New', 'S23', 'Action', '8/11/2016', '#1234', 'Daybreak'];

    /*
     To make this controller work:

     Add ?reportType=daybreak (or another report type)
     to the end of the URL
     and choose an option from the drop down
     */

    $scope.init = function(){
        $scope.date = moment().format('l');
    };

    $scope.checkBoxOptions = {
        initial: 'LBDC - OpenLegislation',
        secondary: 'OpenLegislation - NYSenate.gov'
    };

    $scope.getSummaries = function(){
        $scope.mismatches = $scope.mismatchRows;
        $scope.mismatches.forEach(function(mismatch){
            console.log(mismatch);
        });
    }
}