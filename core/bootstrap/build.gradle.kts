/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

val slf4jVersion: String by project

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi"))
    api("org.slf4j:slf4j-api:${slf4jVersion}")
}

publishing {
    publications {
        create<MavenPublication>("bootstrap") {
            artifactId = "bootstrap"
            from(components["java"])
        }
    }
}
