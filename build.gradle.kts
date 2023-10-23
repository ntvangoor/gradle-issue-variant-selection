subprojects {
    apply(plugin = "java")
}

// dependency other dependencies will depend on
project(":dependency0") {
    val shadowRuntimeElements by configurations.creating {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class, Bundling.SHADOWED))
        }
    }
}

// configure dependency1, dependency2 and dependency3 to depend on dependency0 of which dependency2 requests the shadowed variant
project(":dependency1") {
    dependencies.add("implementation", project(":dependency0"))
}
project(":dependency2") {
    dependencies {
        configurations["implementation"](project(":dependency0")) {
            attributes {
                attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class, Bundling.SHADOWED))
            }
        }
    }
}
project(":dependency3") {
    dependencies.add("implementation", project(":dependency0"))
}

// configure dependency4 to depend on dependency3 which depends on dependency0
project(":dependency4") {
    dependencies.add("implementation", project(":dependency3"))
}

val workingConfiguration: Configuration by configurations.creating
val failingConfiguration: Configuration by configurations.creating

dependencies {
    workingConfiguration(project(":dependency1"))
    workingConfiguration(project(":dependency2"))
    failingConfiguration(project(":dependency1"))
    failingConfiguration(project(":dependency2"))
    failingConfiguration(project(":dependency4"))
}

// choose shadow variant in case of conflict
configurations.all {
    resolutionStrategy {
        capabilitiesResolution.all {
            candidates.forEach {
                if (it.variantName == "shadowRuntimeElements")
                    select(it)
                because("Shadowed jars are preferred over other jars")
            }
        }
    }
}

tasks.create("works") {
    doFirst {
        workingConfiguration.resolve()
    }
}

tasks.create("fails") {
    doFirst {
        failingConfiguration.resolve()
    }
}
