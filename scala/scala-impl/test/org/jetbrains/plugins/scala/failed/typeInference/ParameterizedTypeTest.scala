package org.jetbrains.plugins.scala.failed.typeInference

import org.jetbrains.plugins.scala.PerfCycleTests
import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestAdapter
import org.junit.experimental.categories.Category

/**
  * @author Roman.Shein
  * @since 28.03.2016.
  */
@Category(Array(classOf[PerfCycleTests]))
class ParameterizedTypeTest extends ScalaLightCodeInsightFixtureTestAdapter {

  override protected def shouldPass: Boolean = false

  def testSCL9014() = {
    val text =
      """
        |import scala.util.{Success, Try}
        |class Foo[A, In <: Try[A]] {
        |    def takeA(a: A) = a
        |    def takeIn(in: In) = {
        |      in match {
        |        case Success(a) ⇒ takeA(a) // cannot infer type
        |      }
        |    }
        |  }
      """.stripMargin
    checkTextHasNoErrors(text)
  }

  def testSCL10118() = {
    val text =
      """
        |object SCL10118{
        |
        |  trait Test[A]
        |
        |  case class First(i: Int) extends Test[String]
        |
        |  case class Second(s: String) extends Test[Int]
        |
        |  def run[A](t: Test[A]): A = t match {
        |    case First(i) => i.toString
        |    case Second(s) => s.length
        |  }
        |}
      """.stripMargin
    checkTextHasNoErrors(text)
  }

  def testSCL8031(): Unit = {
    checkTextHasNoErrors(
      """
         |import One.HList.:::
         |
         |object One extends App{
         |
         |  trait Fold[T, V] {
         |    type Apply[N <: T, Acc <: V] <: V
         |    def apply[N <: T, Acc <: V](n: N, acc: Acc): Apply[N, Acc]
         |  }
         |
         |  sealed trait HList {
         |    type Foldr[V, F <: Fold[Any, V], I <: V] <: V
         |    def foldr[V, F <: Fold[Any, V], I <: V](f: F, i: I): Foldr[V, F, I]
         |  }
         |
         |  final case class HCons[H, T <: HList](head: H, tail: T) extends HList {
         |    def ::[E](v: E) = HCons(v, this)
         |    type Foldr[V, F <: Fold[Any, V], I <: V] = F#Apply[H, tail.Foldr[V, F, I]]
         |    def foldr[V, F <: Fold[Any, V], I <: V](f: F, i: I): Foldr[V, F, I] =
         |      f(head, tail.foldr[V, F, I](f, i))
         |  }
         |
         |  object HList {
         |    type ::[H, T <: HList] = HCons[H, T]
         |    val :: = HCons
         |    type :::[A <: HList, B <: HList] = A#Foldr[HList, FoldHCons.type, B]
         |    implicit def concat[B <: HList](b: B): Concat[B] =
         |      new Concat[B] {
         |        def :::[A <: HList](a: A): A#Foldr[HList, FoldHCons.type, B] =
         |          a.foldr[HList, FoldHCons.type, B](FoldHCons, b)
         |      }
         |
         |    object FoldHCons extends Fold[Any, HList] {
         |      type Apply[N <: Any, H <: HList] = N :: H
         |      def apply[A, B <: HList](a: A, b: B) = HCons(a, b)
         |    }
         |  }
         |
         |  sealed trait Concat[B <: HList] { def :::[A <: HList](a: A): A ::: B }
         |}
      """.stripMargin.trim
    )
  }

  def testSCL12656() = {
    checkTextHasNoErrors(
      """import scala.concurrent.{ExecutionContext, Future}
        |import scala.util.Success
        |
        |object TestCase {
        |  def f: Future[Any] = null
        |
        |  implicit class MyFuture[T](val f: Future[T]) {
        |    def awaitAndDo[U <: T](func: U => String)(implicit ec: ExecutionContext): String = {
        |      f onComplete {
        |        case Success(value) => return func(value.asInstanceOf[U])
        |        case _ => Unit
        |      }
        |      "bar"
        |    }
        |  }
        |
        |  private def foo = {
        |    implicit val ec: ExecutionContext = null
        |    var baz: String = f awaitAndDo[Option[String]] {
        |      case Some(s) => s
        |      case None => "oups"
        |    }
        |  }
        |}""".stripMargin
    )
  }

  def testSCL12908() = {
    val text =
      """
        |def check[T](array: Array[T]): Unit = {
        |    array match {
        |      case bytes: Array[Byte] =>
        |        println("Got bytes!")
        |      case _ =>
        |        println("Got something else than bytes!")
        |    }
        |  }
      """.stripMargin
    checkTextHasNoErrors(text)
  }

  def testSCL13042() = {
    val text =
      """
        |def f[R[_], T](fun: String => R[T]): String => R[T] = fun
        |val result = f(str => Option(str))
      """.stripMargin
    checkTextHasNoErrors(text)
  }

  def testSCL13746() = {
    val text =
      """
        |import scala.annotation.tailrec
        |
        |trait IO[A] {
        |  def flatMap[B](f: A => IO[B]): IO[B] = FlatMap(this, f)
        |  def map[B](f: A => B): IO[B] = flatMap(f andThen (Return(_)))
        |}
        |
        |case class Return[A](a: A) extends IO[A]
        |case class Suspend[A](r: () => A) extends IO[A]
        |case class FlatMap[A, B](s: IO[A], k: A => IO[B]) extends IO[B]
        |
        |object IO {
        |  @tailrec
        |  def run[A](io: IO[A]): A = io match {
        |    case Return(a) => a
        |    case Suspend(r) => r()
        |    case FlatMap(x, f) => x match {
        |      case Return(a) => run(f(a))
        |      case Suspend(r) => run(f(r()))
        |      case FlatMap(y, g) => run(y flatMap (a => g(a) flatMap f))
        |    }
        |  }
        |}
      """.stripMargin
    checkTextHasNoErrors(text)
  }

  def testSCL14179() = {
    val text =
      """
        |trait Functor[F[_]] {
        |  def map[A, B](fa: F[A])(f: A => B): F[B]
        |}
        |trait Applicative[F[_]] extends Functor[F] { self =>
        |  def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] =
        |    apply(map(fa)(f.curried))(fb)
        |  def apply[A, B](fab: F[A => B])(fa: F[A]): F[B] =
        |    map2(fab, fa)(_(_))
        |  def map[A, B](fa: F[A])(f: A => B): F[B] =
        |    apply(unit(f))(fa)
        |  def unit[A](a: => A): F[A]
        |}
        |trait Traverse[F[_]] extends Functor[F] {
        |  def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]] =
        |    sequence(map(fa)(f))
        |  def sequence[G[_]: Applicative, A](fma: F[G[A]]): G[F[A]] =
        |    traverse(fma)(ma => ma)
        |}
      """.stripMargin
    checkTextHasNoErrors(text)
  }
}
