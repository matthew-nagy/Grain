package Tool
import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class LoadedImage(
                image: BufferedImage,
                originalPalette: Map[Int, Byte],
                paletteStrings: List[String],
                dataStrings: List[String],
                bpp: Utility.Bitdepth,
                numberOfTiles: Int,
                numberOfPalettes: Int
                ){
  def dataSize: Int = dataStrings.length * 2

  def palleteSize: Int = dataStrings.length * 2
  override def toString: String = {
    "Palette ->" ++ paletteStrings.foldLeft("")(_ ++ "\n\t" ++ _) ++ "\n\nData ->" ++ dataStrings.foldLeft("")(_ ++ "\n\t" ++ _)
  }
}

class ImageLoader(image: BufferedImage, filename: String) {
  //I mean this really could have been a list buffer couldn't it
  private val palette = mutable.Map.empty[Int, Byte] //Colour to index. Transparency is always 0

  //Char is the 16 bit unsigned type
  def getPaletteString: List[String] = {
    val buffer = ListBuffer.empty[String]
    buffer.addAll(Range(0, 16).map(_ => ".dw %0000000000000000"))
    for (colour, index) <- palette do{
      buffer(index) = ".dw %" ++ ImageLoader.toSNESColour(colour)
    }
    buffer.toList
  }

  def getColourIndex(colour: Int): Byte =
    if((colour & ImageLoader.transparencyFlag) == 0){
      0.toByte
    }
    else if(palette.contains(colour)){
      palette(colour)
    }
    else{
      val index = (palette.size + 1).toByte
      palette.addOne((colour, index))
      index
    }

  def cellToStrings(startX: Int, startY: Int, indices: List[List[Byte]]): List[String] = {
    var bppIterations = getBpp.toString.toInt / 2
    val buffer = ListBuffer.empty[String]
    var shifter = 0

    while bppIterations > 0 do{
      val firstShifter = shifter
      val secondShifter = shifter + 1

      val plane1 = StringBuilder()
      val plane2 = StringBuilder()

      for y <- Range(startY, startY + 8) do{
        for x <- Range(startX, startX + 8) do{
          plane1.append(if (((indices(y)(x) >> firstShifter) & 0x01) > 0) "1" else "0")
          plane2.append(if (((indices(y)(x) >> secondShifter) & 0x01) > 0) "1" else "0")
        }

        val line = ".db %" ++ plane1.reverse.toString() ++ " %" ++ plane2.reverse.toString()
        buffer.addOne(line)
        plane1.clear()
        plane2.clear()
      }

      bppIterations -= 1
      shifter += 2
    }
    buffer.append("\n");
    buffer.toList
  }

  def toDataString: List[String] = {
    val indexedColours = (for y <- Range(0, image.getHeight()) yield
      (for x <- Range(0, image.getWidth()) yield
        getColourIndex(image.getRGB(x, y))
        ).toList).toList
    val rows = image.getHeight / 8

    (for row <- Range(0, rows) yield{
      (for column <- Range(0, 16) yield{
        cellToStrings(column * 8, row * 8, indexedColours)
      }).toList
    }).toList
      .foldLeft(ListBuffer.empty[List[String]])((buffer, stringList) => buffer.addAll(stringList))
      .toList
      .foldLeft(ListBuffer.empty[String])((buffer, stringList) => buffer.addAll(stringList))
      .toList
  }

  def getBpp: Utility.Bitdepth = {
    val numberOfColours = palette.size + 1
    if(numberOfColours <= 2){
      2
    }
    else if(numberOfColours <= 4){
      4
    }
    else if(numberOfColours <= 8){
      8
    }
    else{
      throw ConversionError.invalidBpp(numberOfColours, filename)
    }
  }

  def toLoadedImage(): LoadedImage = {
    val dataString = toDataString
    val numberOfPalletes = 1 //Do stuff with this later
    val numberOfTiles = (image.getWidth() / 8) * (image.getHeight() / 8) //change later if sprite size changes
    LoadedImage(image, palette.toMap, getPaletteString, dataString, getBpp, numberOfTiles, numberOfPalletes)
  }
}
object ImageLoader {
  private val requiredWidth = 8 * 16 //The width in memory
  private val transparencyFlag = 0xFF000000 //Anything this or less is transparent

  case class Colour(r: Int, g: Int, b: Int, transparent: Boolean)
  object Colour{
    def create(colourValue: Int): Colour = {
      val b = colourValue & 0xFF
      val g = (colourValue >> 8) & 0xFF
      val r = (colourValue >> 16) & 0xFF
      val transparent: Boolean = (colourValue >> 24) == 0
      Colour(r, g, b, transparent)
    }
  }

  def toSNESColour(encodedColour: Int): String = {
    val colour = Colour.create(encodedColour)
    println(colour)
    def convert(channel: Int, shift: Int): Int = (channel >> 3).toInt << shift
    val convertedInt = convert(colour.r, 0) + convert(colour.g, 5) + convert(colour.b, 10)
    println(convert(colour.r, 0).toString ++ " " ++ convert(colour.g, 0).toString ++ " " ++ convert(colour.b, 0).toString)
    val transparencyBit = if(colour.transparent) "1" else "0"
    Range(0, 15)
      .reverse
      .map(bitNum => Math.pow(2, bitNum).toInt & convertedInt)
      .map(result => if(result > 0) "1" else "0")
      .foldLeft(StringBuilder().append(transparencyBit))((builder, newChar) => builder.append(newChar))
      .toString()
  }

  def tryGetPhoto(filename: String): BufferedImage = {
    try
      ImageIO.read(new File(filename))
    catch
        case e @ _ =>
          println("Error reading ->" ++ filename ++ "<-")
          throw e
          println("Java gave error: " ++ e.toString)
          throw ConversionError.invalidFilename(filename)
  }

  def apply(filename: String): LoadedImage = {
    val photo = tryGetPhoto(filename)

    if(photo.getWidth != requiredWidth){
      throw ConversionError.invalidWidth(photo.getWidth, filename)
    }
    if(photo.getHeight % 8 != 0){
      throw ConversionError.invalidHeight(photo.getHeight, filename)
    }

    println(photo.getRGB(2, 3).toHexString)

    (new ImageLoader(photo, filename)).toLoadedImage()
  }

  def main(args: Array[String]): Unit = {

    val dir: String = System.getProperty("user.dir")
    println("Running in " ++ dir)
    println(apply("src/main/littleGuy.png"))
  }
}
