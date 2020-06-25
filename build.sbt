import sbt._

// root
name := "weePickle-build"
publish / skip := true

//lazy val bench = project
//  .dependsOn(weepickle % "compile->test")
//  .enablePlugins(JmhPlugin)

lazy val core = project
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2"
    )
  )

lazy val implicits = project
  .dependsOn(core, weejson)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    ),
    Compile / sourceGenerators += Def.task[Seq[File]] {
      val pkg = s"com.rallyhealth.weepickle.${shadedVersion.value}.implicits"
      val contents =
        s"""
      package $pkg
      import acyclic.file
      import language.experimental.macros
      /**
       * Auto-generated picklers and unpicklers, used for creating the 22
       * versions of tuple-picklers and case-class picklers
       */
      trait Generated extends com.rallyhealth.weepickle.v1.core.Types{
        ${
          (1 to 22).map { i =>
            def commaSeparated(s: Int => String) = (1 to i).map(s).mkString(", ")

            val writerTypes = commaSeparated(j => s"T$j: From")
            val readerTypes = commaSeparated(j => s"T$j: To")
            val typeTuple = commaSeparated(j => s"T$j")
            val implicitFromTuple = commaSeparated(j => s"implicitly[From[T$j]]")
            val implicitToTuple = commaSeparated(j => s"implicitly[To[T$j]]")
            val lookupTuple = commaSeparated(j => s"x(${j - 1})")
            val fieldTuple = commaSeparated(j => s"x._$j")
            s"""
        implicit def Tuple${i}From[$writerTypes]: TupleNFrom[Tuple$i[$typeTuple]] =
          new TupleNFrom[Tuple$i[$typeTuple]](Array($implicitFromTuple), x => if (x == null) null else Array($fieldTuple))
        implicit def Tuple${i}To[$readerTypes]: TupleNTo[Tuple$i[$typeTuple]] =
          new TupleNTo(Array($implicitToTuple), x => Tuple$i($lookupTuple).asInstanceOf[Tuple$i[$typeTuple]])
        """
          }.mkString("\n")
        }
      }
      """

      val file = sourceManaged.value / pkg.replaceAllLiterally(".", "/") / "Generated.scala"
      IO.write(file, contents)
      Seq(file)
    }
  )

lazy val weepickle = project
  .dependsOn(
    implicits,
    weejson,
    // TODO how?
    //    weepack % "test->test",
    //    LocalProject("weepack") % "test->test",
    `weejson-circe` % "test",
  )

/**
  * ADTs:
  * - Value
  * - BufferedValue
  */
lazy val weejson = project
  .dependsOn(`weejson-jackson`)

lazy val weepack = project
  .dependsOn(core, weepickle)

/**
  * Json string parsing and generation.
  *
  * @see https://github.com/FasterXML/jackson-core
  */
lazy val `weejson-jackson` = (project in file("weejson/jackson"))
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-core" % "2.10.4"
    )
  )

lazy val `weejson-circe` = (project in file("weejson/circe"))
  .dependsOn(core, weejson)
  .settings(
    libraryDependencies ++= {
      Seq(
        "io.circe" %% "circe-parser" % (if (scalaBinaryVersion.value == "2.11") "0.11.1" else "0.12.1")
      )
    }
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
