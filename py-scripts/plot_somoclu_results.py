import os
import sys
import numpy as np
from matplotlib import pyplot as plt

if __name__ == '__main__':
    training_file_path = sys.argv[1]
    som_bm_file_path = sys.argv[2]

    # get class labels
    labels = []
    with open(training_file_path, 'r') as training_file:
        for line in training_file.readlines():
            labels.append(
                0 if line.split(' ')[0] == '-1' else 1
            )
    # get vector positions
    points = np.zeros((len(labels), 2))
    point_hit_count = {}
    with open(som_bm_file_path, 'r') as som_bm_file:
        for line in som_bm_file.read().split('\n'):
            if not line or line[0] == '%':
                continue
            i, x, y = line.split(' ')
            i, x, y, = int(i), int(x), int(y)
            points[i,:] = (x, y)
            if (x, y) not in point_hit_count:
                point_hit_count[(x, y)] = [0, 0]
            point_hit_count[(x, y)][labels[i]] += 1
    # draw scatter plot
    plt.scatter(
        points[:, 0],
        points[:, 1],
        c=map(lambda l: 'r' if l == 0 else 'b', labels)
    )

    # draw bar chart
    for k in point_hit_count:
        print k, ' = ', point_hit_count[k]

    plt.show()
