scalaVersion := "2.11.7"

scalaSource in Compile := baseDirectory.value / "src" / "main"

scalaSource in Test := baseDirectory.value / "src" / "test"

javaSource in Compile := baseDirectory.value / "src" / "main"

javaSource in Test := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings",
                      "-encoding", "us-ascii")

retrieveManaged := true

libraryDependencies ++= Seq(
  "org.nlogo" % "NetLogo" % "6.0.0-PREVIEW" from
    "https://s3.amazonaws.com/ccl-artifacts/NetLogo-6.0-constructionism-preview.jar"
)

libraryDependencies ++= Seq(
  "org.nlogo" % "NetLogo-tests" % "6.0.0-PREVIEW" from
    "https://s3.amazonaws.com/ccl-artifacts/NetLogo-6.0-constructionism-preview.jar",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "org.ow2.asm" % "asm-all" % "5.0.3" % "test"
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
