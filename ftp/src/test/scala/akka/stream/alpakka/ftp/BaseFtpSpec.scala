/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ftp

import akka.NotUsed
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.stream.alpakka.ftp.RemoteFileSettings.FtpSettings
import akka.stream.alpakka.ftp.FtpCredentials.AnonFtpCredentials
import akka.stream.alpakka.ftp.scaladsl.Ftp
import akka.util.ByteString
import scala.concurrent.Future
import java.net.InetAddress

trait BaseFtpSpec extends PlainFtpSupportImpl with BaseSpec {

  type H = Ftp.ftpLike.Handler

  //#create-settings
  val settings = FtpSettings(
    InetAddress.getByName("localhost"),
    getPort,
    AnonFtpCredentials,
    passiveMode = true
  )
  //#create-settings

  protected def connect(): H =
    Ftp.connect(settings)

  protected def disconnect(handler: H): Unit =
    Ftp.disconnect(handler)

  //#traversing
  protected def listFiles(basePath: String): Source[FtpFile, NotUsed] =
    if (basePath.isEmpty)
      Ftp.ls(settings)
    else
      Ftp.ls(getFileSystem.getPath(basePath), settings)
  //#traversing

  protected def listFiles(basePath: String, handler: H): Source[FtpFile, NotUsed] =
    if (basePath.isEmpty)
      Ftp.ls(handler)
    else
      Ftp.ls(getFileSystem.getPath(basePath), handler)

  //#retrieving
  protected def retrieveFromPath(path: String): Source[ByteString, Future[IOResult]] =
    Ftp.fromPath(getFileSystem.getPath(path), settings)
  //#retrieving

  protected def retrieveFromPath(path: String, handler: H): Source[ByteString, Future[IOResult]] =
    Ftp.fromPath(getFileSystem.getPath(path), handler)
}
