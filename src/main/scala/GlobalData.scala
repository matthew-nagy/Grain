package Grain

object GlobalData {
  object Addresses {
    val logicalNotAddress = 0
    val tempStack = 2

    val signedMultiplyMultiplicand = 0x211b
    val signedMultiplyMultiplier = 0x221c
    val signedMultiplyLowByteResult = 0x2134
  }
  
  object optimisationFlags{
    val staticOptimiseTree = true
  }

  object snesData{
    val bankSize = 65536
    val generalInstructionSize = 3

    val fileStart: List[String] = List(
      ".include \"Header.inc\"",
      ".include \"Snes_Init.asm\"",
      ".bank 0",
      ".org $0",
      "Start:",
      "Snes_Init"
    )
  }
}
