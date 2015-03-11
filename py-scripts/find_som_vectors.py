import sys
import numpy as np
from collections import defaultdict

if __name__ == '__main__':
    f = np.load('../features/Donglin.npy')
    wts = []
    lines = open('../somoclu/Donglin.wts', 'r').read().split('\n')
    lines = lines[2:-1]

    for l in lines:
        try:
            wts.append(map(lambda v: float(v), l.strip().split(' ')))
        except ValueError:
            print 'Failed to read all weights'
            sys.exit(1)
    
    d = defaultdict(lambda: np.zeros(2, dtype=np.int))

    wts = np.array(wts, dtype=np.float64)
    best_match_indices = np.zeros(len(f), dtype=np.int)
    for i in range(len(f)):
        distances = np.sum((np.tile(f[i][0], (wts.shape[0], 1)) - wts)**2, axis=1)
        best_match_indices[i] = min(enumerate(distances), key=lambda v: v[1])[0]
        d[best_match_indices[i]][f[i][1] if f[i][1] == 1 else 0] += 1
    print set(best_match_indices)
    print d

