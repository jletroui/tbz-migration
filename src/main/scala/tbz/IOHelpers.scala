package tbz

import java.io.PrintStream

import scala.io.Source

object IOHelpers {
  def replace(file: String, replacements: (String, String)*) = {
    val destFileName = "replaced_" + file

    using(Source.fromURL(getClass.getResource("/" + file), "UTF-8")) { src =>
      using(new PrintStream(destFileName)) { dest =>
        src.getLines().foreach { line =>
          val fixedLine = replacements.foldLeft(line)((text, replacement) => text.replace(replacement._1, replacement._2) )
          dest.println(fixedLine)
        }
      }
    }

    destFileName
  }
}
