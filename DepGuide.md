# Deployment Guide

This section describes the different configuration and deployment methodologies for the gateway components.

## Agent Configuration

Agent configuration consists of setting the runtime parameters for both the protocol and security and trust agents. The agent configuration is for the most part decoupled from the protocol implementation and bridge, and the configuration settings can be stored in any addressable protocol supported by the protocol agent subsystem.  Out of the box, the reference implementation supports configuration that is stored in the DirectProject configuration service.

* [SMTP WebService configuration](SMTPWebConfiguration)
* [Fine Grain Tuning](Tuning)

## Protocol Implementation Deployment

Protocol implementation deployments are dependent are the specific implementation. A specific implementation deployment may even have several deployment options. In almost all cases, the deployment consists of installing and configuring a protocol bridge specific to the implementation.

* [SMTP Deployments](SMTPDeployments)

## PKCS11 Configuration

As of version 4.1, the gateway supports enhanced private key protection by configuring a PKCS11 token that will load wrapped private keys into the token for signing and decryption operations.

* [PKCS11 Configuration](PKCS11Configuration)

