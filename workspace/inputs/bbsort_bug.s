# bubble sorting demo, it is not easy coding in assembly
    addi x1, x0, 7    # x1 = 7, outer count
    addi x2, x0, 92   # x2 = 0x5c, the base number in DMEM
    addi x8, x0, 0    # count = 0, inner count
start:
    lw x5, 4(x2)      # x5 <= read mem1
    lw x6, 8(x2)      # x6 <= read mem2
    bge x6, x5, ge    # if x6 >= x5? no swap
lt: 
    sw x5, 8(x2)      # swap memory if x6<x5
    sw x6, 4(x2)
ge:
    addi x2, x2, 4    # move to next mem address
    addi x8, x8, 1    # ++count
    bge x1, x8, start # if inner count<=outer count, compare again
                      # if innter count>outer count, go to next
                      # i.e., x8 > x1, the last inner loop
end1:
    addi x1, x1, -1  # x1 = 7,6,5...1; for 7 times
    addi x2, x0, 92  # x2 = 0x5c to the beginning base again
    addi x8, x0, 0   # inner count reset
    blt x0, x1, start # outside loop again with x1 decreased
                      # until x1 == -1

