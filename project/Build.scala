import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform._

object Build extends Build {

  lazy val root = Project(
    "sbt2nix",
    file("plugin"),
    settings = commonSettings ++ Seq(
      name := "sbt2nix",
      libraryDependencies ++= Seq(
        "commons-codec"  % "commons-codec"  % "1.6",
        "org.scalaz"  %% "scalaz-core"  % "7.3.0-M3",
        "org.scalaz" %% "scalaz-effect" % "7.3.0-M3")
    )
  )

  def commonSettings =
    Defaults.defaultSettings ++
    scalariformSettings ++
    Seq(
      organization := "sbt2nix",
      scalacOptions ++= Seq("-unchecked", "-deprecation"),
      sbtPlugin := true,
      publishMavenStyle := false,
      sbtVersion in GlobalScope <<= (sbtVersion in GlobalScope) { sbtVersion =>
        System.getProperty("sbt.build.version", sbtVersion)
      },
      scalaVersion <<= (sbtVersion in GlobalScope) {
        case sbt013 if sbt013.startsWith("0.13.") => "2.10.5"
        case sbt012 if sbt012.startsWith("0.12.") => "2.9.3"
        case _ => "2.9.3"
      },
      sbtDependency in GlobalScope <<= (sbtDependency in GlobalScope, sbtVersion in GlobalScope) { (dep, sbtVersion) =>
        dep.copy(revision = sbtVersion)
      },
      publishArtifact in (Compile, packageDoc) := false,
      publishArtifact in (Compile, packageSrc) := false,
      resolvers := Seq(Resolver.sonatypeRepo("releases"))
    )
}
