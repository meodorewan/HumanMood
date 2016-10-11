import urllib
import httplib2
import sys
import os
import csv

sys.path.append(os.path.abspath('..'))

from configure.env import LIST_TABLES

def run(command, body, users):
    url = "http://localhost:8080/HMServlet";
    
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'Connection': 'close'
               }
    
    body = {
        'command' : command,
        'data': body,
        'users': users
    }

    response, content = httplib2.Http().request(
        url, 'POST',
        headers=headers,
        body=urllib.urlencode(body))
    
    print response

if __name__ == "__main__":
    run('naive-bayes', 'HR_min,HR_max', 'user001')
    # cnt = 0
    # with open('../' + LIST_TABLES , 'rb') as lt:
    #     for path in lt:
    #         with open('../resources/' + path.replace('\n', '').replace(' ', '') , 'rb') as f:
    #             reader = csv.DictReader(f)
    #             for row in re(new Double(dNew)).toString()ader:
    #                 cnt += 1
    #                 run('update', row)
    # print cnt