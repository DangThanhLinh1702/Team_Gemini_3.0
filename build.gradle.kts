plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("checkstyle")
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

// ─── Dependencies ────────────────────────────────────────────────
dependencies {
    // App dependencies
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.auth0:java-jwt:4.5.1")
    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    implementation("com.mysql:mysql-connector-j:8.0.33")

    // ─── Testing ─────────────────────────────────────────────────
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mockito (mock database layer)
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
}

// ─── Test configuration ───────────────────────────────────────────
tasks.test {
    useJUnitPlatform()

    // ✅ FIX: Mở quyền truy cập reflection cho Mockito trên JDK 17+
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED"
    )

    // In kết quả test ra console khi chạy CI
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

// ─── Checkstyle (code style) ─────────────────────────────────────
checkstyle {
    toolVersion = "10.17.0"
    isIgnoreFailures = true
}

// ─── Existing tasks ───────────────────────────────────────────────
tasks.register<JavaExec>("testDatabase") {
    mainClass.set("auction.database.DatabaseConnection")
    description = "Test database connection"
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
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

// (Đã xóa đoạn java { modularity.inferModulePath.set(true) } để tránh conflict với JavaFX/Mockito do không dùng module-info.java)