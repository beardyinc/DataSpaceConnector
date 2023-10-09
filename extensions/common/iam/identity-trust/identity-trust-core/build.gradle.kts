plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:identity-trust-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":extensions:common:iam:identity-trust:identity-trust-service"))
    testImplementation(testFixtures(project(":spi:common:identity-trust-spi")))
    testImplementation(project(":core:common:junit"))
}

