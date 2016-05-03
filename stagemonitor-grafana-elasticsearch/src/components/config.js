import _ from 'lodash';

export class ExampleAppConfigCtrl {
    constructor(backendSrv) {
        this.backendSrv = backendSrv;
        this.datasourceName = 'ES stagemonitor';
        this.isDatasourceCreated = false;
        this.appEditCtrl.setPreUpdateHook(this.preUpdate.bind(this));

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
        return this.backendSrv.post('/api/datasources', {
            "name": this.datasourceName,
            "type": "elasticsearch",
            "url": this.appModel.jsonData.elasticsearchUrl,
            "access": "proxy",
            "jsonData": {"timeField": "@timestamp", "esVersion": 2, "interval": "Daily"},
            "database": "[stagemonitor-metrics-]YYYY.MM.DD"
        });
    }
}

ExampleAppConfigCtrl.templateUrl = 'components/config.html';
