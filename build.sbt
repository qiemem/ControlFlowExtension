enablePlugins(org.nlogo.build.NetLogoExtension)

scalaVersion := "2.11.7"

scalaSource in Compile := baseDirectory.value / "src" / "main"

scalaSource in Test := baseDirectory.value / "src" / "test"

javaSource in Compile := baseDirectory.value / "src" / "main"

javaSource in Test := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings",
                      "-encoding", "us-ascii")

retrieveManaged := true

netLogoClassManager := "org.nlogo.extensions.array.ArrayExtension"

netLogoExtName :=    "cf"

netLogoClassManager := "org.nlogo.extensions.cf.CFExtension"

netLogoTarget :=
  org.nlogo.build.NetLogoExtension.directoryTarget(baseDirectory.value)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "org.ow2.asm" % "asm-all" % "5.0.3" % "test"
)

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

netLogoVersion := "6.0.0-M2"
