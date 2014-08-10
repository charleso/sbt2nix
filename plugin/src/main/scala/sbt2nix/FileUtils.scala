package sbt2nix

import java.io.{ FileWriter, Writer }

import sbt._

import scalaz.effect.IO

object FileUtils {

  def save(file: File, xml: String): IO[Unit] =
    fileWriter(file).bracket(closeWriter)(writer => io(writer.write(xml)))

  def fileWriter(file: File): IO[FileWriter] =
    io(new FileWriter(file))

  def fileWriterMkdirs(file: File): IO[FileWriter] =
    io {
      file.getParentFile.mkdirs()
      new FileWriter(file)
    }

  def closeWriter(writer: Writer): IO[Unit] =
    io(writer.close())

  private def io[T](t: => T): IO[T] = scalaz.effect.IO(t)

  def relativize(baseDirectory: File, file: File): File =
    new java.io.File(getRelativePath(baseDirectory, file))

  def getRelativePath(from: File, name: File): String = {
    def rel(base: File): String = {
      val parent = base.getParentFile
      val bpath = base.getCanonicalPath
      val fpath = name.getCanonicalPath
      if (fpath == bpath)
        ""
      else if (fpath.startsWith(bpath))
        fpath.substring(bpath.length() + 1)
      else
        "../" + rel(parent)
    }
    // Nix _needs_ paths to start with './'
    rel(from) match {
      case p if p == "" => "."
      case p if !p.startsWith(".") => "./" + p
      case p => p
    }
  }
}
