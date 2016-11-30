<section class="content-section" ng-controller="ReportCtrl">
    <md-content>
        <p>Report Date: {{date}}</p>
        <select ng-model="textControls.whitespace" ng-change="showAPIData()"
                ng-options="value as label for (value, label) in checkBoxOptions"></select>
    </md-content>
    <md-tabs>
        <md-tab label="Bills"></md-tab>
        <md-tab label="Calendars"></md-tab>
        <md-tab label="Agendas"></md-tab>
    </md-tabs>
</section>
