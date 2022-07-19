import os
import shutil

d = 'Oracle'
for sub in os.listdir(d):
    p = os.path.join(d,sub)
    for ss in os.listdir(p):
        if ss.endswith('Oracle.txt'):
            pp = os.path.join(p,ss)
            os.remove(pp)