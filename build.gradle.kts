import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("com.github.johnrengelman.shadow") version "8.1.1"
	id("org.springframework.boot") version "3.2.5"
	id("io.spring.dependency-management") version "1.1.4"
	kotlin("jvm") version "1.9.23"
	kotlin("plugin.spring") version "1.9.23"
	kotlin("plugin.jpa") version "1.9.23"
}

group = "com.hgstrat"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
	mavenCentral()
	gradlePluginPortal()
}

dependencies {
	//compileClasspath("com.github.johnrengelman:shadow:8.1.1")

	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	implementation("org.hibernate.orm:hibernate-community-dialects:6.4.4.Final")
	implementation("org.xerial:sqlite-jdbc:3.45.3.0")
//	implementation("net.openhft:chronicle-map:3.25ea6")
	implementation("io.github.binance:binance-futures-connector-java:3.0.3")
	implementation("org.seleniumhq.selenium:selenium-java:4.20.0")
	implementation("org.seleniumhq.selenium:selenium-support:4.20.0")
	implementation("io.github.bonigarcia:webdrivermanager:5.8.0")

	//implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "21"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.build {
	dependsOn("shadowJar")
}