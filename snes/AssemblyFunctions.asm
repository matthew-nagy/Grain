jmp AfterAssemblyFunctions

signed_16x16_multiplication:
sep #$20
sta $211c
xba
ldx $2134
stx 10
ldx $2135
stx 11

sta $211c
lda $2136
sta 13
rep #$20
lda $2134
clc
adc 11
bcc AFTERINC

inc 12

AFTERINC
sta 11
lda 10
rtl

signed_16x16_division:
lda 18
sta 22
stz 24
lda 20
bne +
sec
rts		//set carry to indicate divide by zero error
+;
cmp #$0100
bcc divide_by_8_bit
-;
lsr                 // Divide numerator by 2,
adc #$0000          // adding carry flag into itself.
lsr 22              // Divide denominator by 2
cmp #$0100          // until it is under 256.
bcs -
ldx 22
stx $4204           // WRDIVL
sep #$20            // 8-bit accumulator
sta $4206           // WRDIVB
nop
nop
nop
nop
nop
nop
nop
lda $4214           // RDDIVL
sta 24
sta $4202           // WRMPYA
lda 20
sta $4203           // WRMPYB
lda 21
nop
ldx $4216           // RDMPYL
sta $4203           // WRMPYB
stx 22
lda 23
clc
adc $4216           // RDMPYL
sta 23
rep #$20            // 16-bit accumulator
lda 18
sec
sbc 22
-;
cmp 20
bcc +
sbc 20
inc 24
bra -
+;
rts                     //should end with carry clear to indicate valid answer
divide_by_8_bit:
ldx 22
stx $4204           // WRDIVL
sep #$20
sta $4206           // WRDIVB
nop
nop
nop
nop
nop
rep #$21		//clear carry to indicate valid answer
lda $4214
rtl

AfterAssemblyFunctions: