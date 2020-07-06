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
      "com.lihaoyi" %% "fansi" % "0.2.7" withDottyCompat dottyVersion,
      "com.lihaoyi" %% "pprint" % "0.5.6" withDottyCompat dottyVersion,
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.scala-lang.modules" %% "scala-collection-contrib" % "0.1.0" withDottyCompat dottyVersion,
    )
  )
