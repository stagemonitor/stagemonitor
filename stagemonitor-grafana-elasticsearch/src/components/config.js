import _ from 'lodash';

export class ExampleAppConfigCtrl {
    constructor(backendSrv) {
        this.backendSrv = backendSrv;
        this.datasourceName = 'ES stagemonitor';
        this.isDatasourceCreated = false;
        this.appEditCtrl.setPreUpdateHook(this.preUpdate.bind(this));

        this.appModel.jsonData = this.appModel.jsonData || {};
        this.appModel.jsonData.elasticsearchUrl = this.appModel.jsonData.elasticsearchUrl || "";
        this.appModel.jsonData.reportingInterval = this.appModel.jsonData.reportingInterval || ">60s";
        this.validation = {
            reportingIntervalValid: true,
            elasticsearchUrlValid: true
        };


        backendSrv.get('/public/plugins/stagemonitor-grafana-elasticsearch/plugin.json').then(data => {
            this.dashboards = _.filter(data.includes, {type: 'dashboard'});
        });
    }

    preUpdate() {
        if (this.isDatasourceCreated) {
            return Promise.resolve();
        } else {
            return this.createDatasource();
        }
    }

    init() {
        this.backendSrv.get('/api/datasources')
            .then(datasources => {
                this.isDatasourceCreated = _.findIndex(datasources, {name: this.datasourceName}) !== -1
            });
    }

    createDatasource() {

        this.validation.elasticsearchUrlValid = !this.isEmptyOrUndefined(this.appModel.jsonData.elasticsearchUrl);
        this.validation.reportingIntervalValid = !this.isEmptyOrUndefined(this.appModel.jsonData.reportingInterval);

        if (_.values(this.validation).some(value => !value))  {
            return Promise.reject();
        } else {
            return this.backendSrv.post('/api/datasources', {
                "name": this.datasourceName,
                "type": "elasticsearch",
                "url": this.appModel.jsonData.elasticsearchUrl,
                "access": "proxy",
                "jsonData": {
                    "timeField": "@timestamp",
                    "esVersion": 2, "interval": "Daily",
                    "timeInterval": this.appModel.jsonData.reportingInterval
                },
                "database": "[stagemonitor-metrics-]YYYY.MM.DD"
            });
        }
    }

    isEmptyOrUndefined(value) {
        return value === undefined || value == null || value.trim() === "";
    }
}

ExampleAppConfigCtrl.templateUrl = 'components/config.html';
