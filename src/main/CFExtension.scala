package org.nlogo.extensions.cf

import org.nlogo.api._
import org.nlogo.api.Syntax._

object Caster {
  def toReporter(x: AnyRef): ReporterTask = x match {
    case t: ReporterTask => t
    case _ => throw new ExtensionException("Expected a reporter task but got " + x)
  }
  def toCommand(x: AnyRef): CommandTask = x match {
    case t: CommandTask => t
    case _ => throw new ExtensionException("Expected a command task but got " + x)
  }
  def toBoolean(x: AnyRef): Boolean = x match {
    case b: java.lang.Boolean => b
    case _ => throw new ExtensionException("Expected a boolean but go " + x)
  }
}

case object Case extends DefaultReporter {
  override def getSyntax = reporterSyntax(Array(ReporterTaskType, CommandTaskType | ReporterTaskType), ListType)
  override def report(args: Array[Argument], context: Context) =
    LogoList(args(0).getReporterTask, args(1).getCommandTask)
}

case object Else extends DefaultReporter {
  override def getSyntax = reporterSyntax(Array(CommandTaskType), ListType)
  override def report(args: Array[Argument], context: Context) =
    LogoList(true: java.lang.Boolean, args(0).getCommandTask)

}

case object Cond extends DefaultCommand {
  override def getSyntax = commandSyntax(Array(ListType | RepeatableType))
  override def perform(args: Array[Argument], context: Context): Unit = {
    args.map(_.getList.toList).find { _.head match {
      case b: java.lang.Boolean => b
      case t: ReporterTask => Caster.toBoolean(t.report(context, Array.empty[AnyRef]))
      case x => throw new ExtensionException("Expected a reporter task or boolean but go " + x)
    }}.foreach {
      l => Caster.toCommand(l.last).perform(context, Array.empty[AnyRef])
    }
  }
}

class CFExtension extends DefaultClassManager {
  override def load(primManager: PrimitiveManager) = {
    val add = primManager.addPrimitive _
    add("case", Case)
    add("cond", Cond)
    add("else", Else)
  }
}
