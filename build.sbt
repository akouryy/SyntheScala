val dottyVersion = "0.25.0-RC2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "dotty-simple",
    version := "0.1.0",

    scalaVersion := dottyVersion,

    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-language:strictEquality",
      "-migration",
      "-unchecked",
      "-Yindent-colons",
    ),

    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "pprint" % "0.5.6" withDottyCompat scalaVersion.value,
      "com.novocode" % "junit-interface" % "0.11" % "test",
    )
  )
