plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.10"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.github.javaparser:javaparser-core:3.25.1")

    implementation("io.ksmt:ksmt-core:0.5.6")
    implementation("io.ksmt:ksmt-z3:0.5.6")
}

application {
    mainClass.set("pt.test.task.MainKt")
}
