package tbz

import scala.util.matching.Regex.Match

class PostMigrator(spipIdToAttachment: Map[Int, Attachment], spipAliasToInternalLink: (String, Int) => InternalLink) extends Logging {
  import tbz.TbzConfig._

  def migrate(post: Post) =
    replacers.foldLeft(post.content) { (migratedContent, replacer) =>
      try {
        replacer.replace(migratedContent, post)
      }
      catch {
        case _: StackOverflowError =>
          log.error(s"    Cannot treat ${replacer.regex} for post ${post.id} (SO)")
          post.content
        case t: Throwable =>
          log.error(s"    Cannot treat ${replacer.regex} for post ${post.id}", t)
          post.content
      }
    }

  val sizedAlignedImage = new Replacer {
    val regex = """\<img(\d+)\|(\w+)\|taille=(\d+)\>"""

    def replaceBy(m: Match, p: Post) = {
      spipIdToAttachment.get(m.group(1).toInt) match {
        case Some(attachment) =>
          import attachment._
          val align = m.group(2)
          val size = m.group(3).toInt

          val finalAlign = if (align == "lightbox") "center" else align
          if (!Set("left", "right", "center").contains(finalAlign)) log.warn(s"Found new alignment: $align in ${m.group(0)}")

          s"<a href='$url'${titleAttr(title)}><img src='$url' ${sizeAttr(size)} alt='$title' title='$title' class='align$finalAlign wp-image-$wpId' /></a>"
        case None =>
          log.warn(s"    Cannot found attachment for spip tag ${m.group(0)} in post ${p.id}. Image will not be displayed.")
          m.group(0)
      }
    }
  }

  val sizedImage = new Replacer {
    val regex = """\<img(\d+)\|taille=(\d+)\>"""

    def replaceBy(m: Match, p: Post) = {
      spipIdToAttachment.get(m.group(1).toInt) match {
        case Some(attachment) =>
          import attachment._
          val size = m.group(2).toInt

          s"<a href='$url'${titleAttr(title)}><img src='$url' ${sizeAttr(size)} alt='$title' title='$title' class='alignnone wp-image-$wpId' /></a>"
        case None =>
          log.warn(s"    Cannot found attachment for spip tag ${m.group(0)} in post ${p.id}. Image will not be displayed.")
          m.group(0)
      }
    }
  }

  val alignedImage = new Replacer {
    val regex = """\<img(\d+)\|(\w+)\>"""

    def replaceBy(m: Match, p: Post) = {
      spipIdToAttachment.get(m.group(1).toInt) match {
        case Some(attachment) =>
          import attachment._
          val align = m.group(2)

          s"<a href='$url'${titleAttr(title)}><img src='$url' width='$width' height='$heigh' alt='$title' title='$title' class='align$align wp-image-$wpId' /></a>"
        case None =>
          log.warn(s"    Cannot found attachment for spip tag ${m.group(0)} in post ${p.id}. Image will not be displayed.")
          m.group(0)
      }
    }
  }

  val documentLink = new Replacer {
    val regex = """\<doc(\d+)\|(\w+)\|cliquable\|lien=([^>]+)\>"""

    def replaceBy(m: Match, p: Post) = {
      val attachment = spipIdToAttachment(m.group(1).toInt)
      import attachment._
      val align = m.group(2)
      val target = m.group(3)

      if (!Set("left", "right", "center").contains(align)) log.warn("Found new alignment: " + align)

      s"<a href='$target'${titleAttr(title)}><img src='$url' alt='$title' title='$title' class='align$align wp-image-$wpId' /></a>"
    }
  }

  val internalLink = new Replacer {
    // ex: [Lillehammer 2014 - Tatiana Janickova reconduit brillamment son titre mondial, devant Nina Reichenbach et Gemma Abant !->http://www.tribalzine.com/?Lillehammer-2014-Tatiana-Janickova]
    def regex = """\[((?:[^-]|-[^>])*)\-\>http://www.tribalzine.com/\?([^\]]+)\]"""

    def replaceBy(m: Match, p: Post) = {
      val text = m.group(1)
      val articleAlias = cleanAlias(m.group(2))
      val target = spipAliasToInternalLink(articleAlias, p.id) match {
        case InternalLink(_, "article", id) => s"$websiteUrl/?p=$id"
        case InternalLink(_, "auteur", id) => s"$websiteUrl/?author=$id"
        case InternalLink(_, "breve", id) => s"$websiteUrl/?p=${id + newsIdOffset}"
        case InternalLink(_, "rubrique", id) => s"$websiteUrl/?cat=$id"
        case _ => ""
      }

      if (target.isEmpty) text else s"<a href='$target'>$text</a>"
    }

    def cleanAlias(alias: String) = {
      // Sometimes, the alias is wrapped between separators. We need to remove the separators to find the real alias.
      val possibleDecorator = List(("+", "+>"), ("", ">"), ("-", "-"), ("+", "+"), ("_", "_"))
      possibleDecorator.find(d => alias.startsWith(d._1) && alias.endsWith(d._2)) match {
        case Some((prefix, suffix)) =>
          alias.substring(prefix.length, alias.length - suffix.length)
        case _ =>
          alias
      }
    }
  }

  val link = new Replacer {
    def regex = """\[((?:[^-]|-[^>])*)\-\>([^\]]+)\]"""

    def replaceBy(m: Match, p: Post) = {
      val text = m.group(1)
      val target = m.group(2)

      s"<a href='$target'>$text</a>"
    }
  }

  val replacers = List(sizedAlignedImage, documentLink, sizedImage, alignedImage, internalLink, link)

  def titleAttr(text: String) = if (text.isEmpty) "" else s" title='$text'"
}

trait Replacer {
  def regex: String
  def replaceBy(m: Match, p: Post): String

  private lazy val compiled = regex.r
  def replace(content: String, p: Post) = compiled.replaceAllIn(content, replaceBy(_, p))
}
