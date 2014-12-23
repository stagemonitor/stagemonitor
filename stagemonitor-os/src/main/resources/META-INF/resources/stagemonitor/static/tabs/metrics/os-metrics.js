(function () {
	plugins.push(
		{
			id: "os-metrics",
			label: "OS",
			graphs: [
				{
					bindto: '#os-cpu',
					min: 0,
					max: 1,
					format: 'percent',
					stack: true,
					fill: 0.1,
					columns: [
						["gauges", /os\.cpu\.usage\.((?!idle).*$)/, "value"],
						["gauges", /os\.cpu\.usage\.(idle)/, "value"]
					]
				},
				{
					bindto: '#network-io',
					min: 0,
					format: 'bytes',
					derivative: true,
					columns: [
						["gauges", /os.net.[^\.]+.(write)/, "value"],
						["gauges", /os.net.[^\.]+.(read)/, "value"]
					],
					disabledLines: ["Code-Cache"]
				},
				{
					bindto: '#io',
					min: 0,
					fill: 0.1,
					format: 'bytes',
					derivative: true,
					columns: [
						["gauges", /os.fs.[^\.]+.(writes).bytes/, "value"],
						["gauges", /os.fs.[^\.]+.(reads).bytes/, "value"]
					]
				},
				{
					bindto: '#fs-usage',
					min: 0,
					max: 1,
					format: 'percent',
					columns: [
						["gauges", /os.fs.([^\.]+).usage-percent/, "value"]
					]
				},
				{
					bindto: '#ram',
					min: 0,
					format: 'bytes',
					fill: 0.1,
					columns: [
						["gauges", "os.mem.usage.(total)", "value"],
						["gauges", "os.mem.usage.(used)", "value"]
					]
				},
				{
					bindto: '#swap',
					min: 0,
					format: 'bytes',
					fill: 0.1,
					columns: [
						["gauges", "os.swap.usage.(total)", "value"],
						["gauges", "os.swap.usage.(used)", "value"]
					]
				}
			]
		}
	)
}());

