/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ftp
package impl

import akka.NotUsed
import akka.util.ByteString
import akka.stream.alpakka.ftp.RemoteFileSettings.SftpSettings
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import com.jcraft.jsch.JSch
import org.apache.commons.net.ftp.FTPClient
import scala.concurrent.Future
import java.nio.file.{ Path, Paths }

private[ftp] trait FtpBaseApi[FtpClient] { _: FtpSourceFactory[FtpClient] =>

  /**
   * The refined [[RemoteFileSettings]] type.
   */
  type S <: RemoteFileSettings

  def connect(connectionSettings: S): ftpLike.Handler =
    ftpLike.connect(connectionSettings)(ftpClient()).get

  def disconnect(handler: ftpLike.Handler): Unit =
    ftpLike.disconnect(handler)(ftpClient())

  protected[this] val root: Path = Paths.get("/")

  protected[this] implicit val ftpLike: FtpLike[FtpClient, S]

  protected[this] def buildBrowserScalaSource(
      basePath: Path,
      handlerF: () => ftpLike.Handler,
      disconnectAfterCompletion: Boolean
  ): Source[FtpFile, NotUsed] =
    Source.fromGraph(createBrowserGraph(basePath, ftpLike, disconnectAfterCompletion)(handlerF))

  protected[this] def buildIOScalaSource(
      path: Path,
      chunkSize: Int,
      handlerF: () => ftpLike.Handler,
      disconnectAfterCompletion: Boolean
  ): Source[ByteString, Future[IOResult]] =
    Source.fromGraph(createIOGraph(path, ftpLike, chunkSize, disconnectAfterCompletion)(handlerF))
}

private[ftp] trait FtpSourceParams extends FtpSource with FtpDefaultSettings {
  type S = FtpFileSettings
  protected[this] val ftpLike: FtpLike[FTPClient, S] = FtpLike.ftpLikeInstance
}

private[ftp] trait FtpsSourceParams extends FtpsSource with FtpsDefaultSettings {
  type S = FtpFileSettings
  protected[this] val ftpLike: FtpLike[FTPClient, S] = FtpLike.ftpLikeInstance
}

private[ftp] trait SftpSourceParams extends SftpSource with SftpDefaultSettings {
  type S = SftpSettings
  protected[this] val ftpLike: FtpLike[JSch, S] = FtpLike.sFtpLikeInstance
}
