/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ftp
package impl

import akka.stream.stage.{ GraphStageWithMaterializedValue, OutHandler }
import akka.stream.{ Attributes, IOResult, Outlet, SourceShape }
import akka.stream.impl.Stages.DefaultAttributes.IODispatcher
import akka.stream.stage.GraphStageLogic
import akka.util.ByteString
import akka.util.ByteString.ByteString1C
import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal
import java.io.InputStream

private[ftp] trait FtpIOGraphStage[FtpClient, S <: RemoteFileSettings]
    extends GraphStageWithMaterializedValue[SourceShape[ByteString], Future[IOResult]]
    with FtpGraphStageParams[FtpClient, S] {

  def chunkSize: Int

  val shape = SourceShape(Outlet[ByteString](s"$name.out"))

  override def initialAttributes: Attributes =
    super.initialAttributes and Attributes.name(name) and IODispatcher

  def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {

    val matValuePromise = Promise[IOResult]()

    val logic = new GraphStageLogic(shape) {
      import shape.out

      private[this] var handler: Option[H] = None
      private[this] var isOpt: Option[InputStream] = None
      private[this] var readBytesTotal: Long = 0L
      private[this] var connected: Boolean = false

      override def preStart(): Unit = {
        super.preStart()
        try {
          handler = Some(connectF())
          connected = true
          val tryIs = ftpLike.retrieveFileInputStream(path.toAbsolutePath.toString, handler.get)
          isOpt = Some(tryIs.get)
        } catch {
          case NonFatal(t) =>
            t.printStackTrace()
            disconnect()
            failStage(t)
        }
      }

      protected[this] def disconnect(): Unit =
        if (disconnectAfterCompletion && connected) {
          handler.foreach(ftpLike.disconnect)
        }

      setHandler(out,
        new OutHandler {
        def onPull(): Unit =
          readChunk() match {
            case Some(bs) =>
              push(out, bs)
            case None =>
              try {
                isOpt.foreach(_.close())
                disconnect()
              } finally {
                matSuccess()
                complete(out)
              }
          }

        override def onDownstreamFinish(): Unit =
          try {
            isOpt.foreach(_.close())
            disconnect()
          } finally {
            matSuccess()
            super.onDownstreamFinish()
          }
      }) // end of handler

      override def postStop(): Unit =
        try {
          isOpt.foreach(_.close())
        } finally {
          super.postStop()
        }

      protected[this] def matSuccess(): Boolean =
        matValuePromise.trySuccess(IOResult.createSuccessful(readBytesTotal))

      protected[this] def matFailure(t: Throwable): Boolean =
        matValuePromise.trySuccess(IOResult.createFailed(readBytesTotal, t))

      /** BLOCKING I/O READ */
      private[this] def readChunk() = {
        def read(arr: Array[Byte]) =
          isOpt.flatMap { is =>
            val readBytes = is.read(arr)
            if (readBytes > -1) Some(readBytes)
            else None
          }
        val arr = Array.ofDim[Byte](chunkSize)
        read(arr).map { readBytes =>
          readBytesTotal += readBytes
          if (readBytes == chunkSize)
            ByteString1C(arr)
          else
            ByteString1C(arr).take(readBytes)
        }
      }

    } // end of stage logic

    (logic, matValuePromise.future)
  }

}
