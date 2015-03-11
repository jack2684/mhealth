import sys
import os
import glob
import numpy as np
from scipy.signal import welch
import pywt
import muse_store

DATA_ROOT = '/home/mridul/mHealth/project/mHealth_data'
ACTIVITIES = ['Browsing', 'MathProblem']
WINDOW_SIZE = 2 # seconds

if __name__ == '__main__':
    subject_name = sys.argv[1]
    data_paths = []
    for activity in ACTIVITIES:
        activity_data_path = os.path.join(
            DATA_ROOT, activity, subject_name)
        if not os.path.exists(activity_data_path):
            print 'Could not find {}'.format(activity_data_path)
            sys.exit(42)
        dir_name = glob.glob(os.path.join(activity_data_path, 'muse*'))
        if not dir_name:
            print 'Failed to find data in {}'.format(activity_data_path)
            sys.exit(42)
        else:
            dir_name = dir_name[0]
        data_paths.append(os.path.join(activity_data_path, dir_name))

    all_data = []
    out_file = open(os.path.join('../features', subject_name), 'w')
    for data_path in data_paths:
        print '    ', data_path
        eeg_data = muse_store.read_data(data_path, 'eeg')
        time_stamps = map(lambda r: r[0], eeg_data)
        eeg_data = np.array(map(lambda r: r[1:], eeg_data), dtype=np.float64)
        print 'Session duration: {} minutes'.format(
            (muse_store.timestamp_to_datetime(time_stamps[-1] / 10.**6) - muse_store.timestamp_to_datetime(time_stamps[0] / 10.**6)).total_seconds() / 60.
        )
        # split into two second windows
        sampling_rate = muse_store.average_sampling_rate(map(
            lambda t: muse_store.timestamp_to_datetime((float(t)) / 10**6), time_stamps
        ))
        print 'Average EEG sampling rate: {}'.format(sampling_rate)

        window_start_indices = np.arange(
            0, eeg_data.shape[0] - 2*sampling_rate, 2*sampling_rate
        )
        print 'Number of samples: {}, Number of windows: {}'.format(
            eeg_data.shape[0], window_start_indices.shape[0]
        )
        for window_start_index in window_start_indices:
            fft_nperseg = 256
            features_per_channel = fft_nperseg / 2 + 1
#            features_per_channel = 128
            window_features = np.zeros(features_per_channel*4)
            for channel in range(4):
                f, pxx = welch(
                    eeg_data[window_start_index:window_start_index + sampling_rate*WINDOW_SIZE, channel],
                    fs=220, nperseg=fft_nperseg, noverlap=128
                )
                window_features[channel*features_per_channel:(channel + 1)*features_per_channel] = pxx
#                haar_coefficients = []
#                for coeffs in pywt.wavedec(eeg_data[window_start_index:window_start_index + 256, channel], 'haar')[:-1]:
#                    haar_coefficients.extend(coeffs)
#                window_features[channel*features_per_channel:(channel + 1)*features_per_channel] = \
#                    haar_coefficients
            out_file.write(
                ('-1 ' if os.path.join(DATA_ROOT, ACTIVITIES[0]) in data_path else '+1 ') +
                ' '.join(map(lambda t: str(t[0]) + ':' + str(t[1]), enumerate(window_features))) +
                '\n'
            )
            all_data.append([window_features, -1 if os.path.join(DATA_ROOT, ACTIVITIES[0]) in data_path else 1])
    np.save('../features/{}'.format(subject_name), all_data)
    out_file.close()

