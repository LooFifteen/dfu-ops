rootProject.name = "dfu-ops"

setOf(
    "bson",
).forEach {
    val name = "dfu-ops-$it"
    include(name)
    project(":$name").projectDir = file(it)
}
