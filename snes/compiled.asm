
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
jml main_function

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

main_function:
tsx
phx	;Record stack frame
tsc
sec
sbc #6
tcs
;User Assembly
ldx #1
Top:
cpx #1
;End Assembly
lda #50
sta 5, s	;Storing the assignment
lda #7
sta 3, s	;Storing the assignment
lda 5, s
tax
lda 3, s
sep #$10
stx 16900
sta 16902
nop ; :)
nop ; :)
nop ; :)
nop ; :)
nop ; :)
nop ; :)
nop ; :)
nop ; :)
lda 16918
rep #$10
sta 1, s	;Storing the assignment
;User Assembly
tax
;End Assembly
While_l19_f0:
lda #1	;Condition: BooleanLiteral(true)
cmp #1
bne While_End_l19_f0	;End of condition (BooleanLiteral(true)), either branched if IfFalse or fallen through
bra While_l19_f0
While_End_l19_f0:
lda 7, s	;Fix stack before return
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
