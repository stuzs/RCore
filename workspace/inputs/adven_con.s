    addi x8, x0, 0
    addi x9, x0, 8
start:
    addi x8, x8, 1
    bne x8, x9, start	#goto start
end:
    addi x10, x0, 1
