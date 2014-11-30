package tbz

import java.io.File

import com.typesafe.config.ConfigFactory

object TbzConfig {
  val config = ConfigFactory.parseFile(new File("migrate-tbz.conf"))
  val spipDbName = config.getString("spip_database")
  val wpDbName = config.getString("wp_database")
  val wpPrefix = config.getString("wp_table_prefix")
  val rootPassword = config.getString("root_password")
  val uploadLocation = config.getString("upload_relative_location")
  val websiteUrl = config.getString("website_url")
  val attachmentIdOffset = config.getInt("attachment_id_offset")
  val newsIdOffset = config.getInt("news_id_offset")
  val batchSize = config.getInt("batch_size")
}
