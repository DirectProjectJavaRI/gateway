# Fined Grained Tuning

Fine grained tuning of the following components can be achieved using the [OptionsManager](http://api.directproject.info/direct-common/6.0/apidocs/org/nhindirect/common/options/OptionsManager.html). For the Apache James server, these are most easily set by adding JVM parameters to the startup script.

## WS Certificate Resolver

The following settings fine tune the web services based certificate resolver:

| JVM Param/Properties Setting | Description |
| --- | --- |
| org.nhindirect.stagent.cert.wsresolver.MaxCacheSize | Maximum number of certificates that can be cached in the cache manager. Defaults to 1000 certificates. |
| org.nhindirect.stagent.cert.wsresolver.CacheTTL | Maximum amount of time in seconds a certificate can reside in the cache manager. Defaults to 1 hours (3600 seconds). |