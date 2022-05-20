#! /usr/bin/env python3

import random
import os

dir_name = os.environ["CHISEL_PROJ"] + '/src/test/'
# i.e,, directory name = "$CHISEL_PROJ/src/test/"
seq = [i for i in range(1,10000)] 
a = random.sample(seq,8) 
# i.e., take 8 positive random integers < 10000 in a

for index in range(len(a)):
 a[index] = hex(a[index])

for index in range(len(a)):
 a[index] = a[index].strip('0x')

for index in range(len(a)):
 a[index] = a[index].zfill(8)

print(a)

for i in range(24):
 a.insert(0, '00000000')

print('Bubble sorting random number set is generated successfully!')

r ="\n".join(str(i) for i in a)
with open(dir_name+'data_init.pat', 'w+') as wr: 
    wr.write(r)

print('Memory data file is generated successfully!')
