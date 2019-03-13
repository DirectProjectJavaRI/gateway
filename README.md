# gateway
The NHIN Direct Gateway module consists of protocol bridges between the NHIN Direct backbone protocol and security and trust agent. Bridges integrate with the protocol implementation using implementation specific configuration methods. Take, for example, the Apache James SMTP implementation. The gateway module provides a James bridge in the form of a James mailet

Bridges communicate with a common protocol agent that performs protocol specific logic before delegating the message to the security and trust protocol. Protocol agents consume configuration information via Spring configuration and the DirectProject configuration service.

Full documentation can be found [here](https://directprojectjavari.github.io/gateway/).
