
;User Assembly
.include "snes/Header.inc"
.include "snes/Snes_Init.asm"
.bank 0
.org $0
Start:
Snes_Init
rep #$30
lda #$FFFF
sta 0
stz 2
stz 4
stz 6
;End Assembly
stz 4512
lda #2
sta 4514	;Storing the assignment
lda #15
sta 4516	;Storing the assignment
lda #16
sta 4518	;Storing the assignment
lda #17
sta 4520	;Storing the assignment
lda #18
sta 4522	;Storing the assignment
lda #19
sta 4524	;Storing the assignment
lda #1
sta 4526	;Storing the assignment
jml main_function

;---------------

func_setVRamTarget:
;User Assembly
lda 4, s
sta $2116
rtl
;End Assembly

;---------------

func_dmaGraphicsToVRam:
;User Assembly
lda 4
ora #%00000001
sta 4
lda 4, s
sta $4305
lda 8, s
sta $4302
sep #$20
lda 6, s
sta $4304
lda #$18
sta $4301
lda #1
sta $4300
rep #$20
rtl
;End Assembly

;---------------

func_dmaDataToVRam:
;User Assembly
bra func_dmaGraphicsToVRam
;End Assembly

;---------------

func_dmaArrayToVRam:
;User Assembly
bra func_dmaGraphicsToVRam
;End Assembly

;---------------

func_dmaToCGRam:
;User Assembly
lda 4
ora #%00000010
sta 4
lda 4, s
sta $4315
lda 8, s
sta $4312
sep #$20
lda 6, s
sta $4314
lda #$22
sta $4311
lda #0
sta $4310
rep #$20
rtl
;End Assembly

;---------------

func_executeDMA:
;User Assembly
sep #$20
lda 4
stz 4
sta $420B
rep #$20
rtl
;End Assembly

;---------------

func_enableInputAndNMI:
;User Assembly
sep #$20
lda #%10000001
sta $4200
rep #$20
rtl
;End Assembly

;---------------

func_setScreenDisabledAndBrightness:
;User Assembly
lda 6, s
xba
lsr
sep #$20
ora 4, s
sta $2100
rep #$20
rtl
;End Assembly

;---------------

func_getPlayerInput:
;User Assembly
lda 4, s
asl
tax
func_getPlayerInput_waitForUserInput:
lda $4212
and #1
bne func_getPlayerInput_waitForUserInput
;lda $4216, x
lda $4218
sta $30
rtl
;End Assembly

;---------------

func_updatePlayerInput:
tsx
phx	;Record stack frame
phx	;Dummy push
lda 8, s
pha
jsl func_getPlayerInput
sta 3, s
lda #0
sta 1, s
for_l67_f2:
lda #12	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 67)),NumericalLiteral(12))
cmp 1, s
bcc for_end_l67_f2
beq for_end_l67_f2	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 67)),NumericalLiteral(12))), either branched if IfFalse or fallen through
lda 1, s
asl A
tay
lda #0
sta (12, s), y
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l67_f2
for_end_l67_f2:
plx	;Dummy pull
lda 1, s	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 68)),NumericalLiteral(32768)),NumericalLiteral(0))
and #32768
pha
lda #0
cmp 1, s
bcs Else_l68_f2	;End of condition (BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 68)),NumericalLiteral(32768)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1
pha
lda 14, s
tax
pla
sta 10, x
Else_l68_f2:
If_End_l68_f2:
plx	;Dummy pull
lda 1, s	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 69)),NumericalLiteral(16384)),NumericalLiteral(0))
and #16384
pha
lda #0
cmp 1, s
bcs Else_l69_f2	;End of condition (BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 69)),NumericalLiteral(16384)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1
pha
lda 14, s
tax
pla
sta 14, x
Else_l69_f2:
If_End_l69_f2:
plx	;Dummy pull
lda 1, s	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 70)),NumericalLiteral(8192)),NumericalLiteral(0))
and #8192
pha
lda #0
cmp 1, s
bcs Else_l70_f2	;End of condition (BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 70)),NumericalLiteral(8192)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1
pha
lda 14, s
tax
pla
sta 22, x
Else_l70_f2:
If_End_l70_f2:
plx	;Dummy pull
lda 1, s	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 71)),NumericalLiteral(4096)),NumericalLiteral(0))
and #4096
pha
lda #0
cmp 1, s
bcs Else_l71_f2	;End of condition (BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 71)),NumericalLiteral(4096)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1
pha
lda 14, s
tax
pla
sta 20, x
Else_l71_f2:
If_End_l71_f2:
plx	;Dummy pull
lda 1, s	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 72)),NumericalLiteral(2048)),NumericalLiteral(0))
and #2048
pha
lda #0
cmp 1, s
bcs Else_l72_f2	;End of condition (BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 72)),NumericalLiteral(2048)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1
pha
lda 14, s
tax
pla
sta 4, x
Else_l72_f2:
If_End_l72_f2:
plx	;Dummy pull
lda 1, s	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 73)),NumericalLiteral(1024)),NumericalLiteral(0))
and #1024
pha
lda #0
cmp 1, s
bcs Else_l73_f2	;End of condition (BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 73)),NumericalLiteral(1024)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1
pha
lda 14, s
tax
pla
sta 6, x
Else_l73_f2:
If_End_l73_f2:
plx	;Dummy pull
lda 1, s	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 74)),NumericalLiteral(512)),NumericalLiteral(0))
and #512
pha
lda #0
cmp 1, s
bcs Else_l74_f2	;End of condition (BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 74)),NumericalLiteral(512)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1
pha
lda 14, s
tax
pla
sta 0, x
Else_l74_f2:
If_End_l74_f2:
plx	;Dummy pull
lda 1, s	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 75)),NumericalLiteral(256)),NumericalLiteral(0))
and #256
pha
lda #0
cmp 1, s
bcs Else_l75_f2	;End of condition (BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 75)),NumericalLiteral(256)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1
pha
lda 14, s
tax
pla
sta 2, x
Else_l75_f2:
If_End_l75_f2:
plx	;Dummy pull
lda 1, s	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 76)),NumericalLiteral(128)),NumericalLiteral(0))
and #128
pha
lda #0
cmp 1, s
bcs Else_l76_f2	;End of condition (BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 76)),NumericalLiteral(128)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1
pha
lda 14, s
tax
pla
sta 8, x
Else_l76_f2:
If_End_l76_f2:
plx	;Dummy pull
lda 1, s	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 77)),NumericalLiteral(64)),NumericalLiteral(0))
and #64
pha
lda #0
cmp 1, s
bcs Else_l77_f2	;End of condition (BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 77)),NumericalLiteral(64)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1
pha
lda 14, s
tax
pla
sta 12, x
Else_l77_f2:
If_End_l77_f2:
plx	;Dummy pull
lda 1, s	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 78)),NumericalLiteral(32)),NumericalLiteral(0))
and #32
pha
lda #0
cmp 1, s
bcs Else_l78_f2	;End of condition (BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 78)),NumericalLiteral(32)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1
pha
lda 14, s
tax
pla
sta 16, x
Else_l78_f2:
If_End_l78_f2:
plx	;Dummy pull
lda 1, s	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 79)),NumericalLiteral(16)),NumericalLiteral(0))
and #16
pha
lda #0
cmp 1, s
bcs Else_l79_f2	;End of condition (BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 79)),NumericalLiteral(16)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1
pha
lda 14, s
tax
pla
sta 18, x
Else_l79_f2:
If_End_l79_f2:
lda 5, s
tcs
rtl

;---------------

func_frame:
;User Assembly
inc 6
wai
rtl
;End Assembly

;---------------

func_enableScreens:
;User Assembly
sep #$20
lda 4, s
asl
ora 6, s
asl
ora 8, s
asl
ora 10, s
asl
ora 12, s
sta $212c
rep #$20
rtl
;End Assembly

;---------------

func_setBackgroundColour:
;User Assembly
sep #$20
stz $2121
lda 4, s
sta $2122
lda 5, s
sta $2122
rep #$20
rtl
;End Assembly

;---------------

func_setBackgroundAddressAndSize:
;User Assembly
sep #$30
lda 8, s
tax
lda 6, s
asl
asl
ora 4, s
sta $2106, x
rep #$30
rtl
;End Assembly

;---------------

func_setSeed:
;User Assembly
lda 4, s
sta $A
rtl
;End Assembly

;---------------

func_random:
;User Assembly
sep #$30
lda $B
tay
lsr
lsr
lsr
sta $B
lsr
eor $B
lsr
eor $B
eor $A
sta $B
tya
sta $A
asl
eor $A
asl
eor $A
asl
asl
asl
eor $A
sta $A
rep #$30
lda $A
rtl
;End Assembly

;---------------

func_setTile:
tsx
phx	;Record stack frame
lda 10, s
inc A
sta 10, s	;Storing the assignment
lda 8, s
clc
adc #5
sta 8, s
asl A
asl A
asl A
asl A
asl A
asl A
clc
adc #56
pha
lda 12, s
asl A
clc
adc 1, s
plx	;Dummy pull
tax
lda 6, s
sta 0, x
plx	;Dummy pull
rtl

;---------------

func_getTile:
tsx
phx	;Record stack frame
lda 8, s
inc A
sta 8, s	;Storing the assignment
lda 6, s
clc
adc #5
sta 6, s
asl A
asl A
asl A
asl A
asl A
asl A
clc
adc #56
pha
lda 10, s
asl A
clc
adc 1, s
plx	;Dummy pull
tax
lda 0, x
plx	;Dummy pull
rtl

;---------------

func_isSnakeTile:
tsx
phx	;Record stack frame
phx	;Dummy push
lda 10, s
pha
lda 10, s
pha
jsl func_getTile
sta 5, s
plx	;Dummy pull
plx	;Dummy pull
lda 4520	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeHeadTile', 50)))
cmp 1, s
bne ++	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeHeadTile', 50)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeHeadTile', 50)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
pha
lda 4518	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeBodyTile', 50)))
cmp 3, s
bne ++	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeBodyTile', 50)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeBodyTile', 50)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
ora 1, s
sta 1, s
lda 4522	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeTailTile', 50)))
cmp 3, s
bne ++	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeTailTile', 50)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeTailTile', 50)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
ora 1, s
tay
lda 5, s
tcs
tya
rtl

;---------------

func_spawnFruit:
tsx
phx	;Record stack frame
jsl func_random
and #31
pha
jsl func_random
and #31
pha
lda #30	;Condition: BinaryOp(GreaterEqual,Variable(Token(Identifier, 'x', 56)),NumericalLiteral(30))
cmp 3, s
beq +
bcs Else_l56_f0
+:	;End of condition (BinaryOp(GreaterEqual,Variable(Token(Identifier, 'x', 56)),NumericalLiteral(30))), either branched if IfFalse or fallen through
lda 3, s
sec
sbc #10
sta 3, s	;Storing the assignment
Else_l56_f0:
If_End_l56_f0:
lda #20	;Condition: BinaryOp(GreaterEqual,Variable(Token(Identifier, 'y', 57)),NumericalLiteral(20))
cmp 1, s
beq +
bcs Else_l57_f0
+:	;End of condition (BinaryOp(GreaterEqual,Variable(Token(Identifier, 'y', 57)),NumericalLiteral(20))), either branched if IfFalse or fallen through
lda 1, s
sec
sbc #16
sta 1, s	;Storing the assignment
Else_l57_f0:
If_End_l57_f0:
lda 3, s	;Condition: BinaryOp(Or,FunctionCall(Variable(Token(Identifier, 'isSnakeTile', 59)),List(Variable(Token(Identifier, 'x', 59)), Variable(Token(Identifier, 'y', 59)))),BinaryOp(And,BinaryOp(Equal,Variable(Token(Identifier, 'x', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'x', 59))),BinaryOp(Equal,Variable(Token(Identifier, 'y', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'y', 59)))))
pha
lda 3, s
pha
jsl func_isSnakeTile
sta 3, s
plx	;Dummy pull
lda 4508	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'x', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'x', 59)))
cmp 5, s
bne ++	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'x', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'x', 59)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Equal,Variable(Token(Identifier, 'x', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'x', 59)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
pha
lda 4510	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'y', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'y', 59)))
cmp 5, s
bne ++	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'y', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'y', 59)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Equal,Variable(Token(Identifier, 'y', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'y', 59)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
and 1, s
plx	;Dummy pull
ora 1, s
plx	;Dummy pull
cmp #1
bne Else_l59_f0	;End of condition (BinaryOp(Or,FunctionCall(Variable(Token(Identifier, 'isSnakeTile', 59)),List(Variable(Token(Identifier, 'x', 59)), Variable(Token(Identifier, 'y', 59)))),BinaryOp(And,BinaryOp(Equal,Variable(Token(Identifier, 'x', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'x', 59))),BinaryOp(Equal,Variable(Token(Identifier, 'y', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'y', 59)))))), either branched if IfFalse or fallen through
jsl func_spawnFruit
bra If_End_l59_f0
Else_l59_f0:
lda 3, s
pha
lda 3, s
pha
lda 4524
pha
jsl func_setTile
lda 9, s
sta 4508
lda 7, s
sta 4510
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
If_End_l59_f0:
lda 5, s	;Fix stack before return
tcs
rtl

;---------------

func_sendDataToBackground2:
tsx
phx	;Record stack frame
lda #2048
pha
jsl func_setVRamTarget
lda #56
sta 1, s
lda #0
pha
lda #2048
pha
jsl func_dmaArrayToVRam
jsl func_executeDMA
lda 7, s
tcs
rtl

;---------------

func_initSnake:
tsx
phx	;Record stack frame
lda #0
pha
for_l76_f0:
lda #1024	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 76)),NumericalLiteral(1024))
cmp 1, s
bcc for_end_l76_f0
beq for_end_l76_f0	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 76)),NumericalLiteral(1024))), either branched if IfFalse or fallen through
lda 1, s
asl A
tax
lda 4516
sta 56, x
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l76_f0
for_end_l76_f0:
lda #0
sta 1, s	;Storing the assignment
for_l78_f0:
lda #1200	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 78)),NumericalLiteral(1200))
cmp 1, s
bcc for_end_l78_f0
beq for_end_l78_f0	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 78)),NumericalLiteral(1200))), either branched if IfFalse or fallen through
lda 1, s
asl A
tax
lda #0
sta 2104, x
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l78_f0
for_end_l78_f0:
lda #20
sta 2104
lda #20
sta 2106
lda #20
sta 2108
lda #11
sta 3304
lda #10
sta 3306
lda #9
sta 3308
lda #20
sta 1, s
lda #11
pha
lda 4522
pha
jsl func_setTile
lda #20
sta 5, s
lda #10
sta 3, s
lda 4518
sta 1, s
jsl func_setTile
lda #20
sta 5, s
lda #9
sta 3, s
lda 4520
sta 1, s
jsl func_setTile
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
stz 4512
lda #2
sta 4514	;Storing the assignment
stz 4504
lda #-1
sta 4506	;Assigning simple getter
lda #1
pha
lda #2
pha
lda #0
pha
jsl func_setBackgroundAddressAndSize
lda 7, s
tcs
rtl

;---------------

func_secondarySetup:
tsx
phx	;Record stack frame
jsl func_initSnake
lda #-1
sta 4508	;Assigning simple getter
lda #-1
sta 4510	;Assigning simple getter
jsl func_spawnFruit
jsl func_sendDataToBackground2
plx	;Dummy pull
rtl

;---------------

func_setup:
tsx
phx	;Record stack frame
lda #1
pha
lda #0
pha
jsl func_setScreenDisabledAndBrightness
lda #backgroundSprites
sta 3, s
lda #1
sta 1, s
lda #256
pha
jsl func_dmaGraphicsToVRam
lda #snakePalette
sta 5, s
lda #1
sta 3, s
lda #64
sta 1, s
jsl func_dmaToCGRam
jsl func_executeDMA
lda #snakeSprites
sta 5, s
lda #1
sta 3, s
lda #256
sta 1, s
jsl func_dmaGraphicsToVRam
lda #backgroundPalette
sta 5, s
lda #1
sta 3, s
lda #32
sta 1, s
jsl func_dmaToCGRam
jsl func_executeDMA
lda #2272
sta 5, s
plx	;Dummy pull
plx	;Dummy pull
jsl func_setBackgroundColour
lda #1024
sta 1, s
jsl func_setVRamTarget
lda #2
sta 1, s
lda #1
pha
lda #0
pha
jsl func_setBackgroundAddressAndSize
lda #tilemap
sta 5, s
lda #1
sta 3, s
lda #2016
sta 1, s
jsl func_dmaDataToVRam
jsl func_executeDMA
jsl func_secondarySetup
lda #1
sta 5, s
sta 3, s
lda #0
sta 1, s
pha
lda #0
pha
jsl func_enableScreens
jsl func_enableInputAndNMI
lda #0
sta 9, s
lda #15
sta 7, s
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
jsl func_setScreenDisabledAndBrightness
lda 5, s
tcs
rtl

;---------------

func_updatePlayerDirectionalOffset:
tsx
phx	;Record stack frame
lda 32	;Condition: Get(Variable(Token(Identifier, 'playerInput', 134)),Token(Identifier, 'Left', 134))
cmp #1
bne Else_l134_f0	;End of condition (Get(Variable(Token(Identifier, 'playerInput', 134)),Token(Identifier, 'Left', 134))), either branched if IfFalse or fallen through
lda #-1
sta 4504	;Assigning simple getter
stz 4506
bra If_End_l134_f0
Else_l134_f0:
lda 34	;Condition: Get(Variable(Token(Identifier, 'playerInput', 138)),Token(Identifier, 'Right', 138))
cmp #1
bne Else_l138_f0	;End of condition (Get(Variable(Token(Identifier, 'playerInput', 138)),Token(Identifier, 'Right', 138))), either branched if IfFalse or fallen through
lda #1
sta 4504	;Assigning simple getter
stz 4506
bra If_End_l138_f0
Else_l138_f0:
lda 36	;Condition: Get(Variable(Token(Identifier, 'playerInput', 142)),Token(Identifier, 'Up', 142))
cmp #1
bne Else_l142_f0	;End of condition (Get(Variable(Token(Identifier, 'playerInput', 142)),Token(Identifier, 'Up', 142))), either branched if IfFalse or fallen through
stz 4504
lda #-1
sta 4506	;Assigning simple getter
bra If_End_l142_f0
Else_l142_f0:
lda 38	;Condition: Get(Variable(Token(Identifier, 'playerInput', 146)),Token(Identifier, 'Down', 146))
cmp #1
bne Else_l146_f0	;End of condition (Get(Variable(Token(Identifier, 'playerInput', 146)),Token(Identifier, 'Down', 146))), either branched if IfFalse or fallen through
stz 4504
lda #1
sta 4506	;Assigning simple getter
Else_l146_f0:
If_End_l146_f0:
If_End_l142_f0:
If_End_l138_f0:
If_End_l134_f0:
plx	;Dummy pull
rtl

;---------------

func_updateFrame:
tsx
phx	;Record stack frame
tsc
sec
sbc #8
tcs
lda #32
pha
lda #1
pha
jsl func_updatePlayerInput
jsl func_updatePlayerDirectionalOffset
plx	;Dummy pull
plx	;Dummy pull
lda 4514
asl A
tax
lda 2104, x
sta 7, s	;Storing the assignment
lda 4514
asl A
tax
lda 3304, x
sta 5, s	;Storing the assignment
lda 4504
clc
adc 7, s
sta 3, s	;Storing the assignment
lda 4506
clc
adc 5, s
sta 1, s	;Storing the assignment
lda #0	;Condition: BinaryOp(And,BinaryOp(And,BinaryOp(And,BinaryOp(And,BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newX', 161)),NumericalLiteral(0)),BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newY', 161)),NumericalLiteral(0))),BinaryOp(Less,Variable(Token(Identifier, 'newX', 161)),NumericalLiteral(30))),BinaryOp(Less,Variable(Token(Identifier, 'newY', 161)),NumericalLiteral(20))),BinaryOp(NotEqual,FunctionCall(Variable(Token(Identifier, 'getTile', 161)),List(Variable(Token(Identifier, 'newX', 161)), Variable(Token(Identifier, 'newY', 161)))),Variable(Token(Identifier, 'snakeBodyTile', 161))))
cmp 3, s
beq +
bcs ++
+:	;End of condition (BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newX', 161)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newX', 161)),NumericalLiteral(0))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
pha
lda #0	;Condition: BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newY', 161)),NumericalLiteral(0))
cmp 3, s
beq +
bcs ++
+:	;End of condition (BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newY', 161)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newY', 161)),NumericalLiteral(0))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
and 1, s
sta 1, s
lda #30	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'newX', 161)),NumericalLiteral(30))
cmp 5, s
bcc ++
beq ++	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'newX', 161)),NumericalLiteral(30))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Less,Variable(Token(Identifier, 'newX', 161)),NumericalLiteral(30))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
and 1, s
sta 1, s
lda #20	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'newY', 161)),NumericalLiteral(20))
cmp 3, s
bcc ++
beq ++	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'newY', 161)),NumericalLiteral(20))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Less,Variable(Token(Identifier, 'newY', 161)),NumericalLiteral(20))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
and 1, s
sta 1, s
lda 5, s	;Condition: BinaryOp(NotEqual,FunctionCall(Variable(Token(Identifier, 'getTile', 161)),List(Variable(Token(Identifier, 'newX', 161)), Variable(Token(Identifier, 'newY', 161)))),Variable(Token(Identifier, 'snakeBodyTile', 161)))
pha
lda 5, s
pha
jsl func_getTile
sta 3, s
plx	;Dummy pull
lda 4518
cmp 1, s
beq ++	;End of condition (BinaryOp(NotEqual,FunctionCall(Variable(Token(Identifier, 'getTile', 161)),List(Variable(Token(Identifier, 'newX', 161)), Variable(Token(Identifier, 'newY', 161)))),Variable(Token(Identifier, 'snakeBodyTile', 161)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(NotEqual,FunctionCall(Variable(Token(Identifier, 'getTile', 161)),List(Variable(Token(Identifier, 'newX', 161)), Variable(Token(Identifier, 'newY', 161)))),Variable(Token(Identifier, 'snakeBodyTile', 161)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
plx	;Dummy pull
and 1, s
plx	;Dummy pull
cmp #1
beq JMP_not_taken_to_Else_l161_f0	;End of condition (BinaryOp(And,BinaryOp(And,BinaryOp(And,BinaryOp(And,BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newX', 161)),NumericalLiteral(0)),BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newY', 161)),NumericalLiteral(0))),BinaryOp(Less,Variable(Token(Identifier, 'newX', 161)),NumericalLiteral(30))),BinaryOp(Less,Variable(Token(Identifier, 'newY', 161)),NumericalLiteral(20))),BinaryOp(NotEqual,FunctionCall(Variable(Token(Identifier, 'getTile', 161)),List(Variable(Token(Identifier, 'newX', 161)), Variable(Token(Identifier, 'newY', 161)))),Variable(Token(Identifier, 'snakeBodyTile', 161))))), either branched if IfTrue or fallen through
jmp Else_l161_f0
JMP_not_taken_to_Else_l161_f0:
phx	;Dummy push
lda 5, s	;Condition: BinaryOp(Equal,FunctionCall(Variable(Token(Identifier, 'getTile', 162)),List(Variable(Token(Identifier, 'newX', 162)), Variable(Token(Identifier, 'newY', 162)))),Variable(Token(Identifier, 'fruitTile', 162)))
pha
lda 5, s
pha
jsl func_getTile
sta 3, s
plx	;Dummy pull
lda 4524
cmp 1, s
bne ++	;End of condition (BinaryOp(Equal,FunctionCall(Variable(Token(Identifier, 'getTile', 162)),List(Variable(Token(Identifier, 'newX', 162)), Variable(Token(Identifier, 'newY', 162)))),Variable(Token(Identifier, 'fruitTile', 162)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Equal,FunctionCall(Variable(Token(Identifier, 'getTile', 162)),List(Variable(Token(Identifier, 'newX', 162)), Variable(Token(Identifier, 'newY', 162)))),Variable(Token(Identifier, 'fruitTile', 162)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
sta 3, s
plx	;Dummy pull
cmp #0
beq +
lda #0
bra ++
+:
lda #1
++:
cmp #1
bne Else_l163_f0	;End of condition (UnaryOp(BooleanNegation,Variable(Token(Identifier, 'ateFruit', 163)))), either branched if IfFalse or fallen through
lda 4512
asl A
tax
lda 2104, x
pha
lda 4512
asl A
tax
lda 3304, x
pha
lda 4516
pha
jsl func_setTile
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
Else_l163_f0:
If_End_l163_f0:
lda 5, s
pha
lda 5, s
pha
lda 4520
pha
jsl func_setTile
lda 15, s
sta 5, s
lda 13, s
sta 3, s
lda 4518
sta 1, s
jsl func_setTile
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
lda 4514
inc A
sta 4514	;Storing the assignment
lda 1, s	;Condition: UnaryOp(BooleanNegation,Variable(Token(Identifier, 'ateFruit', 167)))
cmp #0
beq +
lda #0
bra ++
+:
lda #1
++:
cmp #1
bne Else_l167_f0	;End of condition (UnaryOp(BooleanNegation,Variable(Token(Identifier, 'ateFruit', 167)))), either branched if IfFalse or fallen through
lda 4512
inc A
sta 4512	;Storing the assignment
lda #600	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'tailIndex', 169)),NumericalLiteral(600))
cmp 4512
bne Else_l169_f0	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'tailIndex', 169)),NumericalLiteral(600))), either branched if IfFalse or fallen through
stz 4512
Else_l169_f0:
If_End_l169_f0:
lda 4512
asl A
tax
lda 2104, x
pha
lda 4512
asl A
tax
lda 3304, x
pha
lda 4522
pha
jsl func_setTile
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
bra If_End_l167_f0
Else_l167_f0:
jsl func_spawnFruit
If_End_l167_f0:
lda #600	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'headIndex', 175)),NumericalLiteral(600))
cmp 4514
bne Else_l175_f0	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'headIndex', 175)),NumericalLiteral(600))), either branched if IfFalse or fallen through
stz 4514
Else_l175_f0:
If_End_l175_f0:
lda 4514
asl A
tax
lda 5, s
sta 2104, x
lda 4514
asl A
tax
lda 3, s
sta 3304, x
plx	;Dummy pull
bra If_End_l161_f0
Else_l161_f0:
stz 4526
If_End_l161_f0:
lda #0
pha
for_l181_f0:
lda #8	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 181)),NumericalLiteral(8))
cmp 1, s
bcc for_end_l181_f0
beq for_end_l181_f0	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 181)),NumericalLiteral(8))), either branched if IfFalse or fallen through
lda #32
pha
lda #1
pha
jsl func_updatePlayerInput
jsl func_updatePlayerDirectionalOffset
jsl func_frame
plx	;Dummy pull
plx	;Dummy pull
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l181_f0
for_end_l181_f0:
lda 11, s
tcs
rtl

;---------------

func_addGameOverText:
tsx
phx	;Record stack frame
lda #20
sta 272
lda #21
sta 274
lda #22
sta 276
lda #23
sta 278
lda #24
sta 280
lda #25
sta 282
lda #23
sta 284
lda #26
sta 286
plx	;Dummy pull
rtl

;---------------

main_function:
tsx
phx	;Record stack frame
lda #37542
pha
jsl func_setSeed
jsl func_setup
plx	;Dummy pull
While_l203_f0:
While_l204_f0:
lda 4526	;Condition: Variable(Token(Identifier, 'gameRunning', 204))
cmp #1
bne While_End_l204_f0	;End of condition (Variable(Token(Identifier, 'gameRunning', 204))), either branched if IfFalse or fallen through
jsl func_updateFrame
bra While_l204_f0
While_End_l204_f0:
jsl func_addGameOverText
While_l208_f0:
lda 50	;Condition: BinaryOp(Equal,Get(Variable(Token(Identifier, 'playerInput', 208)),Token(Identifier, 'R', 208)),BooleanLiteral(false))
bne While_End_l208_f0	;End of condition (BinaryOp(Equal,Get(Variable(Token(Identifier, 'playerInput', 208)),Token(Identifier, 'R', 208)),BooleanLiteral(false))), either branched if IfFalse or fallen through
lda #32
pha
lda #1
pha
jsl func_updatePlayerInput
jsl func_frame
plx	;Dummy pull
plx	;Dummy pull
bra While_l208_f0
While_End_l208_f0:
jsl func_secondarySetup
lda #1
sta 4526	;Storing the assignment
bra While_l203_f0
While_End_l203_f0:
plx	;Dummy pull
stp	;At the end of main

;---------------

VBlank:
pha
phx
phy
php
lda 6
beq VBlank_End
stz 6
sep #$20
lda #128
sta 8448
rep #$30
tsx
phx	;Record stack frame
jsl func_sendDataToBackground2
plx	;Dummy pull
sep #$20
lda #15
sta 8448
VBlank_End:
plp
ply
plx
pla
rti

;---------------

.bank 1
backgroundSprites:
	.db %11111110 %00000001
	.db %11111110 %00000001
	.db %11111110 %00000001
	.db %11111110 %00000001
	.db %11111110 %00000001
	.db %11111110 %00000001
	.db %11111110 %00000001
	.db %00000001 %11111110
	

	.db %00000000 %00000001
	.db %00000000 %00000001
	.db %00000000 %00000001
	.db %00000000 %00000001
	.db %00000000 %00000001
	.db %00000000 %00000001
	.db %00000000 %00000001
	.db %00000000 %00000000
	

	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %11111110
	

	.db %01111110 %01111110
	.db %01000010 %01000010
	.db %01000000 %01000000
	.db %01111110 %01111110
	.db %00000010 %00000010
	.db %00000010 %00000010
	.db %00000010 %00000010
	.db %01111110 %01111110
	

	.db %01000010 %01000010
	.db %01110010 %01110010
	.db %01010010 %01010010
	.db %01010010 %01010010
	.db %01001010 %01001010
	.db %01001010 %01001010
	.db %01001110 %01001110
	.db %01000010 %01000010
	

	.db %00011000 %00011000
	.db %00111100 %00111100
	.db %01100110 %01100110
	.db %01000010 %01000010
	.db %01111110 %01111110
	.db %01000010 %01000010
	.db %01000010 %01000010
	.db %01000010 %01000010
	

	.db %01000100 %01000100
	.db %01001100 %01001100
	.db %01011000 %01011000
	.db %01110000 %01110000
	.db %01011000 %01011000
	.db %01001100 %01001100
	.db %01000100 %01000100
	.db %01000100 %01000100
	

	.db %01111110 %01111110
	.db %01000000 %01000000
	.db %01000000 %01000000
	.db %01111100 %01111100
	.db %01000000 %01000000
	.db %01000000 %01000000
	.db %01000000 %01000000
	.db %01111110 %01111110
	

	.db %00011000 %00011000
	.db %00011000 %00011000
	.db %00011000 %00011000
	.db %00011000 %00011000
	.db %00011000 %00011000
	.db %00000000 %00000000
	.db %00011000 %00011000
	.db %00011000 %00011000
	

	.db %10000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	

	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	

	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	

	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	

	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	

	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	

	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	

backgroundPalette:
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0100001000010000
	.dw %0111111111111111
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
snakeSprites:
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	

	.db %00001111 %00000000
	.db %00111111 %00000000
	.db %01110011 %00101100
	.db %11101100 %00010011
	.db %11101100 %00010011
	.db %01110011 %00101100
	.db %00111111 %00000000
	.db %00001111 %00000000
	

	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %01111110 %00000000
	.db %01111110 %00000000
	.db %00111100 %00000000
	.db %00011000 %00100100
	.db %00000000 %00011000
	

	.db %00000000 %00100000
	.db %00000000 %00100000
	.db %00101000 %00010000
	.db %01111100 %00000000
	.db %01111100 %00000000
	.db %01111100 %00000000
	.db %01111100 %00000000
	.db %00111000 %00000000
	

	.db %00111100 %00000000
	.db %01111110 %00000000
	.db %01100000 %00000000
	.db %01100000 %00000000
	.db %01101110 %00000000
	.db %01101100 %00000000
	.db %01111100 %00000000
	.db %00111100 %00000000
	

	.db %00011000 %00000000
	.db %00111100 %00000000
	.db %01100110 %00000000
	.db %01100110 %00000000
	.db %01111110 %00000000
	.db %01100110 %00000000
	.db %01100110 %00000000
	.db %01100110 %00000000
	

	.db %01101100 %00000000
	.db %11111110 %00000000
	.db %11111110 %00000000
	.db %11010110 %00000000
	.db %11010110 %00000000
	.db %11000110 %00000000
	.db %11000110 %00000000
	.db %11000110 %00000000
	

	.db %01111110 %00000000
	.db %01111110 %00000000
	.db %01100000 %00000000
	.db %01111100 %00000000
	.db %01111100 %00000000
	.db %01100000 %00000000
	.db %01111110 %00000000
	.db %01111110 %00000000
	

	.db %01111110 %00000000
	.db %01111110 %00000000
	.db %01100110 %00000000
	.db %01100110 %00000000
	.db %01100110 %00000000
	.db %01100110 %00000000
	.db %01111110 %00000000
	.db %01111110 %00000000
	

	.db %01100110 %00000000
	.db %01100110 %00000000
	.db %01100110 %00000000
	.db %01100110 %00000000
	.db %01100110 %00000000
	.db %01111110 %00000000
	.db %00111100 %00000000
	.db %00011000 %00000000
	

	.db %01111100 %00000000
	.db %01111100 %00000000
	.db %01100110 %00000000
	.db %01100110 %00000000
	.db %01111100 %00000000
	.db %01100110 %00000000
	.db %01100110 %00000000
	.db %01100110 %00000000
	

	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	

	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	

	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	

	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	

	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	.db %00000000 %00000000
	

snakePalette:
	.dw %0000000000000000
	.dw %0000000000010110
	.dw %0000001101111111
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
	.dw %0000000000000000
tilemap:
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000C $000C $000C $000C $000C $000C $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $0003 $0004 $0005 $0006 $0007 $0008 $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002
	.dw $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $0002 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $0001 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000
	.dw $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $0000 $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A
	.dw $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A $000A