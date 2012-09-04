/*
 * Copyright 2012 Bookish, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License. */

package optional

import scala.collection._
import mutable.HashSet

/**Taken from https://github.com/Bookish/domain-bus/blob/master/src/main/scala/ */

case class Options(
                    options: Map[String, String],
                    args: List[String],
                    rawArgs: List[String]
                    )

case class ArgInfo(short: Char, long: String, isSwitch: Boolean, help: String)

object Options {
  private val ShortOption = """-(\w)""".r
  private val ShortSquashedOption = """-([^-\s]\w+)""".r
  private val LongOption = """--(\w+)""".r
  private val OptionTerminator = "--"
  private val True = "true";

  /**
   * Take a list of string arguments and parse them into options.
   * Currently the dumbest option parser in the entire world, but
   * oh well.
   */
  def parse(argInfos: HashSet[ArgInfo], args: String*): Options = {
    import mutable._;
    val optionsStack = new ArrayStack[String];
    val options = new OpenHashMap[String, String];
    val arguments = new ArrayBuffer[String];

    def addSwitch(c: Char) =
      options(c.toString) = True

    def isSwitch(c: Char) =
      argInfos exists {
        case ArgInfo(`c`, _, true, _) => true
        case _ => false
      }

    def addOption(name: String) = {
      if (optionsStack.isEmpty) options(name) = True;
      else {
        val next = optionsStack.pop;
        next match {
          case ShortOption(_) | ShortSquashedOption(_) | LongOption(_) | OptionTerminator =>
            optionsStack.push(next);
            options(name) = True;
          case x => options(name) = x;
        }
      }
    }

    optionsStack ++= args.reverse;
    while (!optionsStack.isEmpty) {
      optionsStack.pop match {
        case ShortSquashedOption(xs) =>
          xs foreach addSwitch

        case ShortOption(name) =>
          val c = name(0)
          if (isSwitch(c)) addSwitch(c)
          else addOption(name)

        case LongOption(name) => addOption(name);
        case OptionTerminator => optionsStack.drain(arguments += _);
        case x => arguments += x;
      }
    }

    Options(options, arguments.toList, args.toList)
  }
}
