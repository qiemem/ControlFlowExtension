package org.nlogo.extensions.cf

import org.nlogo.api.{AnonymousCommand, AnonymousProcedure, AnonymousReporter, Argument, Command, Context, DefaultClassManager, ExtensionException, PrimitiveManager, Reporter, TypeNames => ApiTypeNames}
import org.nlogo.core.Syntax._
import org.nlogo.core.{ExtensionObject, LogoList, Syntax}
import org.nlogo.nvm

object Caster {
  type JBoolean = java.lang.Boolean

  def toReporter(x: AnyRef): AnonymousReporter = cast(x, classOf[AnonymousReporter], ReporterType)
  def toCommand(x: AnyRef): AnonymousCommand = cast(x, classOf[AnonymousCommand], CommandType)
  def toBoolean(x: AnyRef): JBoolean = cast(x, classOf[JBoolean], BooleanType)
  def toList(x: AnyRef): LogoList = cast(x, classOf[LogoList], ListType)
  def toReporterCase(x: AnyRef): ReporterCase = x match {
    case c: ReporterCase => c
    case _ => throw new ExtensionException(
      s"Expected a case pair but got the ${ApiTypeNames.name(x)} $x instead")
  }
  def toCommandCase(x: AnyRef): CommandCase = x match {
    case c: CommandCase => c
    case _ => throw new ExtensionException(
      s"Expected a case pair but got the ${ApiTypeNames.name(x)} $x instead")
  }

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

sealed trait Case[R] extends ExtensionObject {
  def apply(ctx: Context, args: Array[AnyRef]): R
  def apply(ctx: Context): R = apply(ctx, Array.empty[AnyRef])

  override val getExtensionName: String = "cf"

  override def recursivelyEqual(obj: AnyRef): Boolean = this == obj
}

sealed trait CommandCase extends Case[Unit] {
  override val getNLTypeName = "command-case"
}

case class CommandCasePair(condition: AnonymousReporter,
                           body: AnonymousCommand,
                           rest: CommandCase) extends CommandCase {
  override def apply(ctx: Context, args: Array[AnyRef]): Unit =
    if (Caster.toBoolean(condition.report(ctx, args)))
      body.perform(ctx, args)
    else
      rest(ctx, args)

  override def dump(readable: Boolean, exporting: Boolean, reference: Boolean): String =
    s"$condition $body ${rest.dump(readable, exporting, reference)}"
}

case class CommandElse(body: AnonymousCommand) extends CommandCase {
  override def apply(ctx: Context, args: Array[AnyRef]): Unit = body.perform(ctx, args)
  override def dump(readable: Boolean, exporting: Boolean, reference: Boolean): String =
    s"else $body"
}

sealed trait ReporterCase extends Case[AnyRef] {
  override val getNLTypeName = "reporter-case"
}

case class ReporterCasePair(condition: AnonymousReporter,
                        body: AnonymousReporter,
                        rest: ReporterCase) extends ReporterCase {
  override def apply(ctx: Context, args: Array[AnyRef]): AnyRef =
    if (Caster.toBoolean(condition.report(ctx, args)))
      body.report(ctx, args)
    else
      rest(ctx, args)

  override def dump(readable: Boolean, exporting: Boolean, reference: Boolean): String =
    s"$condition $body ${rest.dump(readable, exporting, reference)}"
}

case class ReporterElse(body: AnonymousReporter) extends ReporterCase {
  override def apply(ctx: Context, args: Array[AnyRef]): AnyRef = body.report(ctx, args)
  override def dump(readable: Boolean, exporting: Boolean, reference: Boolean): String =
    s"else $body"
}

case object CasePrim extends Reporter {
  override def getSyntax = reporterSyntax(
    right = List(ReporterType, CommandType | ReporterType, WildcardType),
    ret = WildcardType
  )
  override def report(args: Array[Argument], context: Context) = args(2).get match {
    case c: CommandCase => CommandCasePair(args(0).getReporter, args(1).getCommand, c)
    case r: ReporterCase => ReporterCasePair(args(0).getReporter, args(1).getReporter, r)
  }
}

case object ElsePrim extends Reporter {
  val trueTask = RepTask(0, (_, _) => Boolean.box(true), "[ -> true ]")

  override def getSyntax = reporterSyntax(
    right = List(CommandType | ReporterType),
    ret = WildcardType
  )
  override def report(args: Array[Argument], ctx: Context) = args(0).get match {
    case c: AnonymousCommand => CommandElse(c)
    case r: AnonymousReporter => ReporterElse(r)
  }
}

case object CaseIs extends Reporter with Runner {
  override def getSyntax =
    reporterSyntax(right = List(ReporterType, WildcardType, CommandType | ReporterType, ListType), ret = ListType)
  override def report(args: Array[Argument], ctx: Context) = {
    val condition = RepTask(1,
      (c: Context, xs: Array[AnyRef]) => predicate(args(0).getReporter, c, Array(xs(0), args(1).get)),
      "cf:case-is"
    )
    args(3).get match {
      case c: CommandCase => CommandCasePair(condition, args(2).getCommand, c)
      case r: ReporterCase => ReporterCasePair(condition, args(2).getReporter, r)
    }
  }
}

case object Cond extends Command with Runner {
  override def getSyntax = commandSyntax(right = List(WildcardType))
  override def perform(args: Array[Argument], ctx: Context): Unit =
    Caster.toCommandCase(args(0).get)(ctx)
}

case object CondValue extends Reporter with Runner {
  override def getSyntax = reporterSyntax(right = List(WildcardType), ret = WildcardType)
  override def report(args: Array[Argument], ctx: Context) =
    Caster.toReporterCase(args(0).get)(ctx)
}

case object Match extends Command with Runner {
  override def getSyntax = commandSyntax(right = List(WildcardType, WildcardType))
  override def perform(args: Array[Argument], ctx: Context): Unit =
    Caster.toCommandCase(args(1).get)(ctx, Array(args(0).get))
}

case object MatchValue extends Reporter with Runner {
  override def getSyntax = reporterSyntax(left = WildcardType, right = List(WildcardType), ret = WildcardType,
    precedence = NormalPrecedence + 2, isRightAssociative = false)
  override def report(args: Array[Argument], ctx: Context): AnyRef =
    Caster.toReporterCase(args(1).get)(ctx, Array(args(0).get))
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

case object RawCond extends Command {
  override def getSyntax = commandSyntax(
    right = List(Syntax.ReporterType | Syntax.CommandType | Syntax.RepeatableType)
  )
  override def perform(args: Array[Argument], context: Context): Unit = {
    var i = 0
    while (i < args.length) {
     if (Caster.toBoolean(args(i).getReporter.report(context, Array.empty[AnyRef]))) {
       args(i + 1).getCommand.perform(context, Array.empty[AnyRef])
       return
     }
      i += 2
    }
  }
}

case object RawCond2 extends Command {
  override def getSyntax = commandSyntax(
    right = List(Syntax.BooleanType | Syntax.CommandType | Syntax.RepeatableType)
  )
  override def perform(args: Array[Argument], context: Context): Unit = {
    var i = 0
    while (i < args.length) {
      if (args(i).getBooleanValue) {
        args(i + 1).getCommand.perform(context, Array.empty[AnyRef])
        return
      }
      i += 2
    }
  }
}

class CFExtension extends DefaultClassManager {
  override def load(primManager: PrimitiveManager) = {
    val add = primManager.addPrimitive _
    add("case", CasePrim)
    add("else", ElsePrim)
    add("case-is", CaseIs)
    add("when", Cond)
    add("select", CondValue)
    add("match", Match)
    add("matching", MatchValue)

    add("apply", Apply)
    add("apply-value", ApplyValue)
    add("unpack", Unpack)
    add("unpack-value", UnpackValue)

    add("cond", RawCond)
    add("cond2", RawCond2)
  }
}
