package org.nlogo.extensions.cf

import org.nlogo.api.{Argument, Command, Context, DefaultClassManager, PrimitiveManager, Reporter}
import org.nlogo.core.Syntax
import org.nlogo.core.Syntax._

case object Unpack extends Command {
  override def getSyntax: Syntax = commandSyntax(right = List(ListType, CommandType))
  override def perform(args: Array[Argument], context: Context): Unit =
    args(1).getCommand.perform(context, args(0).getList.toArray)
}

case object UnpackValue extends Reporter {
  override def getSyntax: Syntax = reporterSyntax(right = List(ListType, ReporterType), ret = WildcardType)
  override def report(args: Array[Argument], context: Context): AnyRef =
    args(1).getReporter.report(context, args(0).getList.toArray)
}

case object IfElse extends Command {
  override def getSyntax: Syntax = commandSyntax(
    right = List(
      BooleanType,
      BooleanType | CommandType | RepeatableType,
      CommandType
    ),
    defaultOption = Some(3),
    minimumOption = Some(2)
  )
  override def perform(args: Array[Argument], context: Context): Unit = {
    var i = 0
    while (i < args.length - 1) {
      if (args(i).getBooleanValue) {
        args(i + 1).getCommand.perform(context, Array.empty[AnyRef])
        return
      }
      i += 2
    }
    if (i < args.length) args.last.getCommand.perform(context, Array.empty[AnyRef])
  }
}

case object IfElseValue extends Reporter {
  override def getSyntax: Syntax = reporterSyntax(
    right = List(
      BooleanType,
      BooleanType | ReporterType | RepeatableType,
      ReporterType,
      ReporterType
    ),
    defaultOption = Some(3),
    precedence = NormalPrecedence - 7,
    ret = WildcardType
  )

  override def report(args: Array[Argument], context: Context): AnyRef = {
    var i = 0
    while (i < args.length - 1) {
      if (args(i).getBooleanValue) {
        return args(i + 1).getReporter.report(context, Array.empty[AnyRef])
      }
      i += 2
    }
    args.last.getReporter.report(context, Array.empty[AnyRef])
  }
}

class CFExtension extends DefaultClassManager {
  override def load(primManager: PrimitiveManager): Unit = {
    val add = primManager.addPrimitive _
    add("unpack", Unpack)
    add("unpack-value", UnpackValue)

    add("ifelse", IfElse)
    add("ifelse-value", IfElseValue)
  }
}
