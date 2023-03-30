package Grain

object GlobalData {
  object Addresses {
    val logicalNotAddress = 0
    val tempStack = 2
    val dmaFlags = 4
    val frameFinished = 6
    val randomSeed = 10

    val signedMultiplyMultiplicand = 0x211b
    val signedMultiplyMultiplier = 0x221c
    val signedMultiplyLowByteResult = 0x2134
  }
  
  object optimisationFlags{
    val staticOptimiseTree = false
    val stackPressureOptimisations = false
    val optimiseStackUsage = false
    val optimiseRegisterUsage = false
    val optimiseDirectAddresses = false
    val optimiseHardwareQuirks = false
  }

  object snesData{
    val bankSize = 65536
    val generalInstructionSize = 3
    val maxConditionalJumpLength = 128

    val fileStart: List[String] = List(
      ".include \"Header.inc\"",
      ".include \"Snes_Init.asm\"",
      ".bank 0",
      ".org $0",
      "Start:",
      "Snes_Init",
      "rep #$30",
      "lda #$FFFF",
      "sta 0",
      "stz 2",
      "stz 4",
      "stz 6"
    )
  }
}
