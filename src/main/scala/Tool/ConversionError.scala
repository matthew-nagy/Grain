package Tool

class ConversionError(filename: String, message: String) extends Exception(message){
  override def toString: String = "Conversion error with file " ++ filename ++ ": " ++ message ++ " " ++ tag

  private var tag = ""
  def addTag(newTag: String):ConversionError = {
    tag = newTag
    this
  }
}

object ConversionError{
  def invalidFilename(filename: String): ConversionError =
    ConversionError(filename, "The file cannot be found")
  def invalidWidth(width: Int, filename: String): ConversionError =
    ConversionError(filename, "The SNES requires 16, 8x8 tiles per row. The image provided has " ++ width.toString)
  def invalidHeight(height: Int, filename: String): ConversionError =
    ConversionError(filename, 
      "The SNES requires 8x8 tiles, but the provided height (" ++ height.toString ++ ") is not a multiple of 8"
    )
  def invalidBpp(bpp: Int, filename: String): ConversionError =
    ConversionError(filename, "SNES supports up to 8 bpp, image provided has " ++ bpp.toString)
}