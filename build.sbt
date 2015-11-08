
lazy val commonRootSettings = Seq(
  organization := "me.jeffmay",
  organizationName := "Jeff May",
  scalaVersion := "2.11.7"
)

lazy val root = (project in file("."))
  .settings(commonRootSettings)
  .settings(
    name := "context",
    publish := {},
    publishLocal := {}
  )
  .aggregate(core, server, client, example)

lazy val commonProjectSettings = commonRootSettings ++ Seq(
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-deprecation:false",
    "-feature",
    "-optimize",
    "-unchecked",
    "-Xfatal-warnings",
    "-Ywarn-dead-code"
  )
)

lazy val core = (project in file("core"))
  .settings(commonProjectSettings)
  .settings(
    name := "context-core",
    version := "0.1.0",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-functional" % Versions.play,  // switch to Cats when stable
      "com.typesafe.play" %% "play-ws" % Versions.play,
      "org.scalactic" %% "scalactic" % Versions.scalactic,
      "me.jeffmay" %% "scalacheck-ops" % Versions.scalacheckOps % "test",
      "org.scalacheck" %% "scalacheck" % Versions.scalacheck % "test",
      "org.scalatest" %% "scalatest" % Versions.scalatest % "test"
    )
  )

lazy val client = (project in file("client"))
  .settings(commonProjectSettings)
  .settings(
    name := "context-play-ws",
    version := "0.1.0",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-functional" % Versions.play,  // switch to Cats when stable
      "com.typesafe.play" %% "play-ws" % Versions.play,
      "org.scalactic" %% "scalactic" % Versions.scalactic,
      "me.jeffmay" %% "scalacheck-ops" % Versions.scalacheckOps % "test",
      "org.scalacheck" %% "scalacheck" % Versions.scalacheck % "test",
      "org.mockito" % "mockito-core" % Versions.mockito % "test",
      "org.scalatest" %% "scalatest" % Versions.scalatest % "test"
    )
  ).dependsOn(core)

lazy val server = (project in file("server"))
  .settings(commonProjectSettings)
  .settings(
    name := "context-play-server",
    version := "0.0.1",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % Versions.play,
      "com.typesafe.play" %% "play-functional" % Versions.play,  // switch to Cats when stable
      "org.scalactic" %% "scalactic" % Versions.scalactic,
      "me.jeffmay" %% "scalacheck-ops" % Versions.scalacheckOps % "test",
      "org.scalacheck" %% "scalacheck" % Versions.scalacheck % "test",
      "org.scalatest" %% "scalatest" % Versions.scalatest % "test"
    )
  ).dependsOn(core)

lazy val example = (project in file("example"))
  .settings(commonProjectSettings)
  .settings(
    name := "example",
    version := "0.0.1",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-functional" % Versions.play,  // switch to Cats when stable
      "com.typesafe.play" %% "play-ws" % Versions.play,
      "org.scalactic" %% "scalactic" % Versions.scalactic
    ),
    // Don't publish this source code example
    publish := {},
    publishLocal := {}
  )
  .enablePlugins(PlayScala)
  .dependsOn(core, client, server)

