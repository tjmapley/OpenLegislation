angular.module('open.spotcheck')
    .controller('ReportCtrl', ['$scope', ReportCtrl]);

function ReportCtrl($scope){
    $scope.billCategories = ['Status', 'Bill', 'Type', 'Date', 'Issue', 'Source'];
    $scope.exampleData = ['New', 'S23', 'Action', '8/11/2016', '#1234', 'Daybreak'];

    $scope.hooplah = function(){
        $scope.date = moment().format('l');
    };

    function init(){
        $scope.hooplah();
    }

    $scope.checkBoxOptions = {
        initial: 'LBDC - OpenLegislation',
        secondary: 'OpenLegislation - NYSenate.gov'
    };

    init();
}