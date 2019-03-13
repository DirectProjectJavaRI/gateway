# PKCS11 Configuration

Depending on institutional or agency policy, private keys may be required to be protected in such a way that the private key is never exposed in unencrypted format unless loaded onto a PKCS11 token such as a NIST certified hardware security module (HSM). Previous and current versions of configuration service can store the private key in an encrypted PKCS12 container, however, this container is decrypted in the configuration service when needed and the private key is present in unecrypted format in process memory of both the configuration service and the gateway. As of version 2.2 of the configuration service, private keys can be store in a "wrapped" format where they are encrypted by a secret key encryption key and never decrypted in the configuration service. As of version 2.1 of the agent, the agent can "unwrap" the private keys into a PKCS11 token and perform signing and decryption operations without the private key ever being decrypted in process memory. As of version 4.1, the gateway can be configured to load and inject an appropriate PKCS11 token implementation into the agent.

Token configuration generally comes in 2 flavors: tokens that leverage the Sun PKCS11 JCE provider implementation and those that provide their own custom JCE provider. The gateway supports both models with a little different configuration for each. HSMs are configured in a file named *staMailet.properties* in the *%james3InstallRoot%/james-server-jpa-guice.lib/properties* 

| Setting Name | Type | Description |
| --- | --- | --- |
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
| direct.gateway.agent.jceProviderName | String | Indicates the name of the JCE provider used for encryption and signature validation. Default value: BC |
| direct.gateway.agent.jceSensitiveProviderName | String | Indicates the name of the JCE provider used for decryption and digital signature generation.  If you are using an HSM, this should be set the JCE provider name supplied by your HSM vendor.  Default value: BC |