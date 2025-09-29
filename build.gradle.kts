plugins {
    id("java")
}

group = "com.juleswhite"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.openai:openai-java:1.5.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
tasks.test {
    useJUnitPlatform()
}