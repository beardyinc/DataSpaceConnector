# About The Project

> The Eclipse DataspaceConnector project is intended as lightweight component to enable sovereign data sharing and policy
> handling.

## Built with

One of the guiding principles in developing the connector is simplicity and keeping a small footprint with as little
external dependencies as possible. We do not want to force any third-party dependencies onto our users, so we aim to
avoid big frameworks like [Apache Commons](https://commons.apache.org/), [Google Guava](https://github.com/google/guava)
or the [Spring framework](https://spring.io/). The connector is a plain Java application built with Gradle.

# Getting Started

## Checkout and build code

The project requires JDK 11+. To get started:

``` shell 
git clone git@github.com:eclipse-dataspaceconnector/DataSpaceConnector.git

cd DataSpaceConnector ```

./gradlew clean build
```

That will build the connector and run tests.

## Run your first connector

In order to get up and running

# Directory structure

The runtime can be configured with custom modules be enabling various build profiles.

By default, no vault is configured. To build with the file system vault, enable the security profile:

```./gradlew -Dsecurity.type=fs clean shadowJar ```

The runtime can then be started from the root clone directory using:

``` java -Dedc.vault=secrets/edc-vault.properties -Dedc.keystore=secrets/edc-test-keystore.jks -Dedc.keystore.password=test123 -jar runtime/build/libs/edc-runtime.jar ```

Note the secrets directory referenced above is configured to be ignored. A test key store and vault must be added (or
the launch command modified to point to different locations). Also, set the keystore password accordingly.

## Contributing

This project welcomes contributions and suggestions. Most contributions require you to agree to a Contributor License
Agreement (CLA) declaring that you have the right to, and actually do, grant us the rights to use your contribution. For
details, visit https://cla.opensource.microsoft.com.

When you submit a pull request, a CLA bot will automatically determine whether you need to provide a CLA and decorate
the PR appropriately (e.g., status check, comment). Simply follow the instructions provided by the bot. You will only
need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Trademarks

This project may contain trademarks or logos for projects, products, or services. Authorized use of Microsoft trademarks
or logos is subject to and must follow
[Microsoft's Trademark & Brand Guidelines](https://www.microsoft.com/en-us/legal/intellectualproperty/trademarks/usage/general)
. Use of Microsoft trademarks or logos in modified versions of this project must not cause confusion or imply Microsoft
sponsorship. Any use of third-party trademarks or logos are subject to those third-party's policies.
