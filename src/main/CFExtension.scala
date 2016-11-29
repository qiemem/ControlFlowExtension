package org.nlogo.extensions.cf

import org.nlogo.core.{LogoList, Let}
import org.nlogo.api.{ AnonymousCommand, Argument, Command, Context, ExtensionException, PrimitiveManager, DefaultClassManager, Reporter, AnonymousReporter, TypeNames => ApiTypeNames }
import org.nlogo.core.Syntax._
import org.nlogo.nvm

object Caster {
  type JBoolean = java.lang.Boolean

  def toReporter(x: AnyRef): AnonymousReporter = cast(x, classOf[AnonymousReporter], ReporterType)
  def toCommand(x: AnyRef): AnonymousCommand = cast(x, classOf[AnonymousCommand], CommandType)
  def toBoolean(x: AnyRef): JBoolean = cast(x, classOf[JBoolean], BooleanType)
  def toList(x: AnyRef): LogoList = cast(x, classOf[LogoList], ListType)

  def cast[T](x: AnyRef, t: Class[T], typeNum: Int): T =
    if (t.isInstance(x))
      t.cast(x)
    else
      throw new ExtensionException(
        "Expected " + ApiTypeNames.aName(typeNum) + " but got the " + ApiTypeNames.name(x) + " " + x + " instead.")
}

class RepTask(ctx: Context, arity: Int, fn: (Context, Array[AnyRef]) => AnyRef)
extends nvm.AnonymousReporter(
    new nvm.Reporter{override def report(ctx: nvm.Context): AnyRef = null},
    Array.fill(arity){new Let}, List(), Array()) {

  val ws = ctx.asInstanceOf[nvm.ExtensionContext].workspace
  override def report(ctx: Context, args: Array[AnyRef]): AnyRef = fn(ctx, args)
  override def report(ctx: nvm.Context, args: Array[AnyRef]): AnyRef = report(new nvm.ExtensionContext(ws, ctx), args)
}

trait Runner {
  import Caster._
  def predicate(task: AnyRef, context: Context, args: AnyRef*): JBoolean = toBoolean(reporter(task, context, args: _*))
  def reporter(task: AnyRef, context: Context, args: AnyRef*): AnyRef = toReporter(task).report(context, args.toArray)
  def command(task: AnyRef, context: Context, args: AnyRef*): Unit = toCommand(task).perform(context, args.toArray)
  def find(list: LogoList, context: Context, predArgs: AnyRef*): Option[AnyRef] =
    list.toVector.map(l => Caster.toList(l).toVector).find(l => predicate(l.head, context, predArgs: _*)).map(_.last)
  def notFoundRep = throw new ExtensionException(
    "No true condition found. If you wish default to a value instead of erroring, use `cf:else [ <default-value> ]`."
  )
  def notFoundCmd = throw new ExtensionException(
    "No true condition found. If you wish to do nothing instead of erroring, use `cf:else []`."
  )
}

case object Case extends Reporter {
  override def getSyntax = reporterSyntax(right = List(ReporterType, CommandType | ReporterType, ListType), ret = ListType)
  override def report(args: Array[Argument], context: Context) =
    args(2).getList.fput(LogoList(args(0).getReporter, args(1).get))
}

case object Else extends Reporter {
  def trueTask(ctx: Context) = new RepTask(ctx, 0, (c, args) => Boolean.box(true))

  override def getSyntax = reporterSyntax(right = List(CommandType | ReporterType), ret = ListType)
  override def report(args: Array[Argument], ctx: Context) =
    LogoList(LogoList(trueTask(ctx), args(0).get))
}

case object CaseIs extends Reporter with Runner {
  override def getSyntax =
    reporterSyntax(right = List(ReporterType, WildcardType, CommandType | ReporterType, ListType), ret = ListType)
  override def report(args: Array[Argument], ctx: Context) = {
    args(3).getList fput LogoList(
      new RepTask(ctx, 1, (c, xs) => predicate(args(0).getReporter, c, xs(0), args(1).get)),
      args(2).get
    )
  }
}

case object Cond extends Command with Runner {
  override def getSyntax = commandSyntax(right = List(ListType))
  override def perform(args: Array[Argument], ctx: Context): Unit =
    command(find(args(0).getList, ctx).getOrElse(notFoundCmd), ctx)
}

case object CondValue extends Reporter with Runner {
  override def getSyntax = reporterSyntax(right = List(ListType), ret = WildcardType)
  override def report(args: Array[Argument], ctx: Context) =
    reporter(find(args(0).getList, ctx).getOrElse(notFoundRep), ctx)
}

case object Match extends Command with Runner {
  override def getSyntax = commandSyntax(right = List(WildcardType, ListType))
  override def perform(args: Array[Argument], ctx: Context): Unit =
    command(find(args(1).getList, ctx, args(0).get).getOrElse(notFoundCmd), ctx, args(0).get)
}

case object MatchValue extends Reporter with Runner {
  override def getSyntax = reporterSyntax(left = WildcardType, right = List(ListType), ret = WildcardType,
    precedence = NormalPrecedence + 2, isRightAssociative = false)
  override def report(args: Array[Argument], ctx: Context): AnyRef =
    reporter(find(args(1).getList, ctx, args(0).get).getOrElse(notFoundRep), ctx, args(0).get)
}

case object Apply extends Command {
  override def getSyntax = commandSyntax(right = List(CommandType, ListType))
  override def perform(args: Array[Argument], context: Context) =
    args(0).getCommand.perform(context, args(1).getList.toArray)
}

case object ApplyValue extends Reporter {
  override def getSyntax = reporterSyntax(right = List(ReporterType, ListType), ret = WildcardType)
  override def report(args: Array[Argument], context: Context): AnyRef =
    args(0).getReporter.report(context, args(1).getList.toArray)
}

case object Unpack extends Command {
  override def getSyntax = commandSyntax(right = List(ListType, CommandType))
  override def perform(args: Array[Argument], context: Context) =
    args(1).getCommand.perform(context, args(0).getList.toArray)
}

case object UnpackValue extends Reporter {
  override def getSyntax = reporterSyntax(right = List(ListType, ReporterType), ret = WildcardType)
  override def report(args: Array[Argument], context: Context): AnyRef =
    args(1).getReporter.report(context, args(0).getList.toArray)
}

class CFExtension extends DefaultClassManager {
  override def load(primManager: PrimitiveManager) = {
    val add = primManager.addPrimitive _
    add("case", Case)
    add("else", Else)
    add("case-is", CaseIs)
    add("when", Cond)
    add("select", CondValue)
    add("match", Match)
    add("matching", MatchValue)

    add("apply", Apply)
    add("apply-value", ApplyValue)
    add("unpack", Unpack)
    add("unpack-value", UnpackValue)
  }
}
