plugins {
    id ("java")
    id ("org.jetbrains.kotlin.jvm") version "1.5.31"
    id ("maven-publish")
}

repositories {
    mavenCentral()
    maven(url="https://jitpack.io")
}

dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
    testImplementation ("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    // Other dependencies.
    testImplementation(kotlin("test"))

    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    //implementation 'com.google.code.gson:gson:2.8.6'
    //implementation ("io.reactivex.rxjava2:rxjava:2.2.19")
    //字符编码识别
    implementation ("com.github.albfernandez:juniversalchardet:2.4.0")

    //okhttp LoggingInterceptor
    implementation("com.github.ihsanbal:LoggingInterceptor:3.1.0") {
        exclude(group="org.json", module="json")
    }
    //implementation ("io.github.microutils:kotlin-logging-jvm:3.0.4")
}

java {
	withSourcesJar()
	//withJavadocJar()
}

val group = "com.github.knightwood"
val ver = "1.0.2"
val name="DownloadEngine"

publishing {
    publications {
        create<MavenPublication>("maven"){
            groupId =group
            artifactId = name
            version = ver

            from(components["java"])
        }
    }

}

tasks.test {
    useJUnitPlatform()
}
