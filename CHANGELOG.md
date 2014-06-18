# next
## Breaking Changes
 * renamed HttpExecutionContextMonitorFiler to HttpExecutionContextMonitorFilter
 * renamed SpringHttpExecutionContextMonitorFiler to SpringHttpExecutionContextMonitorFilter

## Features
 * support for monitoring ehcache (stagemonitor-ehcache)
 * overwrite properties with Java system property (-Dstagemonitor.property=value)
 * disable plugins with property 'stagemonitor.plugins.disabled'

## Bug Fixes