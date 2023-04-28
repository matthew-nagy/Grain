
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
lda #1
sta 2080	;Storing the assignment
inc A
sta 2082	;Storing the assignment
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

func_setup:
tsx
phx	;Record stack frame
lda #1
pha
dec A
pha
jsl func_setScreenDisabledAndBrightness
lda #graphics
sta 3, s
lda #1
sta 1, s
lda #256
pha
jsl func_dmaGraphicsToVRam
lda #palette
sta 5, s
lda #1
sta 3, s
lda #256
sta 1, s
jsl func_dmaToCGRam
jsl func_executeDMA
lda #1
sta 5, s
sta 3, s
dec A
sta 1, s
jsl func_setBackgroundAddressAndSize
lda #1
sta 5, s
dec A
sta 3, s
sta 1, s
pha
pha
jsl func_enableScreens
jsl func_enableInputAndNMI
lda #0
sta 3, s
lda #15
sta 1, s
jsl func_setScreenDisabledAndBrightness
lda #0
sta 9, s
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
for_l24_f0:
lda #1024	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 24)),NumericalLiteral(1024))
cmp 1, s
bcc for_end_l24_f0
beq for_end_l24_f0	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 24)),NumericalLiteral(1024))), either branched if IfFalse or fallen through
lda 1, s
asl A
tax
lda #0
sta 32, x
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l24_f0
for_end_l24_f0:
lda 3, s
tcs
rtl

;---------------

func_aackermann:
;User Assembly
.Block "asmAckermann"
lda 6, s
beq mZero
tax
dex
lda 4, s
beq nZero
phx
inx
phx
dec A
pha
jsl func_aackermann
plx
sta 1, s
jsl func_aackermann
plx
plx
rtl
mZero:
lda 4, s
inc A
rtl
nZero:
phx
lda #1
pha
jsl func_aackermann
plx
plx
rtl
.Endb
;End Assembly

;---------------

func_ackermann:
.Block "ack"
tsx
phx	;Record stack frame
lda 8, s	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'm', 106)),NumericalLiteral(0))
bne Else_l106_f0	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'm', 106)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda 6, s
inc A
plx	;Dummy pull
rtl
Else_l106_f0:
If_End_l106_f0:
lda 6, s	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'n', 107)),NumericalLiteral(0))
bne Else_l107_f0	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'n', 107)),NumericalLiteral(0))), either branched if IfFalse or fallen through
lda 8, s
dec A
pha
lda #1
pha
jsl func_ackermann
tay
lda 5, s
tcs
tya
rtl
Else_l107_f0:
If_End_l107_f0:
lda 8, s
dec A
pha
lda 10, s
pha
lda 10, s
dec A
pha
jsl func_ackermann
sta 3, s
plx	;Dummy pull
jsl func_ackermann
tay
lda 5, s
tcs
tya
rtl
.Endb

;---------------

func_test_ackermann:
tsx
phx	;Record stack frame
lda #0
pha
for_l112_f0:
lda #1024	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 112)),NumericalLiteral(1024))
cmp 1, s
bcc for_end_l112_f0
beq for_end_l112_f0	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 112)),NumericalLiteral(1024))), either branched if IfFalse or fallen through
lda #3
pha
pha
jsl func_ackermann
sta 3, s
plx	;Dummy pull
;User Assembly
sta 10
;End Assembly
lda #61	;Condition: BinaryOp(Equal,Variable(Token(Identifier, 'val', 118)),NumericalLiteral(61))
cmp 1, s
bne Else_l118_f0	;End of condition (BinaryOp(Equal,Variable(Token(Identifier, 'val', 118)),NumericalLiteral(61))), either branched if IfFalse or fallen through
lda 3, s
asl A
tax
lda 2082
sta 32, x
bra If_End_l118_f0
Else_l118_f0:
lda 3, s
asl A
tax
lda 2080
sta 32, x
If_End_l118_f0:
;User Assembly
lda #1
sta 6
;End Assembly
plx	;Dummy pull
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l112_f0
for_end_l112_f0:
lda 3, s
tcs
rtl

;---------------

func_fib:
tsx
phx	;Record stack frame
phx	;Dummy push
phx	;Dummy push
lda #0
sta 3, s	;Storing the assignment
inc A
sta 1, s	;Storing the assignment
dec A
pha
for_l130_f0:
lda 12, s	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 130)),Variable(Token(Identifier, 'num', 130)))
cmp 1, s
bcc for_end_l130_f0
beq for_end_l130_f0	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 130)),Variable(Token(Identifier, 'num', 130)))), either branched if IfFalse or fallen through
lda 7, s
pha
lda 5, s
clc
adc 7, s
sta 5, s	;Storing the assignment
lda 1, s
sta 7, s	;Storing the assignment
plx	;Dummy pull
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l130_f0
for_end_l130_f0:
plx	;Dummy pull
lda 1, s
tay
lda 5, s
tcs
tya
rtl

;---------------

main_function:
tsx
phx	;Record stack frame
jsl func_setup
jsl func_test_ackermann
While_l166_f0:
jsl func_frame
bra While_l166_f0
While_End_l166_f0:
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
lda #1024
pha
jsl func_setVRamTarget
lda #32
sta 1, s
lda #0
pha
lda #2048
pha
jsl func_dmaArrayToVRam
jsl func_executeDMA
lda 7, s
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
graphics:
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	.db %11111111 %00000000
	

	.db %00000000 %11111111
	.db %00000000 %11111111
	.db %00000000 %11111111
	.db %00000000 %11111111
	.db %00000000 %11111111
	.db %00000000 %11111111
	.db %00000000 %11111111
	.db %00000000 %11111111
	

	.db %11111111 %11111111
	.db %11111111 %11111111
	.db %11111111 %11111111
	.db %11111111 %11111111
	.db %11111111 %11111111
	.db %11111111 %11111111
	.db %11111111 %11111111
	.db %11111111 %11111111
	

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
	

palette:
	.dw %0000000000000000
	.dw %0111110010000000
	.dw %0000000000011111
	.dw %0001001111100000
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