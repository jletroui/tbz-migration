package tbz

case class Attachment(spipId: Int, title: String, file: String, extension: String, width: Int, heigh: Int) {
  val url = s"${TbzConfig.uploadLocation}/$file"
  val wpId = TbzConfig.attachmentIdOffset + spipId

  def sizeAttr(bigDim: Int) = {
    if (width > heigh) s"width='$bigDim' height='${math.round(heigh * bigDim.toDouble / width)}'"
    else s"width='${math.round(width * bigDim.toDouble / heigh)}' height='$bigDim'"
  }
}
