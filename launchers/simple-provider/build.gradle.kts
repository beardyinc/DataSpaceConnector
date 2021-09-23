/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}


dependencies {
    implementation(project(":core:protocol:web"))
    implementation(project(":core:transfer"))
    implementation(project(":core:bootstrap"))
    implementation(project(":core:policy:policy-model"))
    implementation(project(":core:policy:policy-engine"))
    implementation(project(":core:schema"))



    implementation(project(":extensions:in-memory:transfer-store-memory"))

    // TODO HACKATHON-1 TASK 6A Commented out until private keys placed in Azure Vault
    implementation(project(":extensions:azure:vault"))

    implementation(project(":extensions:in-memory:policy-registry-memory"))
    implementation(project(":extensions:in-memory:metadata-memory"))
    implementation(project(":extensions:filesystem:configuration-fs"))

    implementation(project(":data-protocols:ids"))
    implementation(project(":data-protocols:ids:ids-policy-mock"))

    implementation(project(":samples:other:copy-between-azure-and-s3"))

    implementation(project(":samples:other:public-rest-api"))
    implementation(project(":extensions:iam:distributed-identity:identity-did-service"))
    implementation(project(":extensions:iam:distributed-identity:identity-did-spi"))
    implementation(project(":extensions:iam:distributed-identity:identity-did-core"))
    implementation(project(":extensions:in-memory:identity-hub-memory"))
    implementation(project(":samples:other:identity-gaiax-verifier"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")

}

application {
    @Suppress("DEPRECATION")
    mainClassName = "org.eclipse.dataspaceconnector.did.ProviderRuntime"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("provider.jar")
}
