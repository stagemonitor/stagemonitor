import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

def cli = new CliBuilder(usage: "groovy ExportKibanaDashboard.groovy")
cli.with {
	width = 160
	_(longOpt: 'url', args: 1, 'The elasticsearch url (defaults to http://192.168.99.100:9200)')
	_(longOpt: 'type', args: 1, required: true, argName: 'config|index-pattern|search|dashboard', 'Which type to export')
	_(longOpt: 'id', args: 1, required: true, 'The id of the type to export')
	_(longOpt: 'curl', 'If set, exports as curl script')
}
def options = cli.parse(args)
if (!options) return

def addr = options.url ?: "http://192.168.99.100:9200"

if (options.curl) println "curl -XPOST $addr/_bulk?format=yaml -d '"
exportKibanaType(addr, options.type, options.id)
println ''
if (options.curl) println "'"

def exportKibanaType(String addr, String type, String id) {
	def hit = new JsonSlurper().parse("$addr/.kibana/$type/$id".toURL())
	println new JsonBuilder([index: [_index: hit._index, _type: hit._type, _id: hit._id, _version: 1, _version_type: "external_gte"]]).toString()
	println new JsonBuilder(hit._source).toString()
	if (type == "dashboard") {
		new JsonSlurper().parseText(hit._source.panelsJSON).each {
			exportKibanaType(addr, it.type, it.id)
		}
	}
}

