import sys
import os
import struct
from datetime import datetime
import pytz

JAVA_LONG_SIZE = 8
JAVA_DOUBLE_SIZE = 8

SENSOR_DATA_LEN = {
    'accelerometer': 3, # note muse's accelerometer data is in milli-Gs
    'blink': 0, # only timestamps of blink event
    'eeg': 4,
    'horseshoe': 4
}

'''
session_path: root directory of a session. e.g. "muse_data_1424380145647"
data_type: name of the data channel you want to read. "accelerometer", "eeg" etc.

returns a 2D array where rows are data captured at discrete time intervals
    in each row, the first column is the timestamp and the rest is the data
'''
def readData(session_path, data_type):
    data = []
    with open(os.path.join(session_path, data_type), 'rb') as f:
        time_bytes = f.read(JAVA_LONG_SIZE)
        while time_bytes:
            data_row = []
            data_row.extend(struct.unpack('>q', time_bytes))
            data_bytes = f.read(JAVA_DOUBLE_SIZE*SENSOR_DATA_LEN[data_type])
            data_row.extend(struct.unpack('>'+'d'*SENSOR_DATA_LEN[data_type], data_bytes))
            data.append(data_row)
            time_bytes = f.read(JAVA_LONG_SIZE)
    return data

def timestampToDatetime(t):
    # t should contain "seconds" since epoch
    # note that timestamps coming from muse are "microseconds" since epoch
    # while android timestamps are "milliseconds" since epoch
    tz = pytz.timezone('US/Eastern')
    d = datetime.fromtimestamp(t, tz)
    return d

