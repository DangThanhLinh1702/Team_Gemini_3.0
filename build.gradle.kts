plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"

}

group = "auction"
version = "1.0"

repositories {
    mavenCentral()
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("auction.client.AppLauncher")
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.auth0:java-jwt:4.5.1")
    implementation("org.java-websocket:Java-WebSocket:1.5.3")
}
sourceSets {
    main {
        resources {
            srcDirs("src/main/resources")
        }
    }
}
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
java {
    modularity.inferModulePath.set(true) // Ép Gradle tự động đưa thư viện vào Module Path
}