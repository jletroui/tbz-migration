package tbz

import java.io.File

import tbz.DB._

import scala.sys.process._

object MigrateToWordpress extends App with Logging {
  import tbz.TbzConfig._

  if (args.length == 2) {
    log.info("Migrating data phase 1...")
    prepareWordpressDatabase(args(0), args(1))

    log.info("Migrating data phase 2...")
    using(new DB(s"jdbc:mysql://localhost/$wpDbName", "root", rootPassword, DB.MySQLDriver)) { wpDb =>
      using(new DB(s"jdbc:mysql://localhost/$spipDbName", "root", rootPassword, DB.MySQLDriver)) { spipDb =>

        val migrator = new PostMigrator(loadAttachments(spipDb), loadLinks(spipDb))

        fixPosts(wpDb, migrator)
      }
    }

    log.info(s"Done!")
  }
  else
    printUsage()

  def printUsage() {
    println("Tribal Zine DB migration tool from Spip to Wordpress")
    println()
    println("Usage: java -jar migrate-tbz.jar <source sql export> <destination sql export>")
    println("  <source sql export> database export of TribalZine spip website")
    println("  <destination sql export> database export of Wordpress website after having installed and configured plugins, theme, etc...")
    println()
    println("Example: java -jar migrate-tbz.jar tbz_export_2014-09-08.sql wordpress_fresh_noplugins.sql")
  }

  def prepareWordpressDatabase(spipExport: String, wpDevExport: String) {
    val setupScript = IOHelpers.replace("setup.sql",
      "@SPIP_DB@" -> spipDbName,
      "@WP_DB@" -> wpDbName)
    val migrationScript = IOHelpers.replace("migration.sql",
      "@SPIP_DB@" -> spipDbName,
      "@UPLOAD_DIR@" -> uploadLocation,
      "@ATTACHMENT_ID_OFFSET@" -> attachmentIdOffset.toString,
      "@NEWS_ID_OFFSET@" -> newsIdOffset.toString,
      "@PREFIX@" -> wpPrefix)

    log.info("  Reseting wp & spip databases...")
    require((new File(setupScript) #> mysqlCmd !) == 0, "Recreating wp & spip databases failed...")
    log.info("  Loading spip TribalZine data...")
    require((new File(spipExport) #> s"$mysqlCmd -D $spipDbName" !) == 0, "Loading spip TribalZine data failed...")
    log.info("  Creating destination wp tables...")
    require((new File(wpDevExport) #> s"$mysqlCmd -D $wpDbName" !) == 0, "Creating destination wp tables...")
    log.info("  Migrating data through SQL...")
    require((new File(migrationScript) #> s"$mysqlCmd -D $wpDbName" !) == 0, "Migrating data through SQL failed...")
  }

  def mysqlCmd =
    if (rootPassword.isEmpty) "mysql -u root"
    else s"mysql -u root -p$rootPassword"

  def loadAttachments(spipDb: DB) = {
    log.info("  Loading attachments...")

    val attachments = spipDb.queryEach("SELECT id_document, titre, fichier, extension, largeur, hauteur FROM spip_documents") { rs =>
      Attachment(
        rs.getInt("id_document"),
        rs.getString("titre"),
        rs.getString("fichier"),
        rs.getString("extension"),
        rs.getInt("largeur"),
        rs.getInt("hauteur"))
    }

    attachments
      .map(a => a.spipId -> a)
      .toMap
  }

  def loadLinks(spipDb: DB) = {
    log.info("  Loading links...")

    val internalLinks = spipDb.queryEach("SELECT url, type, id_objet FROM spip_urls") { rs =>
      InternalLink(
        rs.getString("url"),
        rs.getString("type"),
        rs.getInt("id_objet"))
    }

    val objectNamesIndex = internalLinks
      .map(il => il.alias -> il)
      .toMap

    val artDirectRegex = """art(?:icle)?(\d+)""".r
    def articleDirectIdReference(alias: String) =
      artDirectRegex
        .findFirstMatchIn(alias)
        .map(m => InternalLink(alias, "article", m.group(1).toInt))

    val artIndirectRegex = """[^,]+,(\d+)""".r
    def articleIndirectIdReference(alias: String) =
      artIndirectRegex
        .findFirstMatchIn(alias)
        .map(m => InternalLink(alias, "article", m.group(1).toInt))

    (alias: String, postId: Int) => {
      objectNamesIndex.get(alias) // It's a well identified name / url
        .orElse(articleDirectIdReference(alias)) // It's a generic article id, like 'art1549' or 'article1549'.
        .orElse(articleIndirectIdReference(alias)) // It's an indirect reference, like 'K-124-Days-2009-1ste-manche,1523'
        .getOrElse(throw new Exception(s"Cannot find internal link for link '->$alias' in post $postId"))
    }
  }

  def fixPosts(wpDb: DB, migrator: PostMigrator): Unit = {
    log.info("  Fixing posts...")

    for {
      id <- Range(0, 10000, batchSize)
    } {
      log.info(s"    Fixing posts $id to ${id + batchSize}...")
      val posts = wpDb.queryEach(s"select ID, post_content from ${wpPrefix}posts where ID > ? AND ID <= ?", id, id + batchSize) { rs =>
        Post(rs.getInt("ID"), rs.getString("post_content"))
      }
      posts.foreach { post =>
        val fixedContent = migrator.migrate(post)
        wpDb.update(s"UPDATE ${wpPrefix}posts SET post_content=? WHERE ID=?", fixedContent, post.id)
      }
    }

  }
}
