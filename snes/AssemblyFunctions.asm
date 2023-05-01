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


AfterAssemblyFunctions: