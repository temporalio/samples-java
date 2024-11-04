## AWS Encryption SDK Sample

This sample demonstrates how a user can leverage the [AWS Encryption](https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/java.html) SDK to build a `PayloadCodec` to encrypt and decrypt payloads using [AWS KMS](https://aws.amazon.com/kms/) and envelope encryption.

### About the AWS Encryption SDK:

>The AWS Encryption SDK is a client-side encryption library designed to make it easy for everyone to encrypt and decrypt data using industry standards and best practices. It enables you to focus on the core functionality of your application, rather than on how to best encrypt and decrypt your data. The AWS Encryption SDK is provided free of charge under the Apache 2.0 license.

For more details please see [Amazons Documentation](https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/introduction.html)

### Choosing a Key Ring 

This sample uses am [AWS KMS keyring](https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/use-kms-keyring.html). This approach is convenient as you don't need to manage or secure your own keys. One drawback of this approach is it will require a call to KMS every time you need encrypt or decrypt data. If this is a concern you may want to consider using an [AWS KMS Hierarchical keyring](https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/use-hierarchical-keyring.html).

Note: You can also use the AWS Encryption SDK without any AWS services using the raw keyrings.

For more details please see [Amazons Documentation on choosing a key ring](https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/which-keyring.html).

### Running this sample

Make sure your AWS account credentials are up-to-date and can access KMS.

Export the following environment variables
-  `AWS_KEY_ARN`: Your AWS key ARN.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.keymanagementencryption.awsencryptionsdk.EncryptedPayloads
```
