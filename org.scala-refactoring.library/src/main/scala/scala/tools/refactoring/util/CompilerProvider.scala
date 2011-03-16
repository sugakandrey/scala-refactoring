/*
 * Copyright 2005-2010 LAMP/EPFL
 */

package scala.tools.refactoring
package util

import java.io.File.pathSeparator
import tools.nsc.util.Position
import tools.nsc.io.AbstractFile
import tools.nsc.Settings
import tools.nsc.interactive.Global
import tools.nsc.reporters.ConsoleReporter
import tools.nsc.util.BatchSourceFile
import tools.nsc.util.SourceFile

class CompilerInstance {
  
  def additionalClassPathEntry: Option[String] = None
  
  lazy val compiler = {
    
    val settings = new Settings
    
    val scalaObjectSource = Class.forName("scala.ScalaObject").getProtectionDomain.getCodeSource
      
    // is null in Eclipse/OSGI but luckily we don't need it there
    if(scalaObjectSource != null) {
      val compilerPath = Class.forName("scala.tools.nsc.Interpreter").getProtectionDomain.getCodeSource.getLocation
      val libPath = scalaObjectSource.getLocation          
      val pathList = List(compilerPath,libPath)
      val origBootclasspath = settings.bootclasspath.value
      settings.bootclasspath.value = ((origBootclasspath :: pathList) ::: additionalClassPathEntry.toList) mkString pathSeparator
    }
    
    val compiler = new Global(settings, new ConsoleReporter(settings) {
      override def printMessage(pos: Position, msg: String) {
        //throw new Exception(pos.source.file.name + pos.show + msg)
      }
    })
  
    new compiler.Run  
    
    compiler
  }
}

trait TreeCreationMethods {
  
  private def isScala(version: String) = scala.util.Properties.versionString.contains(version)
    
  val global: scala.tools.nsc.interactive.Global
  
  val randomFileName = {
    val r = new java.util.Random
    () => "file"+ r.nextInt
  }
    
  def treeFrom(src: String): global.Tree = {
    val file = new BatchSourceFile(randomFileName(), src)
    treeFrom(file, true)
  }
  
  def treeFrom(file: SourceFile, forceReload: Boolean): global.Tree = {
    treeFromCompiler28(file, forceReload)
  }
  
  private def treeFromCompiler28(file: SourceFile, forceReload: Boolean) = {
    
    type Scala28Compiler = {
      def typedTree(file: SourceFile, forceReload: Boolean): global.Tree 
    }
    
    val newCompiler = global.asInstanceOf[Scala28Compiler]
    
    newCompiler.typedTree(file, forceReload)
  }

  def treesFrom(sources: List[SourceFile], forceReload: Boolean): List[global.Tree] =
    sources map ( treeFrom(_, forceReload) )
  
  /**
   * Add a source file with the given name and content to this compiler instance.
   * 
   * @param name the name of the file; adding different files with the same name can lead to problems
   */
  def addToCompiler(name: String, src: String): AbstractFile = {
    val file = new BatchSourceFile(name, src)
    treeFrom(file, true) // use the side effect
    file.file
  } 
}

object CompilerInstance extends CompilerInstance

trait CompilerProvider extends TreeCreationMethods {

  val global = CompilerInstance.compiler
}


