/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ftp.scaladsl

import akka.NotUsed
import akka.stream.alpakka.ftp.impl._
import akka.stream.IOResult
import akka.stream.alpakka.ftp.{ FtpFile, FtpFileSettings, RemoteFileSettings }
import akka.stream.alpakka.ftp.RemoteFileSettings.SftpSettings
import akka.stream.alpakka.ftp.impl.{ FtpLike, FtpSourceFactory }
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.jcraft.jsch.JSch
import org.apache.commons.net.ftp.FTPClient
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import java.nio.file.{ Path, Paths }

sealed trait FtpApi[FtpClient] { _: FtpSourceFactory[FtpClient] =>

  /**
   * The refined [[RemoteFileSettings]] type.
   */
  type S <: RemoteFileSettings

  def connect(connectionSettings: S): ftpLike.Handler =
    ftpLike.connect(connectionSettings)(ftpClient()) match {
      case Success(handler) => handler
      case Failure(t) => throw t
    }

  def disconnect(handler: ftpLike.Handler): Unit =
    ftpLike.disconnect(handler)(ftpClient())

  /**
   * Scala API: creates a [[Source]] of [[FtpFile]]s from the remote user `root` directory.
   * By default, `anonymous` credentials will be used.
   *
   * @param host FTP, FTPs or SFTP host
   * @return A [[Source]] of [[FtpFile]]s
   */
  def ls(host: String): Source[FtpFile, NotUsed] =
    ls(host, basePath = Paths.get("/"))

  /**
   * Scala API: creates a [[Source]] of [[FtpFile]]s from a base path.
   * By default, `anonymous` credentials will be used.
   *
   * @param host FTP, FTPs or SFTP host
   * @param basePath Base path from which traverse the remote file server
   * @return A [[Source]] of [[FtpFile]]s
   */
  def ls(host: String, basePath: Path): Source[FtpFile, NotUsed] =
    ls(basePath, defaultSettings(host))

  /**
   * Scala API: creates a [[Source]] of [[FtpFile]]s from the remote user `root` directory.
   *
   * @param host FTP, FTPs or SFTP host
   * @param username username
   * @param password password
   * @return A [[Source]] of [[FtpFile]]s
   */
  def ls(host: String, username: String, password: String): Source[FtpFile, NotUsed] =
    ls(Paths.get("/"), defaultSettings(host, Some(username), Some(password)))

  /**
   * Scala API: creates a [[Source]] of [[FtpFile]]s from a base path.
   *
   * @param host FTP, FTPs or SFTP host
   * @param username username
   * @param password password
   * @param basePath Base path from which traverse the remote file server
   * @return A [[Source]] of [[FtpFile]]s
   */
  def ls(host: String, username: String, password: String, basePath: Path): Source[FtpFile, NotUsed] =
    ls(basePath, defaultSettings(host, Some(username), Some(password)))

  def ls(connectionSettings: S): Source[FtpFile, NotUsed] =
    ls(Paths.get("/"), connectionSettings)

  /**
   * Scala API: creates a [[Source]] of [[FtpFile]]s from a base path.
   *
   * @param basePath Base path from which traverse the remote file server
   * @param connectionSettings connection settings
   * @return A [[Source]] of [[FtpFile]]s
   */
  def ls(basePath: Path, connectionSettings: S): Source[FtpFile, NotUsed] =
    Source.fromGraph(createBrowserGraph(basePath, ftpLike,
        _disconnectAfterCompletion = true)(() => connect(connectionSettings)))

  /**
   * Scala API: creates a [[Source]] of [[ByteString]] from some file [[Path]].
   *
   * @param host FTP, FTPs or SFTP host
   * @param path the file path
   * @return A [[Source]] of [[ByteString]] that materializes to a [[Future]] of [[IOResult]]
   */
  def fromPath(host: String, path: Path): Source[ByteString, Future[IOResult]] =
    fromPath(path, defaultSettings(host))

  /**
   * Scala API: creates a [[Source]] of [[ByteString]] from some file [[Path]].
   *
   * @param host FTP, FTPs or SFTP host
   * @param username username
   * @param password password
   * @param path the file path
   * @return A [[Source]] of [[ByteString]] that materializes to a [[Future]] of [[IOResult]]
   */
  def fromPath(host: String, username: String, password: String, path: Path): Source[ByteString, Future[IOResult]] =
    fromPath(path, defaultSettings(host, Some(username), Some(password)))

  /**
   * Scala API: creates a [[Source]] of [[ByteString]] from some file [[Path]].
   *
   * @param path the file path
   * @param connectionSettings connection settings
   * @param chunkSize the size of transmitted [[ByteString]] chunks
   * @return A [[Source]] of [[ByteString]] that materializes to a [[Future]] of [[IOResult]]
   */
  def fromPath(
      path: Path,
      connectionSettings: S,
      chunkSize: Int = DefaultChunkSize
  ): Source[ByteString, Future[IOResult]] =
    Source.fromGraph(createIOGraph(path, ftpLike, chunkSize,
        _disconnectAfterCompletion = true)(() => connect(connectionSettings)))

  protected[this] implicit val ftpLike: FtpLike[FtpClient, S]
}

object Ftp extends FtpApi[FTPClient] with FtpSource with FtpDefaultSettings {
  type S = FtpFileSettings
  protected[this] val ftpLike: FtpLike[FTPClient, S] = FtpLike.ftpLikeInstance
}
object Ftps extends FtpApi[FTPClient] with FtpsSource with FtpsDefaultSettings {
  type S = FtpFileSettings
  protected[this] val ftpLike: FtpLike[FTPClient, S] = FtpLike.ftpLikeInstance
}
object sFtp extends FtpApi[JSch] with SftpSource with SftpDefaultSettings {
  type S = SftpSettings
  protected[this] val ftpLike: FtpLike[JSch, S] = FtpLike.sFtpLikeInstance
}
