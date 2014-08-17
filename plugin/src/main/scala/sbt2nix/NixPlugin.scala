package sbt2nix

import sbt.Keys._
import sbt._

import scala.io.Source
import scalaz.NonEmptyList
import scalaz.Scalaz._

object NixPlugin extends Plugin {

  val commandName = SettingKey[String]("nix-command-name", "The name of the command.")

  override def settings: Seq[Setting[_]] = {
    Seq(
      commandName := "nix",
      commands <+= commandName(nixCommand)
    )
  }

  import sbt.complete.DefaultParsers._

  def nixCommand(commandName: String): Command =
    Command(commandName)(_ => EOF)((state, args) => {

      val structure = Project.extract(state).structure
      val base = baseDirectory(structure.rootProject(structure.root), state).getOrElse(???)
      val alldeps = collection.mutable.ListBuffer[Lib]()
      for {
        ref <- structure.allProjectRefs
        project <- Project.getProject(ref, structure).toSeq
        config <- Seq(Compile)
      } yield {
        // TODO Remove this!
        val bd = baseDirectory(ref, state).getOrElse(???)
        val projs = project.dependencies.collect {
          case dependency if isInConfiguration(config, ref, dependency, state) =>
            for {
              name <- Keys.name.in(dependency.project).get(structure.data).orElse(Some(dependency.project.project))
              base <- Keys.baseDirectory.in(dependency.project).get(structure.data)
            } yield (name, FileUtils.relativize(bd, base))
        }.flatten

        val deps = externalDependencies(ref, state)(config)
        deps.foreach(alldeps += _)
        val name = setting(Keys.name in ref, state).getOrElse("Unknown")
        // TODO Use \/
        val version = setting(Keys.version in ref, state).getOrElse("1.0-SNAPSHOT")
        val desc = setting(Keys.description in ref, state)

        def proj(name: String, base: File): String = {
          s"$name = import $base { inherit sbt deps; };"
        }

        val depsPath = FileUtils.relativize(bd, base).getPath
        val s = if (project.aggregate.isEmpty) {
          // Nix can only handle source directories that exist
          val src = setting(Keys.unmanagedSourceDirectories in (ref, config), state).getOrElse(Nil)
            .filter(_.exists()).map(FileUtils.relativize(bd, _).getPath)
          // TODO We need to handle test files + dependencies as well
          // Is there a way not to have to inherit sbt manually here?
          val scalacOpts = evaluateTask(Keys.scalacOptions, ref, state).mkString(" ")
          s"""{ sbt ? import $depsPath/sbt.nix {}, deps ? import $depsPath/deps.nix { inherit sbt; } }:
            |let
            |${projs.map(x => proj(x._1, x._2)).mkString("\n")}
            |
            |in sbt.mkDerivation {
            |  pname = "$name";
            |  version = "$version";
            |  src = ./.;
            |  sources = [ ${src.mkString(" ")} ];
            |  modules = [ ${projs.map(_._1).mkString(" ")} ];
            |  scalacOptions = "$scalacOpts";
            |  buildDepends = [
            |    ${deps.map(x => toName(x.artifact)).map("deps." +).mkString(" ")}
            |  ];
            |  meta = {
            |    ${desc.map(d => "description = " + "\"" + d + "\";").getOrElse("")}
            |  };
            |}""".stripMargin
        } else {
          val aggs = project.aggregate.map {
            subref => subref.project -> FileUtils.relativize(base, baseDirectory(subref, state).getOrElse(???)).getPath
          }
          s"""{ sbt ? import $depsPath/sbt.nix {}, deps ? import $depsPath/deps.nix { inherit sbt; } }:
            |{
            |${aggs.map { case (name, a) => name + " = sbt.callPackage " + a + " {};" }.mkString("\n")}
            |}""".stripMargin
        }
        FileUtils.save(bd / "default.nix", s).unsafePerformIO()
      }
      val deps = s"""{ sbt }:
            |{
            |  ${alldeps.distinct.map(a => lib(a.artifact, a.binary)).mkString("\n  ")}
            |}""".stripMargin
      FileUtils.save(base / "deps.nix", deps).unsafePerformIO()
      val sbtstr = Source.fromInputStream(getClass.getResourceAsStream("/sbt.nix"))
      try {
        FileUtils.save(base / "sbt.nix", sbtstr.getLines().mkString("\n")).unsafePerformIO()
      } finally sbtstr.close()
      state
    })

  def toName(a: ModuleID): String = {
    def sanity(s: String) = s.replace(".", "_")
    List(a.organization, a.name, a.revision).map(sanity).mkString("_")
  }

  def lib(a: ModuleID, f: File): String = {
    // TODO We probably should replace this with pure Java. We need to generate a sha256 in base32:
    // http://lists.science.uu.nl/pipermail/nix-dev/2013-April/010885.html
    import scala.sys.process._
    val sha = stringToProcess(s"nix-prefetch-url file://${f.getAbsolutePath}").!!(ProcessLogger(_ => ())).trim
    s"""
      |${toName(a)} = sbt.artifact {
      |  org = "${a.organization}";
      |  jarname = "${a.name}";
      |  version = "${a.revision}";
      |  sha256 = "$sha";
      |};
      """.stripMargin
  }

  def externalDependencies(ref: ProjectRef, state: State)(configuration: Configuration): List[Lib] = {
    def evalTask[A](key: TaskKey[A]): A = evaluateTask(key in configuration, ref, state)
    def moduleReports(key: TaskKey[UpdateReport]): Seq[ModuleReport] = {
      val updateReport = evalTask(key)
      for {
        configurationReport <- (updateReport configuration configuration.name).toSeq
        moduleReports <- configurationReport.modules
      } yield moduleReports
    }
    def moduleToFile(moduleReports: Seq[ModuleReport]): Map[ModuleID, File] = {
      (for {
        moduleReport <- moduleReports
        (artifact, file) <- moduleReport.artifacts
      } yield moduleReport.module -> file).toMap
    }

    // There may be better/faster ways of retrieving the ModuleIDs, in which case we should definitely change this ASAP!
    val ignore = Set("scala-library", "scala-compiler", "scala-reflect")
    moduleToFile(moduleReports(Keys.update)).map { case (m, f) => Lib(m, f) }.filter {
      case lib => !(lib.artifact.organization == "org.scala-lang" && ignore.contains(lib.artifact.name))
    }.toList
  }

  def evaluateTask[A](key: TaskKey[A], ref: ProjectRef, state: State): A =
    EvaluateTask(structure(state), key, state, ref, EvaluateTask defaultConfig state) match {
      case Some((_, Value(a))) => a
      case Some((_, Inc(inc))) => sys.error("Error evaluating task '%s': %s".format(key.key, Incomplete.show(inc.tpe)))
      case None => sys.error("Undefined task '%s' for '%s'!".format(key.key, ref.project))
    }

  def baseDirectory(ref: Reference, state: State): Validation[File] =
    setting(Keys.baseDirectory in ref, state)

  def isInConfiguration(configuration: Configuration,
    ref: ProjectRef,
    dependency: ClasspathDep[ProjectRef],
    state: State): Boolean = {
    Classpaths.mapped(
      dependency.configuration,
      Configurations.names(Classpaths.getConfigurations(ref, structure(state).data)),
      Configurations.names(Classpaths.getConfigurations(dependency.project, structure(state).data)),
      "compile", "*->compile"
    )(configuration.name).nonEmpty
  }

  def setting[A](key: SettingKey[A], state: State): Validation[A] =
    key get structure(state).data match {
      case Some(a) => a.success
      case None => "Undefined setting '%s'!".format(key.key).failNel
    }

  def extracted(state: State): Extracted = Project.extract(state)

  def structure(state: State): BuildStructure = extracted(state).structure

  type Validation[A] = scalaz.Validation[NonEmptyList[String], A]
}

case class Lib(artifact: ModuleID, binary: File)
