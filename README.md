# Overview

The NHIN Direct Gateway module consists of protocol bridges between the NHIN Direct backbone protocol and security and trust agent. Bridges integrate with the protocol implementation using implementation specific configuration methods. Take, for example, the Apache James SMTP implementation. The gateway module provides a James bridge in the form of a James mailet 

Bridges communicate with a common protocol agent that performs protocol specific logic before delegating the message to the security and trust protocol. Protocol agents consume configuration information via Spring configuration and the DirectProject configuration service.


## Guides

This document describes how to develop against components of the security and trust agent module.

* [Development Guide](DevGuide) - This section describes how to develop a protocol bridge specific to a protocol and protocol implementation.
* [Deployment Guide](DepGuide) - This section describes how deploy and configure a protocol bridges provided by the gateway module.