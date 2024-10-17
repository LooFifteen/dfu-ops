group = "dev.lu15"

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = rootProject.group

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))

        withSourcesJar()
        withJavadocJar()
    }

    repositories {
        mavenCentral()
        maven("https://libraries.minecraft.net/")
    }

    dependencies {
        "compileOnlyApi"("com.mojang:datafixerupper:8.0.16")
        "compileOnly"("org.jetbrains:annotations:26.0.1")

        "testImplementation"("com.mojang:datafixerupper:8.0.16")
    }

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "hypera"
                url = uri("https://repo.hypera.dev/releases/")
                credentials(PasswordCredentials::class)
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = project.name
                from(components["java"])
            }
        }
    }

    tasks.withType<Javadoc> {
        (options as? StandardJavadocDocletOptions)?.links("https://kvverti.github.io/Documented-DataFixerUpper/snapshot/")
    }
}