addi x8 x0 2
addi x9 x0 2
beq x8 x9 start		#goto start
start:
    addi x9 x8 10
    add x10 x9 x0
    beq x9 x10 eq	#goto eq
uneq: 
	addi x8 x0 2
	addi x9 x0 2
	beq x8 x9 end
eq: 
	addi x9 x9 -20
	beq x9 x10 uneq	#goto uneq
end:
	addi x10 x0 2
