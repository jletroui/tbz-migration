package object tbz {
  /** Use a resource and make sure it is closed at the end. **/
  def using[CLOSEABLE <: { def close(): Unit }, B](closeable: CLOSEABLE)(action: CLOSEABLE => B): B =
    try {
      action(closeable)
    } finally {
      closeable.close()      
    }
 }
