package org.jetbrains.plugins.scala
package lang
package psi
package stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs._
import com.intellij.util.ArrayUtil
import com.intellij.util.io.StringRef
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil

/**
  * @author adkozlov
  */
package object elements {

  implicit class StubInputStreamExt(val dataStream: StubInputStream) extends AnyVal {
    def readOptionName: Option[StringRef] = {
      val isDefined = dataStream.readBoolean
      if (isDefined) Some(dataStream.readName) else None
    }

    def readNames: Array[StringRef] = {
      val length = dataStream.readInt
      (0 until length).map { _ =>
        dataStream.readName
      }.toArray
    }
  }

  implicit class StubOutputStreamExt(val dataStream: StubOutputStream) extends AnyVal {
    def writeOptionName(maybeName: Option[String]): Unit = {
      dataStream.writeBoolean(maybeName.isDefined)
      maybeName.foreach {
        dataStream.writeName
      }
    }

    def writeNames(names: Array[String]): Unit = {
      dataStream.writeInt(names.length)
      names.foreach {
        dataStream.writeName
      }
    }
  }

  implicit class MaybeStringRefExt(val maybeStringRef: Option[StringRef]) extends AnyVal {
    def asString: Option[String] = maybeStringRef.map {
      StringRef.toString
    }.filter {
      _.nonEmpty
    }
  }

  implicit class MaybeStringExt(val maybeString: Option[String]) extends AnyVal {
    def asReference: Option[StringRef] = maybeString.filter {
      _.nonEmpty
    }.map {
      StringRef.fromString
    }
  }

  implicit class StringRefArrayExt(val stringRefs: Array[StringRef]) extends AnyVal {
    def asStrings: Array[String] = stringRefs.map {
      StringRef.toString
    }.filter {
      _.nonEmpty
    } match {
      case Array() => ArrayUtil.EMPTY_STRING_ARRAY
      case array => array
    }
  }

  implicit class PsiElementsExt(val elements: Seq[PsiElement]) extends AnyVal {

    def asReferences(transformText: String => String = identity): Array[StringRef] =
      if (elements.nonEmpty) elements
        .map(_.getText)
        .map(transformText)
        .map(StringRef.fromString).toArray
      else StringRef.EMPTY_ARRAY
  }

  implicit class StringsExt(val strings: Iterable[String]) extends AnyVal {

    def asReferences: Array[StringRef] = {
      val result = strings.filter(_.nonEmpty)

      if (result.nonEmpty) result.map(StringRef.fromString).toArray
      else StringRef.EMPTY_ARRAY
    }
  }

  implicit class IndexSinkExt(val sink: IndexSink) extends AnyVal {

    def occurrences[T <: PsiElement](key: StubIndexKey[String, T],
                                     names: String*): Unit = for {
      name <- names
      if name != null

      cleanName = ScalaNamesUtil.cleanFqn(name)
      if cleanName.nonEmpty
    } sink.occurrence(key, cleanName)
  }
}
