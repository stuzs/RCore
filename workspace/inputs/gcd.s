    addi x1, x0, 14
    addi x2, x0, 35
    blt x2, x1, xdiv
ydiv:
    sub x2, x2, x1
    beq x2, x0, end1
    blt x2, x1, xdiv
    bge x2, x1, ydiv
xdiv:
    sub x1, x1, x2
    blt x2, x1, xdiv
    bge x2, x1, ydiv
end1:
    add x8, x1, x0
    addi x9, x0, 1
