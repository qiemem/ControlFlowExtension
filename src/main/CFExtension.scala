package org.nlogo.extensions.cf

import org.nlogo.api.ReporterTask
import org.nlogo.api._
import org.nlogo.api.Syntax._

object Caster {
  type JBoolean = java.lang.Boolean

  def toReporter(x: AnyRef): ReporterTask = cast(x, classOf[ReporterTask], ReporterTaskType)
  def toCommand(x: AnyRef): CommandTask = cast(x, classOf[CommandTask], CommandTaskType)
  def toBoolean(x: AnyRef): JBoolean = cast(x, classOf[JBoolean], BooleanType)
  def toList(x: AnyRef): LogoList = cast(x, classOf[LogoList], ListType)

  def cast[T](x: AnyRef, t: Class[T], typeNum: Int): T =
    if (t.isInstance(x))
      t.cast(x)
    else
      throw new ExtensionException(
        "Expected " + TypeNames.aName(typeNum) + " but got the " + TypeNames.name(x) + " " + x + " instead.")
}

trait Runner {
  import Caster._
  def predicate(task: AnyRef, context: Context, args: AnyRef*): JBoolean = toBoolean(reporter(task, context, args: _*))
  def reporter(task: AnyRef, context: Context, args: AnyRef*): AnyRef = toReporter(task).report(context, args.toArray)
  def command(task: AnyRef, context: Context, args: AnyRef*): Unit = toCommand(task).perform(context, args.toArray)
  def find(list: LogoList, context: Context, predArgs: AnyRef*): Option[AnyRef] =
    list.toVector.map(l => Caster.toList(l).toVector).find(l => predicate(l.head, context, predArgs: _*)).map(_.last)
  def notFound = throw new ExtensionException("Needed at least one true condition, but all were false.")
}

case object Case extends DefaultReporter {
  override def getSyntax = reporterSyntax(Array(ReporterTaskType, CommandTaskType | ReporterTaskType, ListType), ListType)
  override def report(args: Array[Argument], context: Context) =
    args(2).getList.fput(LogoList(args(0).getReporterTask, args(1).get))
}

case object Else extends DefaultReporter {
  val trueTask = new ReporterTask {
    def report(c: Context, args: Array[AnyRef]): AnyRef = Boolean.box(true)
  }

  override def getSyntax = reporterSyntax(Array(CommandTaskType | ReporterTaskType), ListType)
  override def report(args: Array[Argument], context: Context) =
    LogoList(LogoList(trueTask, args(0).get))
}

case object CaseIs extends DefaultReporter with Runner {
  override def getSyntax =
    reporterSyntax(Array(ReporterTaskType, WildcardType, CommandTaskType | ReporterTaskType, ListType), ListType)
  override def report(isArgs: Array[Argument], context: Context) = {
    val pair = LogoList(new ReporterTask {
      def report(c: Context, args: Array[AnyRef]) = predicate(isArgs(0).getReporterTask, c, args(0), isArgs(1).get)
    }, isArgs(2).get)
    isArgs(3).getList.fput(pair)
  }
}

case object Cond extends DefaultCommand with Runner {
  override def getSyntax = commandSyntax(Array(ListType))
  override def perform(args: Array[Argument], ctx: Context): Unit =
    find(args(0).getList, ctx).foreach(body => command(body, ctx))
}

case object CondValue extends DefaultReporter with Runner {
  override def getSyntax = reporterSyntax(Array(ListType), WildcardType)
  override def report(args: Array[Argument], ctx: Context) =
    find(args(0).getList, ctx).map(body => reporter(body, ctx)).getOrElse(notFound)
}

case object Match extends DefaultCommand with Runner {
  override def getSyntax = commandSyntax(Array(WildcardType, ListType))
  override def perform(args: Array[Argument], ctx: Context): Unit =
    find(args(1).getList, ctx, args(0).get).foreach(body => command(body, ctx))
}

case object MatchValue extends DefaultReporter with Runner {
  override def getSyntax = reporterSyntax(Array(WildcardType, ListType | RepeatableType), WildcardType)
  override def report(args: Array[Argument], ctx: Context): AnyRef =
    find(args(1).getList, ctx, args(0).get).map(body => reporter(body, ctx)).getOrElse(notFound)
}

case object Apply extends DefaultCommand {
  override def getSyntax = commandSyntax(Array(CommandTaskType, ListType))
  override def perform(args: Array[Argument], context: Context) =
    args(0).getCommandTask.perform(context, args(1).getList.toArray)
}

case object ApplyValue extends DefaultReporter {
  override def getSyntax = reporterSyntax(Array(ReporterTaskType, ListType), WildcardType)
  override def report(args: Array[Argument], context: Context): AnyRef =
    args(0).getReporterTask.report(context, args(1).getList.toArray)
}

case object Unpack extends DefaultCommand {
  override def getSyntax = commandSyntax(Array(ListType, CommandTaskType))
  override def perform(args: Array[Argument], context: Context) =
    args(1).getCommandTask.perform(context, args(0).getList.toArray)
}

case object UnpackValue extends DefaultReporter {
  override def getSyntax = reporterSyntax(Array(ListType, ReporterTaskType), WildcardType)
  override def report(args: Array[Argument], context: Context): AnyRef =
    args(1).getReporterTask.report(context, args(0).getList.toArray)
}

class CFExtension extends DefaultClassManager {
  override def load(primManager: PrimitiveManager) = {
    val add = primManager.addPrimitive _
    add("case", Case)
    add("else", Else)
    add("=", Equals)
    add("is", CaseIs)
    add("cond", Cond)
    add("cond-value", CondValue)
    add("match", Match)
    add("match-value", MatchValue)

    add("apply", Apply)
    add("apply-value", ApplyValue)
    add("unpack", Unpack)
    add("unpack-value", UnpackValue)
  }
}
