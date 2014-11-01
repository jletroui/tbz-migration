import AssemblyKeys._

crossPaths := false

assemblySettings

jarName in assembly := "migrate-tbz.jar"

mainClass in assembly := Some("tbz.MigrateToWordpress")

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.33"

libraryDependencies += "com.zaxxer" % "HikariCP-java6" % "2.1.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "com.typesafe" % "config" % "1.2.1"

