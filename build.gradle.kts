import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.20"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.cqfn.diktat.diktat-gradle-plugin") version "1.0.0-rc.3"
}
group = "me.wintermute"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/kotlin/ktor")
    }
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlinx")
    }
    maven {
        url = uri("https://jitpack.io")
    }
}
dependencies {
    testImplementation(kotlin("test-junit"))
    implementation("io.ktor:ktor-server-netty:1.4.0")
    implementation("io.ktor:ktor-html-builder:1.4.0")
    implementation("io.ktor:ktor-server-sessions:1.4.0")
    implementation("io.ktor:ktor-client-core:1.4.0")
    implementation("io.ktor:ktor-client-cio:1.4.0")
    implementation("io.ktor:ktor-client-gson:1.4.0")
    implementation("io.ktor:ktor-gson:1.4.0")
    implementation("io.ktor:ktor-auth:1.4.0")
    implementation("io.ktor:ktor-auth-jwt:1.4.0")
    implementation("com.github.komputing:khash:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
    implementation("org.jetbrains.exposed:exposed-core:0.34.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.34.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.34.1")
    implementation("org.slf4j:slf4j-nop:1.7.30")
    implementation("org.xerial:sqlite-jdbc:3.30.1")
    implementation("com.zaxxer:HikariCP:3.4.5")

    testImplementation("io.ktor:ktor-server-test-host:1.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.5.20")
}
tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
application {
    mainClassName = "ServerKt"
}

tasks{
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "ServerKt"))
        }
    }
}

diktat {
    inputs = files("src/main/**/*.kt")
}