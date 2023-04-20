package Grain

import scala.language.implicitConversions
import grapple.json.{Json, JsonInput, JsonObject, JsonValue, jsonValueToString}

import java.nio.file.Path
import scala.io.Source

object GlobalData {
  case class GlobalConfig(
                           globalsStart: Int,
                           assemblerPath: String, emulatorPath: String,
                           includePaths: List[String], defaultOptimisations: Boolean,
                           romHeaderPath: String, initPath: String, preSetupCode: List[String]
                         )
  private def loadGlobalConfig(): GlobalConfig = {
    given JsonInput[GlobalConfig] with
      def read(jsonVal: JsonValue): GlobalConfig =
        jsonVal match{
          case json: JsonObject =>
            GlobalConfig(
              json.getInt("globals_start"),
              json.getString("assembler_path"), json.getString("emulator_path"),
              json.getJsonArray("include_paths").values.map(_.as[String]).toList,
              json.getBoolean("default_optimisations_full"),
              json.getString("ROM_header_path"), json.getString("init_path"),
              json.getJsonArray("pre_setup_code").values.map(_.as[String]).toList
            )
        }

    val configFile = Source.fromFile("grain_config.json")
      .getLines
      .toList
      .foldLeft("")(_ ++ _)

    val newConfig = Json.parse(configFile).as[GlobalConfig]

    //Quick sanity checks
    if(newConfig.globalsStart < 18){
      throw new Exception("Global data will be defined in memory used by Grain at runtime")
    }

    newConfig
  }

  val Config: GlobalConfig = loadGlobalConfig()
  object Addresses {
    val logicalNotAddress = 0 //Stores 0xFFFF to xor with the effect of a logical NOT
    val tempStack = 2 //When you bank switch (when I add that), you can store where the stack *was*, here
    val dmaFlags = 4  //Keep track of what channels you set up
    val frameFinished = 6 //Has the current frame finished updating?
    val randomSeed = 10 //Current random value
    val multiplicationResultHigh = 8
    val multiplicationResultLow = 12
    val hardwareMathsArgLeft = 14
    val hardwareMathsArgRight = 16

    val signedMultiplyMultiplicand = 0x211b
    val signedMultiplyMultiplier = 0x211c
    val signedMultiplyUpByteResult = 0x2136
    val signedMultiplyMidByteResult = 0x2135
    val signedMultiplyLowByteResult = 0x2134

    val dividendLowByte = 0x4204
    val divisor = 0x4206
    val divisionResultLowByte = 0x4214
    val divisionRemainderLowByte = 0x4216
  }
  
  object optimisationFlags{
    private val optimisations = Config.defaultOptimisations
    val staticOptimiseTree = true
    val stackPressureOptimisations = false
    val optimiseStackUsage: Boolean = optimisations
    val optimiseRegisterUsage: Boolean = optimisations
    val optimiseDirectAddresses: Boolean = optimisations
    val optimiseTransfers: Boolean = optimisations
    val optimiseHardwareQuirks: Boolean = optimisations
  }

  object snesData{
    val bankSize = 0x5000 //0x7FFF - the 0x1FFF WRAM mirror
    val generalInstructionSize = 3
    val maxConditionalJumpLength = 128

    val fileStart: List[String] = List(
      ".include \"" ++ Config.romHeaderPath ++ "\"",
      ".include \"" ++ Config.initPath ++ "\"",
      ".bank 0",
      ".org $0",
      "Start:") :::
      Config.preSetupCode ::: List(
      "rep #$30",
      "lda #$FFFF",
      "sta 0",
      "stz 2",
      "stz 4",
      "stz 6"
    )

    val multiply16x16: List[String] = List(
      "signed_16x16_multiplication: ;https://wiki.superfamicom.org/16-bit-multiplication-and-division",
      "sep #$10",
      "ldx " ++ Addresses.hardwareMathsArgLeft.toString,
      "stx $4202",
      "ldy " ++ Addresses.hardwareMathsArgRight.toString,
      "sty $4203" ++ " ;set up 1st multiply",
      "ldx " ++ (Addresses.hardwareMathsArgRight + 1).toString,
      "clc",
      "lda $4216" ++ " ;load $4216 for 1st multiply",
      "stx $4203" ++ " ;start 2nd multiply",
      "sta " ++ Addresses.multiplicationResultLow.toString,
      "stz " ++ Addresses.multiplicationResultHigh.toString ++ " ;high word of product needs to be cleared",
      "lda $4216" ++ " ;read $4216 from 2nd multiply",
      "ldx " ++ (Addresses.hardwareMathsArgLeft+1).toString,
      "stx $4202" ++ " ;set up 3rd multiply",
      "sty $4203" ++ " ;y still contains temp2",
      "ldy " ++ (Addresses.hardwareMathsArgRight + 1).toString,
      "adc " ++ (Addresses.multiplicationResultLow + 1).toString,
      "adc $4216 ;add 3rd product",
      "sta " ++ (Addresses.multiplicationResultLow + 1).toString,
      "sty $4203 ;set up 4th multiply",
      "lda " ++ Addresses.multiplicationResultHigh.toString ++ " ;carry bit to last byte of product",
      "bcc +",
      "adc #$00ff",
      "+:",
      "adc $4216", //add 4th product
      "cpx #$80",
      "bcc +",
      "sbc {temp2}",
      "+:",
      "cpy #$80",
      "bcc +",
      "sbc {temp}",
      "+:",
      "sta {temp4}", //final store
      "rep",
      "#$10",
      "rts"
    )
  }
}
