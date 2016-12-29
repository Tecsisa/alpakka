/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ftp

import akka.NotUsed
import akka.stream.alpakka.ftp.RemoteFileSettings.FtpsSettings
import akka.stream.alpakka.ftp.FtpCredentials.AnonFtpCredentials
import akka.stream.alpakka.ftp.scaladsl.Ftps
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import scala.concurrent.Future
import java.net.InetAddress

trait BaseFtpsSpec extends FtpsSupportImpl with BaseSpec {

  type H = Ftps.ftpLike.Handler

  //#create-settings
  val settings = FtpsSettings(
    InetAddress.getByName("localhost"),
    getPort,
    AnonFtpCredentials,
    passiveMode = true
  )
  //#create-settings

  protected def connect(): H =
    Ftps.connect(settings)

  protected def disconnect(handler: H): Unit =
    Ftps.disconnect(handler)

  protected def listFiles(basePath: String): Source[FtpFile, NotUsed] =
    if (basePath.isEmpty)
      Ftps.ls(settings)
    else
      Ftps.ls(
        getFileSystem.getPath(basePath),
        settings
      )

  protected def listFiles(basePath: String, handler: H): Source[FtpFile, NotUsed] =
    if (basePath.isEmpty)
      Ftps.ls(handler)
    else
      Ftps.ls(getFileSystem.getPath(basePath), handler)

  protected def retrieveFromPath(path: String): Source[ByteString, Future[IOResult]] =
    Ftps.fromPath(
      getFileSystem.getPath(path),
      settings
    )

  protected def retrieveFromPath(path: String, handler: H): Source[ByteString, Future[IOResult]] =
    Ftps.fromPath(getFileSystem.getPath(path), handler)

}
