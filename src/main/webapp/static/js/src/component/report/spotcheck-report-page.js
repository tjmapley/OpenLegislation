angular.module('open.spotcheck')
    .controller('ReportCtrl', ['$scope', ReportCtrl])
    .controller('SpotcheckOpenSummaryCtrl',
        ['$scope', '$filter', 'SpotcheckOpenMismatchSummaryAPI', showAPIData]);

function ReportCtrl($scope){
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

function showAPIData($scope, $filter, openSummaryApi){
    $scope.summaries = {};

    $scope.init = function(){
        $scope.setHeaderText('Summary of Open Mismatches');
        getSummaries();
    };

    function getSummaries(){
        $scope.loadingSummaries = true;
        openSummaryApi.get({}, function(response){
            $scope.summaries = response.result.summaryMap;
            $scope.loadingSummaries = false;
        }, function(errorResponse){
            $scope.loadingSummaries = false;
            $scope.summaryRequestError = true;
        })
    }


}