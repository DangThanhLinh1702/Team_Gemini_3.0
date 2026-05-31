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

// Cấu hình mặc định cho lệnh ./gradlew run (Mở Client)
application {
    mainClass.set("auction.client.AppLauncher")
}

tasks.register<JavaExec>("testDatabase") {
    mainClass.set("auction.database.DatabaseConnection")
    description = "Test database connection"
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.auth0:java-jwt:4.5.1")
    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    implementation("com.mysql:mysql-connector-j:8.0.33")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.platform:junit-platform-suite:1.9.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")
    testImplementation("org.assertj:assertj-core:3.24.1")
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

// ==========================================
// CÁC TASK CHẠY TRỰC TIẾP TRONG LÚC CODE
// ==========================================

// Tạo thêm lệnh chạy Server
tasks.register<JavaExec>("runServer") {
    mainClass.set("auction.server.core.ServerMain")
    classpath = sourceSets.main.get().runtimeClasspath
    description = "Khởi chạy Server Đấu giá"
    group = "application"
}

// ==========================================
// CÁC TASK ĐÓNG GÓI RA FILE JAR NỘP BÀI
// ==========================================

// Đóng gói Client: File sinh ra sẽ là auction-1.0-client.jar
tasks.jar {
    manifest {
        attributes["Main-Class"] = "auction.client.AppLauncher"
    }
    archiveClassifier.set("client")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Đóng gói Server: File sinh ra sẽ là auction-1.0-server.jar
tasks.register<Jar>("serverJar") {
    manifest {
        attributes["Main-Class"] = "auction.server.core.ServerMain"
    }
    archiveClassifier.set("server")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}