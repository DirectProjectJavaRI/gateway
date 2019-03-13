# DNS Failure Generation Config

Fine grained tuning of the information found in DSN 'bounce' message can be achieve via the [OptionsManager](http://api.directproject.info/direct-common/6.0/apidocs/org/nhindirect/common/options/OptionsManager.html).

## Failed Delivery DSN Bounce Message Generation

The following parameters can be used to fine to the specific sections of the bounce messages created by the bounce message generator when message delivery fails.

| JVM Param/Properties Setting | Description |
| --- | --- |
| org.nhindirect.gateway.smtp.dsn.impl.DSNFailedPrefix | The prefix to add to the DSN subject |
| org.nhindirect.gateway.smtp.dsn.impl.DNSPostmaster | The postmaster account name used as the from attribute for DSN messages. The postmaster name will be pre-appended to the domain name of the original sender. Default value is 'postmaster'. |
| org.nhindirect.gateway.smtp.dsn.imp.DeliveryFailureDSNMTAName | The name of the agent creating the DSN message |
| org.nhindirect.gateway.smtp.dsn.impl.DeliveryFailureDSNFaileRecipTitle | Title that goes above the list of failed recipients in the human readable section of the DSN message. |
| org.nhindirect.gateway.smtp.dsn.impl.DeliveryFailureDSNFaileRecipTitle | Title that goes at the top of the human readable section of the DSN message. |
| org.nhindirect.gateway.smtp.dsn.impl.DeliveryFailureDSNFailedErrorMessage | A human readable description of why the message failed to be delivered. |
| org.nhindirect.gateway.smtp.dsn.impl.DeliveryFailureDSNFailedHeader | A message header that appears at the top of the human readable section of the DSN message. This generally used as the message introduction. |
| org.nhindirect.gateway.smtp.dsn.impl.DeliveryFailureDSNFailedFooter	| A footer at the bottom of the human readable section of the DSN message. This is generally used to provide troubleshooting information. |