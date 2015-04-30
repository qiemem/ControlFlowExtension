scalaVersion := "2.9.2"

scalaSource in Compile := baseDirectory.value / "src" / "main"

scalaSource in Test := baseDirectory.value / "src" / "test"

javaSource in Compile := baseDirectory.value / "src" / "main"

javaSource in Test := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings",
                      "-encoding", "us-ascii")

retrieveManaged := true

libraryDependencies ++= Seq(
  "org.nlogo" % "NetLogo" % "5.2.0" from
    "http://ccl.northwestern.edu/netlogo/5.2.0/NetLogo.jar"
)

libraryDependencies ++= Seq(
  "org.nlogo" % "NetLogo-tests" % "5.2.0" % "test" from
    "http://ccl.northwestern.edu/netlogo/5.2.0/NetLogo-tests.jar",
  "org.scalatest" %% "scalatest" % "1.8" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "asm" % "asm-all" % "3.3.1" % "test"
)

artifactName := { (_, _, _) => "cf.jar" }

packageOptions +=
  Package.ManifestAttributes(
    ("Extension-Name", "cf"),
    ("Class-Manager", "org.nlogo.extensions.cf.CFExtension"),
    ("NetLogo-Extension-API-Version", "5.0"))

packageBin in Compile := {
  val jar = (packageBin in Compile).value
  val base = baseDirectory.value
  IO.copyFile(jar, base / "cf.jar")
  jar
}

test in Test := {
  val _ = (packageBin in Compile).value
  (test in Test).value
}

cleanFiles ++= {
  val base = baseDirectory.value
  val cfDir = base / "extensions" / "cf"
  Seq(base / "cf.jar")
}
