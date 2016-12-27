/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ftp
package impl

import java.nio.file.Path

private[ftp] trait FtpGraphStageParams[FtpClient, S <: RemoteFileSettings] {

  protected type H

  protected def name: String

  protected def path: Path

  protected def disconnectAfterCompletion: Boolean

  protected def connectF: () => H

  protected def ftpClient: () => FtpClient

  protected def ftpLike: FtpLike[FtpClient, S] { type Handler = H }

}
