val defaultVersion = "9.1.0-SNAPSHOT"
val actualVersion = project.findProperty("revision") ?: defaultVersion
val actualRevision = project.findProperty("changelist") ?: ""


project.group = "com.draeger.medical"
project.version = "$actualVersion$actualRevision"