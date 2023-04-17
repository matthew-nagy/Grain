
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
lda #0
sta 4580	;Storing the assignment
lda #2
sta 4582	;Storing the assignment
lda #15
sta 4584	;Storing the assignment
lda #16
sta 4586	;Storing the assignment
lda #17
sta 4588	;Storing the assignment
lda #18
sta 4590	;Storing the assignment
lda #19
sta 4592	;Storing the assignment
lda #1
sta 4594	;Storing the assignment
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
plx	;Dummy pull
sta 1, s	;Storing the assignment
lda #0
pha
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
lda #32768	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 68)),NumericalLiteral(32768)),NumericalLiteral(0))
and 1, s
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
lda #16384	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 69)),NumericalLiteral(16384)),NumericalLiteral(0))
and 1, s
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
lda #8192	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 70)),NumericalLiteral(8192)),NumericalLiteral(0))
and 1, s
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
lda #4096	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 71)),NumericalLiteral(4096)),NumericalLiteral(0))
and 1, s
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
lda #2048	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 72)),NumericalLiteral(2048)),NumericalLiteral(0))
and 1, s
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
lda #1024	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 73)),NumericalLiteral(1024)),NumericalLiteral(0))
and 1, s
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
lda #512	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 74)),NumericalLiteral(512)),NumericalLiteral(0))
and 1, s
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
lda #256	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 75)),NumericalLiteral(256)),NumericalLiteral(0))
and 1, s
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
lda #128	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 76)),NumericalLiteral(128)),NumericalLiteral(0))
and 1, s
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
lda #64	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 77)),NumericalLiteral(64)),NumericalLiteral(0))
and 1, s
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
lda #32	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 78)),NumericalLiteral(32)),NumericalLiteral(0))
and 1, s
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
lda #16	;Condition: BinaryOp(Greater,BinaryOp(And,Variable(Token(Identifier, 'inputFlags', 79)),NumericalLiteral(16)),NumericalLiteral(0))
and 1, s
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
plx	;Dummy pull
lda 3, s	;Fix stack before return
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
lda #5
clc
adc 8, s
sta 8, s
asl A
asl A
asl A
asl A
asl A
asl A
clc
adc #124
pha
lda 12, s
asl A
clc
adc 1, s
plx	;Dummy pull
tax
lda 6, s
sta 0, x
lda 1, s	;Fix stack before return
tcs
rtl

;---------------

func_getTile:
tsx
phx	;Record stack frame
lda 8, s
inc A
sta 8, s	;Storing the assignment
lda #5
clc
adc 6, s
sta 6, s
asl A
asl A
asl A
asl A
asl A
asl A
clc
adc #124
pha
lda 10, s
asl A
clc
adc 1, s
plx	;Dummy pull
tax
lda 0, x
tay
lda 1, s
tcs
tya
rtl
lda 1, s	;Fix stack before return
tcs
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
plx	;Dummy pull
plx	;Dummy pull
sta 1, s	;Storing the assignment
lda 4588	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeHeadTile', 50)))
cmp 1, s
bne ++	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeHeadTile', 50)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeHeadTile', 50)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
pha
lda 4586	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeBodyTile', 50)))
cmp 3, s
bne ++	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeBodyTile', 50)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeBodyTile', 50)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
ora 1, s
sta 1, s
lda 4590	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeTailTile', 50)))
cmp 3, s
bne ++	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeTailTile', 50)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Equal,Variable(Token(Identifier, 'atTileID', 50)),Variable(Token(Identifier, 'snakeTailTile', 50)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
ora 1, s
plx	;Dummy pull
tay
lda 3, s
tcs
tya
rtl
lda 3, s	;Fix stack before return
tcs
rtl

;---------------

func_spawnFruit:
tsx
phx	;Record stack frame
phx	;Dummy push
phx	;Dummy push
jsl func_random
pha
lda #31
and 1, s
plx	;Dummy pull
sta 3, s	;Storing the assignment
jsl func_random
pha
lda #31
and 1, s
plx	;Dummy pull
sta 1, s	;Storing the assignment
lda #30	;Condition: BinaryOp(GreaterEqual,Variable(Token(Identifier, 'x', 56)),NumericalLiteral(30))
cmp 3, s
beq +
bcs Else_l56_f0
+:	;End of condition (BinaryOp(GreaterEqual,Variable(Token(Identifier, 'x', 56)),NumericalLiteral(30))), either branched if IfFalse or fallen through
lda #10
sta 3, s	;Storing the assignment
Else_l56_f0:
If_End_l56_f0:
lda #20	;Condition: BinaryOp(GreaterEqual,Variable(Token(Identifier, 'y', 57)),NumericalLiteral(20))
cmp 1, s
beq +
bcs Else_l57_f0
+:	;End of condition (BinaryOp(GreaterEqual,Variable(Token(Identifier, 'y', 57)),NumericalLiteral(20))), either branched if IfFalse or fallen through
lda #16
pha
lda 3, s
sec
sbc 1, s
plx	;Dummy pull
sta 1, s	;Storing the assignment
Else_l57_f0:
If_End_l57_f0:
lda 3, s	;Condition: BinaryOp(Or,FunctionCall(Variable(Token(Identifier, 'isSnakeTile', 59)),List(Variable(Token(Identifier, 'x', 59)), Variable(Token(Identifier, 'y', 59)))),BinaryOp(And,BinaryOp(Equal,Variable(Token(Identifier, 'x', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'x', 59))),BinaryOp(Equal,Variable(Token(Identifier, 'y', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'y', 59)))))
pha
lda 3, s
pha
jsl func_isSnakeTile
plx	;Dummy pull
sta 1, s
lda 4576	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'x', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'x', 59)))
cmp 5, s
bne ++	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'x', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'x', 59)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Equal,Variable(Token(Identifier, 'x', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'x', 59)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
pha
lda 4578	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'y', 59)),Get(Variable(Token(Identifier, 'lastFruit', 59)),Token(Identifier, 'y', 59)))
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
lda 4592
pha
jsl func_setTile
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
lda 3, s
sta 4576	;Assigning simple getter
lda 1, s
sta 4578	;Assigning simple getter
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
plx	;Dummy pull
lda #124
pha
lda #0
pha
lda #2048
pha
jsl func_dmaArrayToVRam
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
jsl func_executeDMA
lda 1, s	;Fix stack before return
tcs
rtl

;---------------

func_detailBackground1:
tsx
phx	;Record stack frame
tsc
sec
sbc #6
tcs
lda #318
sta 5, s	;Storing the assignment
lda #0
sta 3, s	;Storing the assignment
lda #3
sta 1, s	;Storing the assignment
for_l80_f0:
lda #6	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 80)),NumericalLiteral(6))
cmp 3, s
bcc for_end_l80_f0
beq for_end_l80_f0	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 80)),NumericalLiteral(6))), either branched if IfFalse or fallen through
lda 3, s
asl A
tay
lda 1, s
sta (5, s), y
lda 1, s
inc A
sta 1, s	;Storing the assignment
lda 3, s
inc A
sta 3, s	;Storing the assignment
bra for_l80_f0
for_end_l80_f0:
lda 7, s	;Fix stack before return
tcs
rtl

;---------------

func_initSnake:
tsx
phx	;Record stack frame
lda #0
pha
for_l112_f0:
lda #1024	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 112)),NumericalLiteral(1024))
cmp 1, s
bcc for_end_l112_f0
beq for_end_l112_f0	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 112)),NumericalLiteral(1024))), either branched if IfFalse or fallen through
lda 1, s
asl A
tax
lda 4584
sta 124, x
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l112_f0
for_end_l112_f0:
lda #0
sta 1, s	;Storing the assignment
for_l114_f0:
lda #1200	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 114)),NumericalLiteral(1200))
cmp 1, s
bcc for_end_l114_f0
beq for_end_l114_f0	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 114)),NumericalLiteral(1200))), either branched if IfFalse or fallen through
lda 1, s
asl A
tax
lda #0
sta 2172, x
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l114_f0
for_end_l114_f0:
plx	;Dummy pull
lda #20
sta 2172
lda #20
sta 2174
lda #20
sta 2176
lda #11
sta 3372
lda #10
sta 3374
lda #9
sta 3376
lda #20
pha
lda #11
pha
lda 4590
pha
jsl func_setTile
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
lda #20
pha
lda #10
pha
lda 4586
pha
jsl func_setTile
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
lda #20
pha
lda #9
pha
lda 4588
pha
jsl func_setTile
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
lda #0
sta 4572	;Assigning simple getter
lda #-1
sta 4574	;Assigning simple getter
lda #1
pha
lda #2
pha
lda #0
pha
jsl func_setBackgroundAddressAndSize
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
lda 1, s	;Fix stack before return
tcs
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
plx	;Dummy pull
plx	;Dummy pull
lda #backgroundSprites
pha
lda #1
pha
lda #256
pha
jsl func_dmaGraphicsToVRam
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
lda #snakePalette
pha
lda #1
pha
lda #64
pha
jsl func_dmaToCGRam
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
jsl func_executeDMA
lda #snakeSprites
pha
lda #1
pha
lda #256
pha
jsl func_dmaGraphicsToVRam
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
lda #backgroundPalette
pha
lda #1
pha
lda #32
pha
jsl func_dmaToCGRam
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
jsl func_executeDMA
lda #2272
pha
jsl func_setBackgroundColour
plx	;Dummy pull
lda #1024
pha
jsl func_setVRamTarget
plx	;Dummy pull
lda #2
pha
lda #1
pha
lda #0
pha
jsl func_setBackgroundAddressAndSize
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
lda #tilemap
pha
lda #1
pha
lda #2016
pha
jsl func_dmaDataToVRam
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
jsl func_executeDMA
jsl func_initSnake
lda #-1
sta 4576	;Assigning simple getter
lda #-1
sta 4578	;Assigning simple getter
jsl func_spawnFruit
jsl func_sendDataToBackground2
lda #1
pha
pha
lda #0
pha
pha
lda #0
pha
jsl func_enableScreens
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
jsl func_enableInputAndNMI
lda #0
pha
lda #15
pha
jsl func_setScreenDisabledAndBrightness
plx	;Dummy pull
plx	;Dummy pull
lda 1, s	;Fix stack before return
tcs
rtl

;---------------

func_updatePlayerDirectionalOffset:
tsx
phx	;Record stack frame
lda 100	;Condition: Get(Variable(Token(Identifier, 'playerInput', 164)),Token(Identifier, 'Left', 164))
cmp #1
bne Else_l164_f0	;End of condition (Get(Variable(Token(Identifier, 'playerInput', 164)),Token(Identifier, 'Left', 164))), either branched if IfFalse or fallen through
lda #-1
sta 4572	;Assigning simple getter
lda #0
sta 4574	;Assigning simple getter
bra If_End_l164_f0
Else_l164_f0:
lda 102	;Condition: Get(Variable(Token(Identifier, 'playerInput', 168)),Token(Identifier, 'Right', 168))
cmp #1
bne Else_l168_f0	;End of condition (Get(Variable(Token(Identifier, 'playerInput', 168)),Token(Identifier, 'Right', 168))), either branched if IfFalse or fallen through
lda #1
sta 4572	;Assigning simple getter
lda #0
sta 4574	;Assigning simple getter
bra If_End_l168_f0
Else_l168_f0:
lda 104	;Condition: Get(Variable(Token(Identifier, 'playerInput', 172)),Token(Identifier, 'Up', 172))
cmp #1
bne Else_l172_f0	;End of condition (Get(Variable(Token(Identifier, 'playerInput', 172)),Token(Identifier, 'Up', 172))), either branched if IfFalse or fallen through
lda #0
sta 4572	;Assigning simple getter
lda #-1
sta 4574	;Assigning simple getter
bra If_End_l172_f0
Else_l172_f0:
lda 106	;Condition: Get(Variable(Token(Identifier, 'playerInput', 176)),Token(Identifier, 'Down', 176))
cmp #1
bne Else_l176_f0	;End of condition (Get(Variable(Token(Identifier, 'playerInput', 176)),Token(Identifier, 'Down', 176))), either branched if IfFalse or fallen through
lda #0
sta 4572	;Assigning simple getter
lda #1
sta 4574	;Assigning simple getter
Else_l176_f0:
If_End_l176_f0:
If_End_l172_f0:
If_End_l168_f0:
If_End_l164_f0:
lda 1, s	;Fix stack before return
tcs
rtl

;---------------

func_updateFrame:
tsx
phx	;Record stack frame
tsc
sec
sbc #8
tcs
lda #100
pha
lda #1
pha
jsl func_updatePlayerInput
plx	;Dummy pull
plx	;Dummy pull
jsl func_updatePlayerDirectionalOffset
lda 4582
asl A
tax
lda 2172, x
sta 7, s	;Storing the assignment
lda 4582
asl A
tax
lda 3372, x
sta 5, s	;Storing the assignment
lda 4572
clc
adc 7, s
sta 3, s	;Storing the assignment
lda 4574
clc
adc 5, s
sta 1, s	;Storing the assignment
lda #0	;Condition: BinaryOp(And,BinaryOp(And,BinaryOp(And,BinaryOp(And,BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newX', 191)),NumericalLiteral(0)),BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newY', 191)),NumericalLiteral(0))),BinaryOp(Less,Variable(Token(Identifier, 'newX', 191)),NumericalLiteral(30))),BinaryOp(Less,Variable(Token(Identifier, 'newY', 191)),NumericalLiteral(20))),BinaryOp(NotEqual,FunctionCall(Variable(Token(Identifier, 'getTile', 191)),List(Variable(Token(Identifier, 'newX', 191)), Variable(Token(Identifier, 'newY', 191)))),Variable(Token(Identifier, 'snakeBodyTile', 191))))
cmp 3, s
beq +
bcs ++
+:	;End of condition (BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newX', 191)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newX', 191)),NumericalLiteral(0))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
pha
lda #0	;Condition: BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newY', 191)),NumericalLiteral(0))
cmp 3, s
beq +
bcs ++
+:	;End of condition (BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newY', 191)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newY', 191)),NumericalLiteral(0))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
and 1, s
sta 1, s
lda #30	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'newX', 191)),NumericalLiteral(30))
cmp 5, s
bcc ++
beq ++	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'newX', 191)),NumericalLiteral(30))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Less,Variable(Token(Identifier, 'newX', 191)),NumericalLiteral(30))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
and 1, s
sta 1, s
lda #20	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'newY', 191)),NumericalLiteral(20))
cmp 3, s
bcc ++
beq ++	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'newY', 191)),NumericalLiteral(20))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Less,Variable(Token(Identifier, 'newY', 191)),NumericalLiteral(20))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
and 1, s
sta 1, s
lda 5, s	;Condition: BinaryOp(NotEqual,FunctionCall(Variable(Token(Identifier, 'getTile', 191)),List(Variable(Token(Identifier, 'newX', 191)), Variable(Token(Identifier, 'newY', 191)))),Variable(Token(Identifier, 'snakeBodyTile', 191)))
pha
lda 5, s
pha
jsl func_getTile
plx	;Dummy pull
sta 1, s
lda 4586
cmp 1, s
beq ++	;End of condition (BinaryOp(NotEqual,FunctionCall(Variable(Token(Identifier, 'getTile', 191)),List(Variable(Token(Identifier, 'newX', 191)), Variable(Token(Identifier, 'newY', 191)))),Variable(Token(Identifier, 'snakeBodyTile', 191)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(NotEqual,FunctionCall(Variable(Token(Identifier, 'getTile', 191)),List(Variable(Token(Identifier, 'newX', 191)), Variable(Token(Identifier, 'newY', 191)))),Variable(Token(Identifier, 'snakeBodyTile', 191)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
plx	;Dummy pull
and 1, s
plx	;Dummy pull
cmp #1
beq JMP_not_taken_to_Else_l191_f0	;End of condition (BinaryOp(And,BinaryOp(And,BinaryOp(And,BinaryOp(And,BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newX', 191)),NumericalLiteral(0)),BinaryOp(GreaterEqual,Variable(Token(Identifier, 'newY', 191)),NumericalLiteral(0))),BinaryOp(Less,Variable(Token(Identifier, 'newX', 191)),NumericalLiteral(30))),BinaryOp(Less,Variable(Token(Identifier, 'newY', 191)),NumericalLiteral(20))),BinaryOp(NotEqual,FunctionCall(Variable(Token(Identifier, 'getTile', 191)),List(Variable(Token(Identifier, 'newX', 191)), Variable(Token(Identifier, 'newY', 191)))),Variable(Token(Identifier, 'snakeBodyTile', 191))))), either branched if IfTrue or fallen through
jmp Else_l191_f0
JMP_not_taken_to_Else_l191_f0:
phx	;Dummy push
lda 5, s	;Condition: BinaryOp(Equal,FunctionCall(Variable(Token(Identifier, 'getTile', 192)),List(Variable(Token(Identifier, 'newX', 192)), Variable(Token(Identifier, 'newY', 192)))),Variable(Token(Identifier, 'fruitTile', 192)))
pha
lda 5, s
pha
jsl func_getTile
plx	;Dummy pull
sta 1, s
lda 4592
cmp 1, s
bne ++	;End of condition (BinaryOp(Equal,FunctionCall(Variable(Token(Identifier, 'getTile', 192)),List(Variable(Token(Identifier, 'newX', 192)), Variable(Token(Identifier, 'newY', 192)))),Variable(Token(Identifier, 'fruitTile', 192)))), either branched if IfFalse or fallen through
lda #1	;BinaryOp(Equal,FunctionCall(Variable(Token(Identifier, 'getTile', 192)),List(Variable(Token(Identifier, 'newX', 192)), Variable(Token(Identifier, 'newY', 192)))),Variable(Token(Identifier, 'fruitTile', 192)))is true
bra +++
++:
lda #0	;Its false
+++:	;End Binary check, clear stack beneath
plx	;Dummy pull
sta 1, s
cmp #0
beq +
lda #0
bra ++
+:
lda #1
++:
cmp #1
bne Else_l193_f0	;End of condition (UnaryOp(BooleanNegation,Variable(Token(Identifier, 'ateFruit', 193)))), either branched if IfFalse or fallen through
lda 4580
asl A
tax
lda 2172, x
pha
lda 4580
asl A
tax
lda 3372, x
pha
lda 4584
pha
jsl func_setTile
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
Else_l193_f0:
If_End_l193_f0:
lda 5, s
pha
lda 5, s
pha
lda 4588
pha
jsl func_setTile
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
lda 9, s
pha
lda 9, s
pha
lda 4586
pha
jsl func_setTile
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
inc 4582
lda 1, s	;Condition: UnaryOp(BooleanNegation,Variable(Token(Identifier, 'ateFruit', 197)))
cmp #0
beq +
lda #0
bra ++
+:
lda #1
++:
cmp #1
bne Else_l197_f0	;End of condition (UnaryOp(BooleanNegation,Variable(Token(Identifier, 'ateFruit', 197)))), either branched if IfFalse or fallen through
inc 4580
lda #600	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'tailIndex', 199)),NumericalLiteral(600))
cmp 4580
bne Else_l199_f0	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'tailIndex', 199)),NumericalLiteral(600))), either branched if IfFalse or fallen through
lda #0
sta 4580	;Storing the assignment
Else_l199_f0:
If_End_l199_f0:
lda 4580
asl A
tax
lda 2172, x
pha
lda 4580
asl A
tax
lda 3372, x
pha
lda 4590
pha
jsl func_setTile
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
bra If_End_l197_f0
Else_l197_f0:
jsl func_spawnFruit
If_End_l197_f0:
lda #600	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'headIndex', 205)),NumericalLiteral(600))
cmp 4582
bne Else_l205_f0	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'headIndex', 205)),NumericalLiteral(600))), either branched if IfFalse or fallen through
lda #0
sta 4582	;Storing the assignment
Else_l205_f0:
If_End_l205_f0:
lda 4582
asl A
tax
lda 5, s
sta 2172, x
lda 4582
asl A
tax
lda 3, s
sta 3372, x
plx	;Dummy pull
bra If_End_l191_f0
Else_l191_f0:
lda #0
sta 4594	;Storing the assignment
If_End_l191_f0:
lda #0
pha
for_l211_f0:
lda #8	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 211)),NumericalLiteral(8))
cmp 1, s
bcc for_end_l211_f0
beq for_end_l211_f0	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 211)),NumericalLiteral(8))), either branched if IfFalse or fallen through
lda #100
pha
lda #1
pha
jsl func_updatePlayerInput
plx	;Dummy pull
plx	;Dummy pull
jsl func_updatePlayerDirectionalOffset
jsl func_frame
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l211_f0
for_end_l211_f0:
plx	;Dummy pull
lda 9, s	;Fix stack before return
tcs
rtl

;---------------

func_addGameOverText:
tsx
phx	;Record stack frame
lda #20
sta 340
lda #21
sta 342
lda #22
sta 344
lda #23
sta 346
lda #24
sta 348
lda #25
sta 350
lda #23
sta 352
lda #26
sta 354
lda 1, s	;Fix stack before return
tcs
rtl

;---------------

main_function:
tsx
phx	;Record stack frame
lda #37542
pha
jsl func_setSeed
plx	;Dummy pull
While_l230_f0:
lda #1	;Condition: BooleanLiteral(true)
cmp #1
bne While_End_l230_f0	;End of condition (BooleanLiteral(true)), either branched if IfFalse or fallen through
jsl func_setup
While_l234_f0:
lda 4594	;Condition: Variable(Token(Identifier, 'gameRunning', 234))
cmp #1
bne While_End_l234_f0	;End of condition (Variable(Token(Identifier, 'gameRunning', 234))), either branched if IfFalse or fallen through
jsl func_updateFrame
bra While_l234_f0
While_End_l234_f0:
jsl func_addGameOverText
While_l238_f0:
lda 118	;Condition: BinaryOp(Equal,Get(Variable(Token(Identifier, 'playerInput', 238)),Token(Identifier, 'R', 238)),BooleanLiteral(false))
pha
lda #0
cmp 1, s
bne While_End_l238_f0	;End of condition (BinaryOp(Equal,Get(Variable(Token(Identifier, 'playerInput', 238)),Token(Identifier, 'R', 238)),BooleanLiteral(false))), either branched if IfFalse or fallen through
plx	;Dummy pull
lda #100
pha
lda #1
pha
jsl func_updatePlayerInput
plx	;Dummy pull
plx	;Dummy pull
jsl func_frame
bra While_l238_f0
While_End_l238_f0:
plx	;Dummy pull
bra While_l230_f0
While_End_l230_f0:
lda 1, s	;Fix stack before return
tcs
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
lda 1, s	;Fix stack before return
tcs
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