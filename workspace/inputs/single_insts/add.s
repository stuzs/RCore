addi x5, x0, 2047
addi x6, x0, 0x3
addi x7, x0, 2047
add x2, x0, x6    #normal addition
add x8, x6, x7    #normal addition
add x1, x2, x7    #normal addition
add x9, x5, x6    #overflow
add x10, x7, x7   #add same num
add x6, x6, x6    #double t1
