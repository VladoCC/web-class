import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val koin_version = "3.1.3"

plugins {
    kotlin("jvm") version "1.5.20"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
    // id("org.cqfn.diktat.diktat-gradle-plugin") version "1.0.0-rc.3"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
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
    implementation("io.ktor:ktor-server-netty:1.6.4")
    implementation("io.ktor:ktor-html-builder:1.6.4")
    implementation("io.ktor:ktor-server-sessions:1.6.4")
    implementation("io.ktor:ktor-client-core:1.6.4")
    implementation("io.ktor:ktor-client-cio:1.6.4")
    implementation("io.ktor:ktor-client-gson:1.6.4")
    implementation("io.ktor:ktor-gson:1.6.4")
    implementation("io.ktor:ktor-auth:1.6.4")
    implementation("io.ktor:ktor-auth-jwt:1.6.4")
    implementation("com.github.komputing:khash:1.1.1")
    implementation("ch.qos.logback:logback-classic:1.2.6")
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
    implementation("org.jetbrains.exposed:exposed-core:0.36.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.36.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.36.1")
    implementation("org.slf4j:slf4j-nop:1.7.32")
    implementation("org.xerial:sqlite-jdbc:3.36.0.2")
    implementation("com.zaxxer:HikariCP:3.4.5")

    testImplementation("io.ktor:ktor-server-test-host:1.6.4")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.5.20")
}
tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
application {
    mainClassName = "ServerKt"
}

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "ServerKt"))
        }
    }
}

// diktat {
//    inputs = files("src/main/**/*.kt")
// }

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint") // Version should be inherited from parent

    repositories {
        // Required to download KtLint
        mavenCentral()
    }

    // Optionally configure plugin
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        // debug.set(true)
        disabledRules.set(setOf("no-wildcard-imports"))
        filter {
            include("**/main/**")
            exclude("**/test/**")
        }
    }
}
