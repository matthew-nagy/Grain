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
    private val optimisations = true
    val staticOptimiseTree = true
    val stackPressureOptimisations = false
    val optimiseStackUsage: Boolean = optimisations
    val optimiseRegisterUsage: Boolean = optimisations
    val optimiseDirectAddresses: Boolean = optimisations
    val optimiseTransfers: Boolean = optimisations
    val optimiseHardwareQuirks: Boolean = optimisations
  }

  object snesData{
    val bankSize = 65536
    val generalInstructionSize = 3
    val maxConditionalJumpLength = 128

    val fileStart: List[String] = List(
      ".include \"snes/Header.inc\"",
      ".include \"snes/Snes_Init.asm\"",
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
