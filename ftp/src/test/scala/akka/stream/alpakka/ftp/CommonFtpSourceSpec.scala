/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ftp

import akka.NotUsed
import akka.stream.IOResult
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final class FtpSourceSpec extends BaseFtpSpec with CommonFtpSourceSpec
final class SftpSourceSpec extends BaseSftpSpec with CommonFtpSourceSpec
final class FtpsSourceSpec extends BaseFtpsSpec with CommonFtpSourceSpec {
  setAuthValue("TLS")
  setUseImplicit(false)
}

trait CommonFtpSourceSpec extends BaseSpec {

  implicit val system = getSystem
  implicit val mat = getMaterializer

  "FtpBrowserSource" should {
    "list all files from root" in {
      val basePath = ""
      generateFiles(30, 10, basePath)
      val probe =
        listFiles(basePath).toMat(TestSink.probe)(Keep.right).run()
      probe.request(40).expectNextN(30)
      probe.expectComplete()
    }
    "list all files from non-root" in {
      val basePath = "/foo"
      generateFiles(30, 10, basePath)
      val probe =
        listFiles(basePath).toMat(TestSink.probe)(Keep.right).run()
      probe.request(40).expectNextN(30)
      probe.expectComplete()
    }
    "list all files re-using previous connection" in {
      val basePath = ""
      generateFiles(30, 10, basePath)
      val handler = connect()
      val probe =
        listFiles(basePath, handler).toMat(TestSink.probe)(Keep.right).run()
      probe.request(40).expectNextN(30)
      probe.expectComplete()
      disconnect(handler)
    }
    "throw an error when connection is closed before materialization" in {
      val basePath = ""
      generateFiles(30, 10, basePath)
      val handler = connect()
      disconnect(handler)
      val probe = listFiles(basePath, handler).toMat(TestSink.probe)(Keep.right).run()
      probe.expectSubscriptionAndError()
    }
  }

  "FtpIOSource" should {
    "retrieve a file from path as a stream of bytes" in {
      val fileName = "sample_io"
      putFileOnFtp(FtpBaseSupport.FTP_ROOT_DIR, fileName)
      val (result, probe) =
        retrieveFromPath(s"/$fileName").toMat(TestSink.probe)(Keep.both).run()
      probe.request(100).expectNextOrComplete()
      val expectedNumOfBytes = getLoremIpsum.getBytes().length
      result.futureValue shouldBe IOResult.createSuccessful(expectedNumOfBytes)
    }
  }

  "FtpBrowserSource & FtpIOSource" should {
    "work together reusing connection" in {
      val basePath = ""
      generateFiles(30, 10, basePath)
      Thread.sleep(10000)
      val handler = connect()
      val probe = listFiles(basePath, handler).map { file =>
        println(s"file: $file")
        val f = retrieveFromPath(file.path, handler).to(Sink.ignore).run() // ERROR!
        Await.result(f, 3.seconds)
      }.toMat(TestSink.probe)(Keep.right).run()
      probe.request(100).expectNextN(30)
      probe.expectComplete()
      disconnect(handler)
    }
  }
}
