package tbz

import org.slf4j.LoggerFactory

trait Logging {
  protected val log = LoggerFactory.getLogger("[TBZ Migration]")
}
