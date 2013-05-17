package scala.slick.yy

import scala.{ Int => SInt }
import scala.language.implicitConversions
import scala.slick.lifted.Case
import scala.slick.ast.BaseTypedType
import scala.slick.lifted.AbstractTable
import scala.slick.driver.H2Driver
import scala.slick.jdbc.JdbcBackend
import scala.slick.driver.JdbcDriver
import scala.slick.SlickException
import scala.slick.profile.BasicDriver
import scala.slick.driver.JdbcProfile

trait YYSlickCake extends YYSlickLowPriorityImplicits {
  type CakeRep[T] = YYRep[T]
  type Tuple2[T1, T2] = YYProjection2[T1, T2]
  type Column[T] = YYColumn[T]
  type Table[T] = YYTable[T]
  type Query[T] = YYQuery[T]
  type Int = YYColumn[SInt]
  type Long = YYColumn[scala.Long]
  type Double = YYColumn[scala.Double]
  type String = YYColumn[Predef.String]
  type Boolean = YYColumn[scala.Boolean]
  type TableRow = scala.slick.yy.YYTableRow
  type YYTableRow = Table[TableRow] // w/o it: "type YYTableRow is not a member of CAKE"
  type ColumnOps[T] = YYColumn[T]
  type Invoker[T] = scala.slick.yy.Shallow.Invoker[T]

  //order stuff
  type Ordering[T] = YYOrdering[T]
  val Ordering = YYOrdering
  val String = YYOrdering.String
  val Int = YYOrdering.Int

  implicit def fixClosureContraVariance[T, U <: YYRep[T], S](x: U => S) =
    x.asInstanceOf[YYRep[T] => S]

  object Queryable {
    def apply[T](implicit t: YYTable[T]): Query[T] = YYQuery.apply(t)
  }

  object Query {
    def apply[T](v: YYRep[T]): YYQuery[T] = YYQuery.apply(v)
    def ofTable[T](t: YYTable[T]): YYQuery[T] = YYQuery.apply(t)
  }

  object Shallow {
    object ColumnOps {
      def apply[T](value: YYColumn[T]): YYColumn[T] = value
    }
    def stringWrapper(value: String): String = value
  }

  def intWrapper(value: Int): Int = value

  //  def augmentString(value: String): String = value

  def __ifThenElse[T: BaseTypedType](c: => Boolean, t: Column[T], e: Column[T]): Column[T] = {
    val condition = Case.If(c.underlying)
    val _then = condition.Then[T](t.underlying)
    val _else = _then.Else(e.underlying)
    YYColumn(_else)
  }

  def __equals[T](t: Column[T], e: Column[T]) = t === e

  object Tuple2 {
    def apply[T1, T2](_1: Column[T1], _2: Column[T2]) = YYProjection.fromYY(_1, _2)
  }

  // testing stuffs

  type TableARow = scala.slick.yy.YYTableARow
  type YYTableARow = Table[TableARow] // w/o it: "type YYTableARow is not a member of CAKE"

  implicit def convertYYTableARow(t: Table[TableARow]) = new TestTable.YYTableA(t.underlying.asInstanceOf[TestTable.TableA])
  implicit object implicitYYTableA extends TestTable.YYTableA(TestTable.TableA)

  object Table {
    def test(): Table[TableRow] = TestTable.YYTableA.asInstanceOf[Table[TableRow]]
    def test2(): Table[TableARow] = TestTable.YYTableA
    def getTable[S](implicit mapping: Table[S]): Table[S] = mapping
  }

}

trait YYSlickLowPriorityImplicits {
  // These two implicits are needed for the cake to be type chacked!

  // Type of this one is JdbcProfile and not JdbcDriver in order to make it lower priority in comparison with the implicit driver which will
  // provided by the user. If type of this one is JdbcDriver, we would get 'ambiguous implicit' error. 
  implicit def dummyDriver: JdbcProfile = throw new SlickException("You forgot to provide appropriate implicit jdbc driver for YY block!")
  // The reason of generality of this type is the same as the above one.
  implicit def dummySession: JdbcBackend#Session = throw new SlickException("You forgot to provide implicit session for YY block!")
}

object TestTable {
  import scala.slick.driver.H2Driver.simple
  import scala.slick.driver.H2Driver.Implicit._

  class TableA extends simple.Table[YYTableARow]("TABLE_A") {
    def id = column[SInt]("A_ID")
    def grade = column[SInt]("A_GRADE")
    def * = id ~ grade <> (YYTableARow, YYTableARow.unapply _)
  }
  object TableA extends TableA

  implicit def convertTuple2ToTableARow(tup2: (scala.Int, scala.Int)): YYTableARow =
    YYTableARow(tup2._1, tup2._2)

  class YYTableA(val table: TableA) extends YYTable[YYTableARow] {

    def id = YYColumn(table.id)
    def grade = YYColumn(table.grade)
    override def toString = "YYTableA"
  }

  object YYTableA extends YYTableA(TableA)

  def underlying[E](x: YYRep[E]): TableA = x.underlying.asInstanceOf[TableA]
}

case class YYTableARow(val id: SInt, val grade: SInt) extends YYTableRow
