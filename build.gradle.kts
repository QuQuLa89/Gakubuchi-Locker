plugins {
    kotlin("jvm") version "2.4.21"
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "com.gakubuchilocker"
version = "1.3.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib"))
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
}

tasks {
    test {
        useJUnitPlatform()
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    shadowJar {
        archiveClassifier.set("")
        // sqlite-jdbc はJNIネイティブライブラリを含むため relocate 不可
    }
    build {
        dependsOn(shadowJar)
    }
}
