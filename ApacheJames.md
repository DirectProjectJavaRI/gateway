# Apache James Deployment

Apache [James](http://james.apache.org/) is an open source Java based mail and news server supported by the Apache software foundation. It is implemented using a modular pattern and can extended by adding new modules and components. It can run either as a completely stand alone email system, or can be integrated into a more complex mail deployment acting as either the final mail destination or a smart relay.

**NOTE:** As version 4.0, James 3 is better supported, but still considered to be a non-production release. It adds support for additional features such as IMAP, contemporary SSL/TLS encryption ciphers, and better multi domain support.  As of version 6.0, only James 3.2.0 is supported.  This document will only cover the Java RI 6.0 and later meaning it will only cover James 3.2.0 which is a stable release.  Documentation for older versions can be found [here](http://api.directproject.info/gateway/4.2/users-guide/depl-james.html).

## NHINDSecurityAndTrustMailet

As stated earlier, James is modular platform that facilitates easy integration of custom processing modules into the SMTP stack. The modular constructs of James are the Mailet and Matcher interfaces. In fact the default James configuration consists mainly of pre-configured Mailets and Matchers. The Direct Project gateway module provides the NHINDSecurityAndTrustMailet as protocol bridge for James. This custom mailet intercepts messages as they pass through the James transport processor, extracts the sender and recipients from the SMTP envelope, and calls the SMTPAgent to process the messages according the security and trust policy. After processing, the bridge either allows the message to continue through the James stack or calls appropriate error handling routines if the message cannot be processed correctly.

##### Mailet installation

All standard and custom modules are deployed as jar files and placed in the James james-server-jpa-guice.lib directory:

*%james3InstallRoot%/james-server-jpa-guice.lib*

**NOTE:** The reference implementation contains a pre-built James configuration in the direct-project-stock assembly that contains all of the necessary jars in the lib folder as well as a pre-configured mailet.

##### Mailet Configuration

The NHINDSecurityAndTrustMailet is added to the James processing stack by adding specific lines into the James conf/mailetcontainer.xml file.  In addition, other template configuration files are found in the *conf* directory.  The ones of most interest are: 

* %james3InstallRoot%/conf/domainlist.xml - Contains the list of all domains managed by the system.
* %james3InstallRoot%/conf/imapserver.xml - Contains the configuration for the IMAP edge protocol.
* %james3InstallRoot%/conf/mailetcontainer.xml - Contains the configuration of the mailets.
* %james3InstallRoot%/conf/pop3server.xml - Contains the configuration of the POP3 edge protocol.
* %james3InstallRoot%/conf/smtpserver.xml - Contains the configuration of the SMTP edge protocol.

To install the security and trust mailet into the James stack, you need to a mailet element under the transport processor in the mailetcontainer.xml file.

Example:

```
   <mailet match="org.nhindirect.gateway.smtp.james.matcher.RecipAndSenderIsNotLocal=mydomain.com" class="org.nhindirect.gateway.smtp.james.mailet.NHINDSecurityAndTrustMailet"/>
```

Previous version of the NHINDSecurityAndTrustMailet pulled configuration attributes from nodes of the mailet entry.  With the introduction of Spring in to the stack in version 6.0 of the Java RI, configuration has been moved to a properties file named *staMailet.properties* in the *%james3InstallRoot%/james-server-jpa-guice.lib/properties* directory.  Most of the properties have default values that are set in an internal properties file, but can be overridden by adding/modifying the properties in the *staMailet.properties* file.

| Property Name | Type | Description |
| --- | --- | --- |
| direct.config.service.url | URL | URL of the direct project configuration service.  Default value: localhost:8080/config-service |
| direct.msgmonitor.service.url | URL | URL of the direct project message monitoring service.  Default value: localhost:8080/msg-monitor |
| direct.webservices.security.basic.user.name | String | Username to authenticate to the direct project services.  Default value: admin |
| direct.webservices.security.basic.user.password | String | Password to authenticate to the direct project services.  Default value: d1r3ct; |
| direct.gateway.keystore.hsmpresent | Boolean | Indicates if a PKCS11 hardware security module (HSM) is used.  Default value: false |
| direct.gateway.keystore.keyStoreType | String | If an HSM is used, indicates the key store type used.  Default value: Luna |
| direct.gateway.keystore.keyStoreSourceAsString | String | If an HSM is used, indicates the key source param as a string.  Default value: slot:0 |
| direct.gateway.keystore.keyStoreProviderName | String | If an HSM is used, indicates the key store provider name.  Default value: com.safenetinc.luna.provider.LunaProvider |
| direct.gateway.keystore.keyStorePin | String | If an HSM is used, indicates the password used to authenticate.  Default value: som3randomp!n |
| direct.gateway.keystore.keyStorePassPhraseAlias | String | If an HSM is used, indicates symmetric key name used to decrypt key store entries.  Default value: keyStorePassPhrase |
| direct.gateway.keystore.privateKeyPassPhraseAlias | String | If an HSM is used, indicates symmetric key name used to decrypt private keys within a key store entry.  Default value: privateKeyPassPhrase |
| direct.gateway.keystore.keyStorePassPhrase | String | If an HSM is NOT used, passphrase used to decrypt and encrypt key store entires.  Default value: H1TBr0s! |
| direct.gateway.keystore.privateKeyPassPhrase | String | If an HSM is NOT used, passphrase used to decrypt and encrypt private keys within a key store entires.  Default value: H1TCh1ckS! |
| direct.gateway.keystore.initOnStart | Boolean | Indicates if the key store manager should self init on startup.  Default value: true |
| direct.gateway.agent.rejectOnTamper | Boolean | Indicates if messages should be rejected if the SMTP envelope information just not match the to and from data in the MimeMessage.  Default value: false |
| direct.gateway.agent.jceProviderName | Boolean | Indicates if messages should be rejected if the SMTP envelope information just not match the to and from data in the MimeMessage.  Default value: false |
| direct.gateway.agent.useOutgoingPolicyForIncomingNotifications | Boolean | Indicates if the anchor policy for outoing messages should be applied to incoming MDN and DSN messages. Default value: true |
| direct.gateway.agent.jceProviderName | String | Indicates the name of the JCE provider used for encryption and signature validation. Default value: BC |
| direct.gateway.agent.jceSensitiveProviderName | String | Indicates the name of the JCE provider used for decryption and digital signature generation.  If you are using an HSM, this should be set the JCE provider name supplied by your HSM vendor.  Default value: BC |

##### Matchers

Every mailet is required to provide a matcher. A matcher is a thin piece of logic that determines if the message should be processed by the mailet. The result of matcher is a list of recipients that should be processed by the mailet. James provides a stock set of out of the box matchers, so may use any of them for the security and trust mailet if they suit your needs.

Depending on your SMTP deployment, you may want to use simple local delivery for outgoing messages when the recipients are in the same domain as your HISP. In this case you will not want the messages to be processed by the security and trust agent unless your HISP configuration allows users to not trust other users inside the same domain (i.e. user1@sample.com does not trust user2@sample.com). The gateway module provides a custom matcher called RecipAndSenderIsNotLocal which takes one parameter: a list of domains managed by your HISP. **NOTE:** Multiple domains are comma delimited.

Example:

```
<mailet match="RecipAndSenderIsNotLocal=securehealthemail.com,cerner.com" class="org.nhindirect.gateway.smtp.james.mailet.NHINDSecurityAndTrustMailet" />
```

When James reaches this mailet, the matcher determines if the message is incoming from an external domain or an outgoing message. If the message is incoming, all recipients are sent to the mailet. If the message is outgoing, all external recipients are processed by the agent while all local recipients are not processed and remain unencrypted for local delivery. **NOTE:** When deploying James in multi domain environment, unless all domains within a HISP mutually trust each other, all messages and recipients should be processed by the agent.

##### Required Web Service Configuration

It is assumed the DirectProject configuration service has been deployed and settings such as domains, anchors, and certificates have been configured.  

## Message Monitoring and Delivery Notification

Version 2.0 of the gateway introduced not only support for the delivery notification implementation [guide](http://wiki.directproject.org/w/images/a/a1/Implementation_Guide_for_Delivery_Notification_in_Direct_v1.0.pdf), but better support for delivery quality of service in general. Increased support has been added in three sections:

* Generation of DSN bounce messages for rejected outgoing message for security and trust reasons
* Generation of DSN bounce messages for messages that do not receive MDNs.
* Generation of DSN bounce messages for messages that cannot be delivered to James mailboxes.

The default behavior of the gateway has been designed and configured to be as passive as possible to older releases.

##### Message Monitoring

Messages that are not trusted by a receiving HISP do not receive any type of positive notification. To provide feedback to the sender, the NHINDSecurityAndTrustMailet can send outbound message information to a monitoring service that keeps track of all out bound messages and received notifications. The monitoring service is responsible for generating DSN bounce messages for messages that have not received notification messages within a given time period. The time period is configured in the monitoring [service](https://directprojectjavari.github.io/direct-msg-monitor/DepAndConfig).

##### Delivery Failure

The gateway overrides the default James LocalDelivery mailet with the TimelyAndReliableLocalDelivery mailet to generate DSN bounce messages if the message cannot be delivered to the local users mailbox. It also adds the DirectBounce mailet to the 'local-address-error' processor to generate a DSN bounce if the local mail account does not exist.

## Incoming Notification Policy

Trust can be configured to be directional meaning trust can be configured to allow either outbound only or inbound only messages to be trusted. If directional trust is set for outbound only, issues can occur with message monitoring because the incoming MDN and DSN notification messages will be marked as not trusted. To allow message monitoring and QoS in general to operate properly, notification (and only notifications messages) must be allowed to flow through the system as trusted messages. However the agent needs to ensure that only notification messages that are generated from trusted outbound destinations are allowed to marked as trusted.

The security and trust mailet supports the *direct.gateway.agent.useOutgoingPolicyForIncomingNotifications* property to allow incoming notification messages to be marked as trusted. 

## SuppressAndTrackAggregate Mailet

There are two scenarios where MDN notification messages should be suppressed from being delivered to the sender's edge client:

* MDN processed messages. These are intended to be only STA to STA messages.
* Duplicate notifications for messages requesting timely and reliable message.

The SuppressAndTrackAggregate mailet is responsible for sending inbound notification messages to the monitoring service and determining if the notification message should be suppressed. The following is an example configuration:

```
     <mailet match="org.nhindirect.gateway.smtp.james.matcher.IsNotification" class="org.nhindirect.gateway.smtp.james.mailet.SuppressAndTrackAggregate">
        <ConsumeMDNProcessed>true</ConsumeMDNProcessed>            
     </mailet>
```

The following mailet elements are used:

| Element | Type | Description |
| --- | --- | --- |
| ConsumeMDNProcessed | Boolean | Indicates if all MDN processed messages should be suppressed. It is recommended that this be set to true. Setting to false will allow all MDN processed message to be sent to the sender's edge client. This setting is mainly here to enable passivity from older versions of the agent (setting to false enables the passive behavior). |

## Prebuilt Assembly Configuration

A full stock assembly and configuration is available that contains the minimum set of components to run the Direct Project in a single domain stand alone James deployment. See the stock project page for installation and configuration details.

## Deployment Configuration Scenarios

The following scenarios are not at all an exhaustive list of deployment configurations, but some identified use cases and suggested best practice deployments.

**Development Testing Configuration**

This use case describes a scenario where the HISP is only used for development testing. See the Bare Metal Project Java source wiki for details.

**Single Domain Standalone**

This use case describes a scenario where the HISP consists of only one domain, and a deployed instance of James comprises the entire SMTP server stack.

![JamesStandAlone](JamesStandAlone.png)

Mailet configuration in this use case is fairly simple.

Example:

```
<mailet match="org.nhindirect.gateway.smtp.james.matcher.RecipAndSenderIsNotLocal=mydomain.com" class="org.nhindirect.gateway.smtp.james.mailet.NHINDSecurityAndTrustMailet" />
```

Because all messages sent from within the domain are delivered locally, the matcher is necessary to ensure local outgoing messages are not processed by the agent.

**Single Domain Standalone With EMAIL Gateway**

This use case describes a scenario where the HSIP consists of only one domain, mail is stored and retrieve using James, but all incoming and outgoing messages going to and coming from the backbone are handled by an email gateway.

![JamesStandAloneWithGateway](assets/JamesStandAloneWithGateway.png)

This model assumes that the gateway is configured to send all incoming messages to James' configured incoming port (default 25). The mailet configuration is still fairly simple.

Example:

```
<mailet match="org.nhindirect.gateway.smtp.james.matcher.RecipAndSenderIsNotLocal=mydomain.com" class="org.nhindirect.gateway.smtp.james.mailet.NHINDSecurityAndTrustMailet" />
```

As with the previous configuration, all local mail is delivered locally. However all outgoing mail needs to be forwarded to the email gateway. The RemoteDelivery mailet handles forwarding out bound messages to a configured location. In this scenario we want local messages to stay local and all externally bound messages to be forwarded to the gateway. The mailet can be configured immediately following the security and trust mailet if no other processing is necessary in the transport processor.

Example:

```
 <mailet match="RecipientIsRegex=(?!mydomain.com)"  class="RemoteDelivery">
    <gateway>mygatewayhostname:25</gateway>
 </mailet>
```

Note the use of negative look arounds in the regex. James does not provide a matcher for remote recipients, but a negative look around regular expression can be used to mimic the desired affect. The RecipAndSenderIsNotLocal should not be used because it would send all incoming messages to the gateway.

**Single Domain James as Processing Relay**

This use case describes a scenario where the HISP has an existing stand alone email solution such as Postfix. The email server may not have the ability to inject custom processing modules into the server, but can relay messages to other servers for custom processing. After the custom email server finishes processing the message, the custom server must send the message back to the original email server (typically on port separate from the main incoming email port). In this scenario, James is the custom email server. The standalone email server may or may not be configured to handle local delivery before the custom processor is executed, but an identical mailet configuration can be used for both cases.

![JamesStandAloneProcessingRelay](assets/JamesStandAloneProcessingRelay.png)

Example:

```
<mailet match="org.nhindirect.gateway.smtp.james.matcher.RecipAndSenderIsNotLocal=mydomain.com" class="org.nhindirect.gateway.smtp.james.mailet.NHINDSecurityAndTrustMailet" />
```

The mailet will still want to ensure that local outbound messages remain unprocessed because the standalone email server will still deliver these messages locally. Howerver, the relay matcher will need to match all messages so all messages are sent back to the standalone email server.

```
 <mailet match="All"  class="RemoteDelivery">
    <gateway>mystandaloneemailserver:10026</gateway>
 </mailet>
```

Note the custom port on the remote delivery email server. This is because the email server will more than likely accept processed messages on a separte port than incoming messages.

If the standalone email server and James are deployed on the same OS instance, you will probably need to change James' incoming port.

**Single Domain James as Injected Into Relay/Gateway Flow**

This use case describes a scenario where the HISP already uses a relay or email gateway for incoming and outgoing messages. James is injected between the email server and gateway. The email server is configured to send all outgoing messages to James incoming port, and James RemoteDelivery mailet is configured to send all messages to the relay/gateway.

![JamesStandAloneRelayFlow](assets/JamesStandAloneRelayFlow.png)

Example:

```
   <mailet match="org.nhindirect.gateway.smtp.james.matcher.RecipAndSenderIsNotLocal=mydomain.com" class="org.nhindirect.gateway.smtp.james.mailet.NHINDSecurityAndTrustMailet"/>

   <mailet match="All"  class="RemoteDelivery">
      <gateway>mygateway:25</gateway>
   </mailet>
```

As with the previous use case, James' incoming SMTP port may need to be changed if the email server and James are deployed on the same OS instance.

What about incoming messages from the email gateway that need to go the server? How do you configue the RemoteDelivery mailet to deliver outgoing messages to the gateway and incoming messages to the email server. The answer is you can't. That's not entirely true. The problem is that there is not matcher that can determine if the messages are outbound or inbound. If they did exist (and someone is entirely free to write them), the configuration could look like this.

```
   <mailet match="org.nhindirect.gateway.smtp.james.matcher.RecipAndSenderIsNotLocal=mydomain.com" class="org.nhindirect.gateway.smtp.james.mailet.NHINDSecurityAndTrustMailet"/>


   <mailet match="MesssageIsOutbound"  class="RemoteDelivery">
      <gateway>mygateway:25</gateway>
   </mailet>
   
   <mailet match="MesssageIsInbound"  class="RemoteDelivery">
      <gateway>mygateway:25</gateway>
   </mailet>
```

Something missing in this configuration is that the RemoteDelivery mailet requires a different mail repository URL for each mailet instance.

Another option is to deploy to sepearte James instances. One to handle incoming messages and the other to handle outgoing messages. The outgoing instance would be configured similar to the first example in this use case. The incoming instance would be configured similar to the following:

Example:

```
   <mailet match="All" class="org.nhindirect.gateway.smtp.james.mailet.NHINDSecurityAndTrustMailet"/>

   <mailet match="All"  class="RemoteDelivery">
      <gateway>myemailserver:25</gateway>
   </mailet>
```

The gateway would forward all incoming email to the inbound instance's SMTP port (needs to be different than the outgoing instance's port) and James forwards all messages on to the email server.

**Multi Domain James as Injected Into Relay/Gateway Flow**

This use case describes a scenario where the HISP already uses a relay or email gateway for incoming and outgoing messages and supports multiple domains on the its email server. James is injected between the email server and gateway. The email server is configured to send all outgoing messages to James incoming port, and James RemoteDelivery mailet is configured to send all messages to the relay/gateway.

In this scenario, it is suggested that the email server not use local delivery for messages coming from email clients. The reason is that each domain may have a sepearate trust policy and local delivered messages would never be sent to the agent for trust policy processing. If all domains in the HISP mutually trust each other, then local is an acceptable configuration option.

The James and mailet configurations are exactly the same as the previous scenario with one exception. The outgoing James instance should list all domains managed by the HISP in its matcher.

Example:

```
   <mailet match="org.nhindirect.gateway.smtp.james.matcher.RecipAndSenderIsNotLocal=mydomain.com,myotherdomain.com" class="org.nhindirect.gateway.smtp.james.mailet.NHINDSecurityAndTrustMailet"/>

   <mailet match="All"  class="RemoteDelivery">
      <gateway>mygateway:25</gateway>
   </mailet>
```