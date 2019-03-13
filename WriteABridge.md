# Writing A Protocol Bridge

Protocol bridges are specific to two variables:

* **Backbone Protocol Example:** SMTP
* **Protocol Implementation Example:** Apache James

Protocols bridges intercept messages as they flow through the protocol implementation's stack and hand the messages off to lower layers of the architecture for security and trust processing. Each protocol implementation will more than likely provide its own architecture, API set, and configuration for adding custom processing logic into its stack. For example: the Apache James SMTP server provides the Mailet API to write custom mail processing logic and deploys the Mailet using an XML based configuration file.

Generally messages intercepted by a protocol bridge are in a format or structure specific to the protocol implementation. For example the Apache James hands off a message to a mailet using the Mail interface. In most cases, the bridge will need to convert the message into a common structure supported by the protocol agent. For example, the SMTPAgent requires that all messages are passed to it using a MimeMessage structure. After the message is processed by the protocol and subsequently the security and trust agent, the bridge must inject the processed message back into the protocol implementation's stack according the implemntation's specification.

The Direct Project gateway module provides protocol agent for the following backbone protocols:

* SMTP

## Writing an SMTP Protocol Bridge

SMTP bridges interact with the system through the SMTPAgent interface. Although concrete implementations of the SMTPAgent interface can be instantiated directly, the agent architecture implements a dependency injection (DI) paradigm.  To facilitate the instantiation of an agent, the gateway module provides the SmtpAgentFactory to configure and create instances of the SMTPAgent. The factory has a single static methods for creating instances:

```
public static SmtpAgentFactory getInstance(CertificateService certService, TrustBundleService bundleService, DomainService domainService,
			AnchorService anchorService, SettingService settingService, CertPolicyService polService, Auditor auditor, KeyStoreProtectionManager keyStoreMgr)

```

The dependent services such as certificate and trust bundle services are injected into the factory which in turn injects them into the agent implementation.  How these dependencies are created are outside of the scope of the factory and is dependent on how your bridge is deployed and configured.  For example the NHINDirect mailet used with Apache James is configured from a Spring application context and an accompanying properties file.

##### Getting an SMTPAgent Instance

To get an instance of the SMTPAgent, simply call SmtpAgentFactory.getInstance.

Pseudo Example:

```
  // probably declared as a class instance variable.. depends on the protocol implementation API
  SmtpAgent agent = null;
  
  // create the services... this could be done in a number of ways including depdency injection
  // to create in a function for this example and put them in a structure
  AgentServices svcs = createAgentServices();
  
  // get an instance of the agent
  agent = SmtpAgentFactory.getInstance(svcs.getCertService(), svcs.getBundleService(), svcs.getDomainService(), svcs.getAnchorService(),
          svcs.getSettingService(), svcs.getPolServices(), svcs.getAuditor(), svcs.getKeyStoreMgr());
```

##### Processing Messages

When your bridge received messages from the protocol implementation, it calls the processMessage() method on the SmtpAgent instance. The method takes 3 parameters: the actual message, a collection of recipients, and the sender. processMessage requires that message be passed as a MimeMessage. If your implementation does not provide the message as a MimeMessage, you can use the static utility method org.nhindirect.stagent.parser.EntitySerializer.deserialize() if the message is provided as a raw string or an input stream.

For the recipient list and sender, you should use the SMTP envelope's MAIL FROM and RCTP TO headers. If these headers are not provided or not available through the protocol implementation API, then you should fall back to the routing headers in the message. NOTE You should only use the messages routing headers as a last resort.

Pseudo Example:

```
    public void handleSMTPMessage(String rawMessage)
    {
       String theRawMessage = getRawMessageFromHandler();
       NHINDAddressCollection recips = getRecipsFromMessage(theRawMessage);
       NHINDAddress sender = getSenderFromMessage( theRawMessage);
       
       MimeMessage msg = EntitySerialize.deserialize(theRawMessage);
       
       MessageProcessResult result = agent.processMessage(msg, recips, sender);
       
       if (result.getProcessedMessage() != null)
       {
      		moveProcessedMessageBackIntoSMTPStack(result.getProcessedMessage());
       }
    }  
```

##### MDN Messages

By default the SMTP agent automatically produces a collection of MDN messages with a Disposition of Processed for processed messages. The purpose is to indicate to the sender that the security and trust sub system of the Direct Project network received and successfully processed the sender's message. MDN is described in [RFC3798](http://tools.ietf.org/html/rfc3798).

The SMTP agent returns generate MDN messages in the MessageProcessResult object's notificationMessages attribute. Because the SMTP agent is unaware of the SMTP protocol implementation, the protocol bridge is responsible for sending the messages using the appropriate process as specified by the protocol implementation. MDN messages produced by the SMTP bridge are not encrypted and signed per the security and trust agent specification and should placed on the protocol implementation's outgoing queue or whatever process will result in the MDN messages being encrypted and signed before being sent.

Pseduo James Example:

```
    public void service(Mail mail) throws MessagingException 
    { 
       MimeMessage msg = mail.getMessage();
       // get recipients and sender
       .
       .
       .
       .
       MessageProcessResult result = agent.processMessage(msg, recipients, sender);
       //  check result and processed message for errors and move the processed message
       //  through the James stack
       .
       .
       .
       .       

       // send the MDN messages
       Collection<NotificationMessage> notifications = result.getNotificationMessages();
       if (notifications != null && notifications.size() > 0)
       {
           // create a message for each notification and put it on James "stack"
           for (NotificationMessage message : notifications)
           {
              try
              {
                  this.getMailetContext().sendMail(message);
              }
              catch (Throwable t)
              {
                  // don't kill the process if this fails
                  // but handle the exception
              }
           }
        }
    }  
```

##### Dependencies
The following jars are direct dependencies for writing protocol bridges.

* agent-<version>.jar
* gateway-<version>.jar

Transitive dependencies of these libraries may be required for development, testing, and deployment. These libraries are available in the public mavne repository under the group [org.nhind](http://repo1.maven.org/maven2/org/nhind/).