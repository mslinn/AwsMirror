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

/**Taken from https://github.com/Bookish/domain-bus/blob/master/src/main/scala/ */

object MakeCommand {
  val template = """
_%s()
{
    local cur prev opts
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    opts="%s"

    if [[ ${cur} == -* ]] ; then
        COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
        return 0
    fi
}
complete -F _%s %s
alias %s='scala %s $*'
                 """

  def mkTemplate(name: String, className: String, opts: Seq[String]): String =
    template.format(name, opts mkString " ", name, name, name, className)

  private def getArgNames(className: String) = {
    val clazz = Class.forName(className + "$")
    val singleton = clazz.getField("MODULE$").get()
    val m = clazz.getMethod("argumentNames")

    (m invoke singleton).asInstanceOf[Array[String]] map ("--" + _)
  }

  def _main(args: Array[String]): Unit = {
    if (args == null || args.size != 2)
      return println("Usage: mkCommand <name> <class>")

    val Array(scriptName, className) = args
    val opts = getArgNames(className)

    val txt = mkTemplate(scriptName, className, opts)
    val tmpfile = java.io.File.createTempFile(scriptName, "", null)
    val writer = new java.io.FileWriter(tmpfile)
    writer write txt
    writer.close()

    println("# run this command in bash")
    println("source " + tmpfile.getAbsolutePath())
  }
}
