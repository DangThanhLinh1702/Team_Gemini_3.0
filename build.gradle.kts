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

// ✅ Khai báo JavaFX
javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

// ✅ Chỉ định file main
application {
    mainClass.set("auction.client.ClientMain")
}

dependencies {
    // Thêm thư viện khác nếu cần
    implementation("com.google.code.gson:gson:2.10.1")
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