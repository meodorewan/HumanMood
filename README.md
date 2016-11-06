import urllib
import httplib2
import sys
import os
import csv

sys.path.append(os.path.abspath('..'))

from configure.env import LIST_TABLES

def run(command, body):
    url = "http://localhost:8080/HMServlet";
    
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'Connection': 'close'
               }
    
    body = {
        'command' : command,
        'data': body,
    }

    response, content = httplib2.Http().request(
        url, 'POST',
        headers=headers,
        body=urllib.urlencode(body))
    
    print response
    print content

def pearson():
    params = {
        'user' : 'user001',
        'attribute-1': 'MoodEA',
        'attribute-2': 'HR_avg'
    }

    run('pearson', params);

def k_nearest_neighbor():
    params = {
        'user' : 'user001',
        'prediction': 'EA',
        'num_clusters': '4',
        'ratio': '0.7',
        'parameters': 'HR_min,HR_avg,HR_max'
    }
    run('k-nearest-neighbor', params)

if __name__ == "__main__":

    k_nearest_neighbor()
    pearson()



