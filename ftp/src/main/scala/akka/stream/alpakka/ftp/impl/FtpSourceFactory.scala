/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ftp.impl

import akka.stream.alpakka.ftp.FtpCredentials.{ AnonFtpCredentials, NonAnonFtpCredentials }
import akka.stream.alpakka.ftp.RemoteFileSettings
import akka.stream.alpakka.ftp.RemoteFileSettings._
import com.jcraft.jsch.JSch
import org.apache.commons.net.ftp.FTPClient
import java.net.InetAddress
import java.nio.file.Path

private[ftp] trait FtpSourceFactory[FtpClient] { self =>

  type S <: RemoteFileSettings

  protected[this] final val DefaultChunkSize = 8192

  protected[this] def ftpClient: () => FtpClient

  protected[this] def ftpBrowserSourceName: String

  protected[this] def ftpIOSourceName: String

  protected[this] def createBrowserGraph(
      _basePath: String,
      _ftpLike: FtpLike[FtpClient, S],
      _disconnectAfterCompletion: Boolean
  )(_handlerF: () => _ftpLike.Handler): FtpBrowserGraphStage[FtpClient, S] =
    new FtpBrowserGraphStage[FtpClient, S] {
      type H = _ftpLike.Handler
      lazy val name: String = ftpBrowserSourceName
      val basePath: String = _basePath
      val disconnectAfterCompletion: Boolean = _disconnectAfterCompletion
      val connectF: () => H = _handlerF
      val ftpClient: () => FtpClient = self.ftpClient
      val ftpLike: FtpLike[FtpClient, S] { type Handler = H } = _ftpLike
    }

  protected[this] def createIOGraph(
      _path: Path,
      _ftpLike: FtpLike[FtpClient, S],
      _chunkSize: Int,
      _disconnectAfterCompletion: Boolean
  )(_handlerF: () => _ftpLike.Handler): FtpIOGraphStage[FtpClient, S] =
    new FtpIOGraphStage[FtpClient, S] {
      type H = _ftpLike.Handler
      lazy val name: String = ftpIOSourceName
      val path: Path = _path
      val disconnectAfterCompletion: Boolean = _disconnectAfterCompletion
      val connectF: () => H = _handlerF
      val ftpClient: () => FtpClient = self.ftpClient
      val ftpLike: FtpLike[FtpClient, S] { type Handler = H } = _ftpLike
      val chunkSize: Int = _chunkSize
    }

  protected[this] def defaultSettings(
      hostname: String,
      username: Option[String] = None,
      password: Option[String] = None
  ): S
}

private[ftp] trait FtpSource extends FtpSourceFactory[FTPClient] {
  protected final val FtpBrowserSourceName = "FtpBrowserSource"
  protected final val FtpIOSourceName = "FtpIOSource"
  protected val ftpClient: () => FTPClient = () => new FTPClient
  protected val ftpBrowserSourceName: String = FtpBrowserSourceName
  protected val ftpIOSourceName: String = FtpIOSourceName
}

private[ftp] trait FtpsSource extends FtpSourceFactory[FTPClient] {
  protected final val FtpsBrowserSourceName = "FtpsBrowserSource"
  protected final val FtpsIOSourceName = "FtpsIOSource"
  protected val ftpClient: () => FTPClient = () => new FTPClient
  protected val ftpBrowserSourceName: String = FtpsBrowserSourceName
  protected val ftpIOSourceName: String = FtpsIOSourceName
}

private[ftp] trait SftpSource extends FtpSourceFactory[JSch] {
  protected final val sFtpBrowserSourceName = "sFtpBrowserSource"
  protected final val sFtpIOSourceName = "sFtpIOSource"
  protected val ftpClient: () => JSch = () => new JSch
  protected val ftpBrowserSourceName: String = sFtpBrowserSourceName
  protected val ftpIOSourceName: String = sFtpIOSourceName
}

private[ftp] trait FtpDefaultSettings {
  protected def defaultSettings(
      hostname: String,
      username: Option[String],
      password: Option[String]
  ): FtpSettings =
    FtpSettings(
      InetAddress.getByName(hostname),
      DefaultFtpPort,
      if (username.isDefined)
        NonAnonFtpCredentials(username.get, password.getOrElse(""))
      else
        AnonFtpCredentials
    )
}

private[ftp] trait FtpsDefaultSettings {
  protected def defaultSettings(
      hostname: String,
      username: Option[String],
      password: Option[String]
  ): FtpsSettings =
    FtpsSettings(
      InetAddress.getByName(hostname),
      DefaultFtpsPort,
      if (username.isDefined)
        NonAnonFtpCredentials(username.get, password.getOrElse(""))
      else
        AnonFtpCredentials
    )
}

private[ftp] trait SftpDefaultSettings {
  protected def defaultSettings(
      hostname: String,
      username: Option[String],
      password: Option[String]
  ): SftpSettings =
    SftpSettings(
      InetAddress.getByName(hostname),
      DefaultSftpPort,
      if (username.isDefined)
        NonAnonFtpCredentials(username.get, password.getOrElse(""))
      else
        AnonFtpCredentials
    )
}
