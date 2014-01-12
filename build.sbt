scalaVersion := "2.10.1"

scalaOrganization := "org.scala-lang.virtualized"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
   "scalapipe." + artifact.extension
}

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test"

