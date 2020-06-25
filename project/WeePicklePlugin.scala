import sbt.Keys._
import sbt.{Def, Developer, url, _}
import sbt.plugins.JvmPlugin

object WeePicklePlugin extends AutoPlugin {

  override def requires = JvmPlugin

  override def trigger = allRequirements

  object autoImport {

    val shadedVersion = settingKey[String]("e.g. v1")
  }

  private val acyclic = settingKey[ModuleID]("")

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    // Mill conventions because I don't want to mv dirs right now:
    Compile / scalaSource := baseDirectory.value / "src",
    Compile / sourceDirectories += baseDirectory.value / s"src-${scalaBinaryVersion.value}",
    Test / scalaSource := baseDirectory.value / "test" / "src",
    organizationHomepage := Some(url("https://www.rallyhealth.com")),
    homepage := Some(url("https://github.com/rallyhealth/weePickle")),
    licenses := Seq(("MIT License", url("https://opensource.org/licenses/mit-license.html"))),
    startYear := Some(2019),

    scalacOptions ++= Seq("-feature"),

    autoImport.shadedVersion := "v" + version.value.split('.').head,

    acyclic := "com.lihaoyi" %% "acyclic" % (if (scalaBinaryVersion.value == "2.11") "0.1.8" else "0.2.0"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % "0.7.2" % "test",
      compilerPlugin(acyclic.value),
      acyclic.value % "provided"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    autoCompilerPlugins := true,
  )

  //    Test / test := {
  //      mimaReportBinaryIssues.value
  //      (Test / test).value
  //    },
  //    publish := publish.dependsOn(mimaReportBinaryIssues).value,
  //    mimaPreviousArtifacts ++= {
  //      rallyVersioningPreviousRelease.value
  //        .map(organization.value %% name.value % _.toString)
  //        .toSet
  //    }
}
