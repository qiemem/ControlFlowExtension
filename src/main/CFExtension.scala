package org.nlogo.extensions.cf

import org.nlogo.api.{AnonymousCommand, AnonymousReporter, Argument, Command, Context, DefaultClassManager, ExtensionException, PrimitiveManager, Reporter, TypeNames => ApiTypeNames}
import org.nlogo.core.Syntax._
import org.nlogo.core.{LogoList, Syntax}
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

case class RepTask(arity: Int, fn: (Context, Array[AnyRef]) => AnyRef, source: String) extends AnonymousReporter {
  override def report(c: Context, args: Array[AnyRef]): AnyRef = fn(c, args)

  override val syntax: Syntax = Syntax.reporterSyntax(
    ret = Syntax.WildcardType,
    defaultOption = Some(arity),
    minimumOption = Some(arity),
    right = nvm.AnonymousProcedure.rightArgs
  )
}

trait Runner {
  import Caster._
  def predicate(task: AnyRef, context: Context, args: Array[AnyRef]): JBoolean =
    toBoolean(reporter(task, context, args))

  def reporter(task: AnyRef, context: Context): AnyRef =
    reporter(task, context, Array.empty[AnyRef])
  def reporter(task: AnyRef, context: Context, args: Array[AnyRef]): AnyRef =
    toReporter(task).report(context, args)

  def command(task: AnyRef, context: Context): Unit =
    command(task, context, Array.empty[AnyRef])
  def command(task: AnyRef, context: Context, args: Array[AnyRef]): Unit =
    toCommand(task).perform(context, args)

  def find(list: LogoList, context: Context): Option[AnyRef] =
    find(list, context, Array.empty[AnyRef])
  def find(list: LogoList, context: Context, predArgs: Array[AnyRef]): Option[AnyRef] =
    list.find { l =>
      predicate(Caster.toList(l)(0), context, predArgs)
    }.map {
      case l: LogoList => l(l.size - 1)
    }

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
  val trueTask = RepTask(0, (_, _) => Boolean.box(true), "[ -> true ]")

  override def getSyntax = reporterSyntax(right = List(CommandType | ReporterType), ret = ListType)
  override def report(args: Array[Argument], ctx: Context) =
    LogoList(LogoList(trueTask, args(0).get))
}

case object CaseIs extends Reporter with Runner {
  override def getSyntax =
    reporterSyntax(right = List(ReporterType, WildcardType, CommandType | ReporterType, ListType), ret = ListType)
  override def report(args: Array[Argument], ctx: Context) = {
    args(3).getList fput LogoList(
      RepTask(1,
        (c: Context, xs: Array[AnyRef]) =>
          predicate(args(0).getReporter, c, Array(xs(0), args(1).get)),
        "cf:case-is"),
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
  override def perform(args: Array[Argument], ctx: Context): Unit = {
    val actuals = Array(args(0).get)
    command(find(args(1).getList, ctx, actuals).getOrElse(notFoundCmd), ctx, actuals)
  }
}

case object MatchValue extends Reporter with Runner {
  override def getSyntax = reporterSyntax(left = WildcardType, right = List(ListType), ret = WildcardType,
    precedence = NormalPrecedence + 2, isRightAssociative = false)
  override def report(args: Array[Argument], ctx: Context): AnyRef = {
    val actuals = Array(args(0).get)
    reporter(find(args(1).getList, ctx, actuals).getOrElse(notFoundRep), ctx, actuals)
  }
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
