/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ftp.scaladsl

import akka.NotUsed
import akka.stream.alpakka.ftp.impl._
import akka.stream.IOResult
import akka.stream.alpakka.ftp.FtpFile
import akka.stream.alpakka.ftp.impl.FtpSourceFactory
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.jcraft.jsch.JSch
import org.apache.commons.net.ftp.FTPClient
import scala.concurrent.Future
import java.nio.file.Path

sealed trait FtpApi[FtpClient] extends FtpBaseApi[FtpClient] { _: FtpSourceFactory[FtpClient] =>

  /**
   * Scala API: creates a [[Source]] of [[FtpFile]]s from the remote user `root` directory.
   * By default, `anonymous` credentials will be used.
   *
   * @param host FTP, FTPs or SFTP host
   * @return A [[Source]] of [[FtpFile]]s
   */
  def ls(host: String): Source[FtpFile, NotUsed] =
    ls(host, root)

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
    ls(root, defaultSettings(host, Some(username), Some(password)))

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
    ls(root, connectionSettings)

  /**
   * Scala API: creates a [[Source]] of [[FtpFile]]s from a base path.
   *
   * @param basePath Base path from which traverse the remote file server
   * @param connectionSettings connection settings
   * @return A [[Source]] of [[FtpFile]]s
   */
  def ls(basePath: Path, connectionSettings: S): Source[FtpFile, NotUsed] =
    buildBrowserScalaSource(basePath, () => connect(connectionSettings), disconnectAfterCompletion = true)

  def ls(
      basePath: Path,
      handler: ftpLike.Handler,
      disconnectAfterCompletion: Boolean = false
  ): Source[FtpFile, NotUsed] =
    buildBrowserScalaSource(basePath, () => handler, disconnectAfterCompletion)

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

  def fromPath(
      path: Path,
      connectionSettings: S
  ): Source[ByteString, Future[IOResult]] =
    fromPath(path, connectionSettings, DefaultChunkSize)

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
      chunkSize: Int
  ): Source[ByteString, Future[IOResult]] =
    buildIOScalaSource(path, chunkSize, () => connect(connectionSettings), disconnectAfterCompletion = true)

  def fromPath(
      path: Path,
      handler: ftpLike.Handler,
      chunkSize: Int,
      disconnectAfterCompletion: Boolean = false
  ): Source[ByteString, Future[IOResult]] =
    buildIOScalaSource(path, chunkSize, () => handler, disconnectAfterCompletion)
}

object Ftp extends FtpApi[FTPClient] with FtpSourceParams
object Ftps extends FtpApi[FTPClient] with FtpsSourceParams
object sFtp extends FtpApi[JSch] with SftpSourceParams
