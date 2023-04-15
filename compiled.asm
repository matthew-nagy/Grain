
;User Assembly
.include "Header.inc"
.include "Snes_Init.asm"
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
sta 3348	;Storing the assignment
lda #2
sta 3350	;Storing the assignment
lda #15
sta 3352	;Storing the assignment
lda #16
sta 3354	;Storing the assignment
lda #17
sta 3356	;Storing the assignment
lda #18
sta 3358	;Storing the assignment
lda #19
sta 3360	;Storing the assignment
jml main_function

;---------------

func_setVRamTarget:
;User Assembly
lda 4, s
sta $2116
rtl
;End Assembly

;---------------

func_dmaToVRam:
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

func_sendDataToBackground2:
tsx
phx	;Record stack frame
lda #2048
pha
jsl func_setVRamTarget
plx	;Dummy pull
lda #100
pha
lda #0
pha
lda #2048
pha
jsl func_dmaToVRam
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
lda #294
sta 5, s	;Storing the assignment
lda #0
sta 3, s	;Storing the assignment
lda #3
sta 1, s	;Storing the assignment
for_l32:
lda #6	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 32)),NumericalLiteral(6))
cmp 3, s
bcc for_end_l32
beq for_end_l32	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 32)),NumericalLiteral(6))), either branched if IfFalse or fallen through
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
bra for_l32
for_end_l32:
lda 7, s	;Fix stack before return
tcs
rtl

;---------------

func_initBackground1:
tsx
phx	;Record stack frame
phx	;Dummy push
phx	;Dummy push
lda #0
pha
for_l40:
lda #160	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 40)),NumericalLiteral(160))
cmp 1, s
bcc for_end_l40
beq for_end_l40	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 40)),NumericalLiteral(160))), either branched if IfFalse or fallen through
lda 1, s
asl A
tax
lda 3352
sta 100, x
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l40
for_end_l40:
plx	;Dummy pull
lda #420
sta 3, s	;Storing the assignment
ldy #0
lda 3352
sta (3, s), y
lda #1
pha
for_l45:
lda #31	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 45)),NumericalLiteral(31))
cmp 1, s
bcc for_end_l45
beq for_end_l45	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 45)),NumericalLiteral(31))), either branched if IfFalse or fallen through
lda 1, s
asl A
tay
lda #2
sta (5, s), y
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l45
for_end_l45:
plx	;Dummy pull
ldy #62
lda 3352
sta (3, s), y
lda #6
sta 1, s	;Storing the assignment
for_l49:
lda #26	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'y', 49)),NumericalLiteral(26))
cmp 1, s
bcc for_end_l49
beq for_end_l49	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'y', 49)),NumericalLiteral(26))), either branched if IfFalse or fallen through
phx	;Dummy push
lda 3, s
asl A
asl A
asl A
asl A
asl A
asl A
clc
adc #100
sta 1, s	;Storing the assignment
ldy #0
lda #1
sta (1, s), y
ldy #62
lda 3352
sta (1, s), y
lda #1
pha
for_l53:
lda #31	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'x', 53)),NumericalLiteral(31))
cmp 1, s
bcc for_end_l53
beq for_end_l53	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'x', 53)),NumericalLiteral(31))), either branched if IfFalse or fallen through
lda 1, s
asl A
tay
lda #0
sta (3, s), y
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l53
for_end_l53:
plx	;Dummy pull
plx	;Dummy pull
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l49
for_end_l49:
for_l55:
lda #32	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'y', 55)),NumericalLiteral(32))
cmp 1, s
bcc for_end_l55
beq for_end_l55	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'y', 55)),NumericalLiteral(32))), either branched if IfFalse or fallen through
lda #0
pha
for_l56:
lda #32	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'x', 56)),NumericalLiteral(32))
cmp 1, s
bcc for_end_l56
beq for_end_l56	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'x', 56)),NumericalLiteral(32))), either branched if IfFalse or fallen through
lda 3, s
asl A
asl A
asl A
asl A
asl A
asl A
clc
adc #100
pha
lda 3, s
asl A
clc
adc 1, s
plx	;Dummy pull
tax
lda 3352
sta 0, x
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l56
for_end_l56:
plx	;Dummy pull
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l55
for_end_l55:
jsl func_detailBackground1
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
lda 5, s	;Fix stack before return
tcs
rtl

;---------------

func_initSnake:
tsx
phx	;Record stack frame
lda #0
pha
for_l64:
lda #1024	;Condition: BinaryOp(Less,Variable(Token(Identifier, 'i', 64)),NumericalLiteral(1024))
cmp 1, s
bcc for_end_l64
beq for_end_l64	;End of condition (BinaryOp(Less,Variable(Token(Identifier, 'i', 64)),NumericalLiteral(1024))), either branched if IfFalse or fallen through
lda 1, s
asl A
tax
lda 3352
sta 100, x
lda 1, s
inc A
sta 1, s	;Storing the assignment
bra for_l64
for_end_l64:
plx	;Dummy pull
xba
and #65280
pha
lda #11
ora 1, s
sta 1, s
pla
sta 2148
xba
and #65280
pha
lda #10
ora 1, s
sta 1, s
pla
sta 2150
xba
and #65280
pha
lda #9
ora 1, s
sta 1, s
pla
sta 2152
lda 3358
sta 1402
lda 3354
sta 1400
lda 3356
sta 1398
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
jsl func_dmaToVRam
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
jsl func_dmaToVRam
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
jsl func_initBackground1
lda #100
pha
lda #0
pha
lda #2048
pha
jsl func_dmaToVRam
plx	;Dummy pull
plx	;Dummy pull
plx	;Dummy pull
jsl func_executeDMA
jsl func_initSnake
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

main_function:
tsx
phx	;Record stack frame
jsl func_setup
While_l110:
lda #1	;Condition: BooleanLiteral(true)
cmp #1
bne While_End_l110	;End of condition (BooleanLiteral(true)), either branched if IfFalse or fallen through
jsl func_frame
bra While_l110
While_End_l110:
lda 1, s	;Fix stack before return
tcs
stp	;At the end of main

;---------------

VBlank:
pha
phx
phy
php
rep #$30
lda 6
beq VBlank_End
stz 6
tsx
phx	;Record stack frame
jsl func_sendDataToBackground2
lda 1, s	;Fix stack before return
tcs
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