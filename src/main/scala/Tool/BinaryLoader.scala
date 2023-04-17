package Tool

import java.io.FileInputStream
import java.nio.file.{Files, Paths}
import scala.collection.mutable.ListBuffer

object BinaryLoader {

  val wordsPerLine = 16
  val bytesPerLine = 32

  case class ReadBinary(binaryStrings: List[String], wordLength: Int)

  private enum Filetype:
    case Comma_Seperated_Value_File, Binary_File

  private val extentionToTypeMap: Map[String, Filetype] = Map(
    "csv" -> Filetype.Comma_Seperated_Value_File,
    "bin" -> Filetype.Binary_File
  )

  private def getFileExtension(filename: String): String =
    filename.split('.').last

  private def getFileType(filename: String): Filetype = {
    val extension = getFileExtension(filename)
    if(extentionToTypeMap.contains(extension)){
      extentionToTypeMap(extension)
    }
    else{
      throw Utility.Errors.UnrecognisedFileExtension(extension, filename, extentionToTypeMap.keys.toList)
    }
  }

  private def byteToString(byte: Byte): String = {
    val hexDigitArray = Array("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F")
    "$" ++ hexDigitArray(byte >> 4) ++ hexDigitArray(byte & 0xF)
  }

  private def wordToString(word: Int): String = {
    byteToString((word >> 16).toByte) ++ byteToString((word & 0xFF).toByte).substring(1)
  }

  private def readCSV(filename: String): ReadBinary ={
    try {
      val csv = io.Source.fromFile(filename)
      var currentLine = ".dw"
      var currentLineCount = 0
      val lines = ListBuffer.empty[String]
      for(line <- csv.getLines()){
        val words = line.split(',').map(_.trim)
        for(word <- words)do{
          val asString = wordToString(word.toInt)
          currentLine = currentLine ++ " " ++ asString
          currentLineCount += 1
          if(currentLineCount == wordsPerLine){
            lines.addOne(currentLine)
            currentLine = ".dw"
            currentLineCount = 0
          }
        }
      }
      if(currentLineCount > 0){
        lines.addOne(currentLine)
      }
      val wordLengthOfFullLines = (lines.length - 1) * wordsPerLine
      ReadBinary(lines.toList, currentLineCount + wordLengthOfFullLines)
    }
    catch {
      case e@_ =>
        println("Error reading ->" ++ filename ++ "<-")
        throw e
    }

  }

  private def readBinary(filename: String): ReadBinary = {
    try{
      val byteArray = Files.readAllBytes(Paths.get(filename))
      val lines = ListBuffer.empty[String]
      var index = 0
      var lineSpecific = 0
      while index < byteArray.length do{
        lineSpecific = 0
        var line = ".db"
        while lineSpecific < bytesPerLine && index < byteArray.length do{
          line = line ++ " " ++ byteToString(byteArray(index))
          index += 1
          lineSpecific += 1
        }
        if(lineSpecific % 2 == 1){
          lines.addOne(byteToString(0.toByte))
          lineSpecific += 1
        }
        lines.addOne(line)
      }
      val wordLengthOfFullLines = (lines.length - 1) * bytesPerLine
      val wordLengthOfLastLine = lineSpecific / 2
      ReadBinary(lines.toList, wordLengthOfFullLines + wordLengthOfLastLine)
    }
    catch {
      case e@_ =>
        println("Error reading ->" ++ filename ++ "<-")
        throw e
    }
  }

  def apply(filename: String): ReadBinary = {
    getFileType(filename) match
      case Filetype.Comma_Seperated_Value_File => readCSV(filename)
      case Filetype.Binary_File => readBinary(filename)
  }
}

