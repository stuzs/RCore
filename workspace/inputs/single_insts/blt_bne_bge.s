addi x8 x0 3
addi x9 x0 2
bne x8 x9 lt		#goto lt
ge:
    addi x2 x0 5
    addi x1 x0 1
    bge x2 x1 end	#goto end
lt: 
	addi x5 x0 2
	addi x6 x0 3
	blt x5 x6 ge    #ge
end:
	addi x7 x0 9
