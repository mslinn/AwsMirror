/* Copyright 2012 Micronautics Research Corporation.
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

package com.micronautics.aws

import Main._
import Util._

class Empty(args: Array[String]) {
  if (!credentialPath.toFile.exists) {
    println(".aws file not found in %s\nUse 'auth add' subcommand to create".format(credentialPath.toAbsolutePath))
    System.exit(-1)
  }

  args.length match {
    case 0 =>
    // todo empty bucket specified in .s3 file in this directory or parent; error if no .s3 file
    // todo error if bucket does not exist

    case 1 =>
    // todo error if bucket does not exist
    // todo else empty bucket
    // todo if .s3 file exists in current directory or parent, tell user that they could run sync or upload commands

    case _ =>
      println("Error: Too many arguments provided for empty")
      help
  }

}
