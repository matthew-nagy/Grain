package Grain

import Grain.Operation.Binary.Add
import TreeWalker.*

import scala.language.implicitConversions
import grapple.json.{Json, JsonInput, JsonObject, JsonValue, jsonValueToString}

import java.io.ObjectInputFilter
import java.nio.file.Path
import scala.io.Source

object GlobalData {
  case class GlobalConfig(
                           globalsStart: Int, globalsLowLimit: String, wramTop: String,
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
              json.getString("lowram_global_limit"),
              json.getString("wram_top"),
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
    val randomSeed = 8 //Current random value
    val multiplicationResultLow = 10
    val multiplicationResultHigh = 12
    val hardwareMathsArgLeft = 14
    val hardwareMathsArgRight = 16
    val divisionTemp1 = 18
    val divisionTemp2 = 20
    val divisionTemp3 = 22
    val divisionTemp4 = 24

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
    val stackPressureOptimisations = optimisations
    val optimiseStackUsage: Boolean = optimisations
    val optimiseRegisterUsage: Boolean = optimisations
    val optimiseDirectAddresses: Boolean = optimisations
    val optimiseTransfers: Boolean = optimisations
    val optimiseHardwareQuirks: Boolean = optimisations
    val optimiseBubbleUp: Boolean = optimisations
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

  }
}
