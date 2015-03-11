from sklearn.cross_validation import LeaveOneOut
from sklearn.neighbors import NearestNeighbors
from sklearn import preprocessing
from scipy.stats.mstats import mode
import sys
import numpy as np

if __name__ == '__main__':
    subject_name = sys.argv[1]
    print subject_name
    f = np.load('../features/{}.npy'.format(subject_name))
    correct_count = 0
    for train, test in LeaveOneOut(len(f)):
        train_data = [r for r in f[train][:, 0]]
        train_labels = f[train][:, 1].astype(np.int)
        test_data = [r for r in f[test][:, 0]]
        test_label = f[test][:, 1].astype(np.int)
#        scaler = preprocessing.StandardScaler().fit(train_data)
#        train_data = scaler.transform(train_data)
#        test_data = scaler.transform(test_data)
        nbrs = NearestNeighbors(
            n_neighbors=5,
            algorithm='kd_tree'
        ).fit(train_data)
        dist, indices = nbrs.kneighbors(test_data)
        predicted_test_label, _ = mode(train_labels[indices[0]])
        predicted_test_label = int(predicted_test_label[0])
        if predicted_test_label == test_label:
            correct_count += 1
    print "Correctly predicted {} out of {}".format(
        correct_count, len(f))

