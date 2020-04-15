import  sys, json, requests, time, uuid, hmac, hashlib, base64
BASE_URL = 'http://localhost:9756'
API_TOKEN = 'sLNC3zzvyFBtQVbp6wQYv989gTOrUNyN'
API_SECRET = 'GAlzh4EDmWzpNUHJCXieXA83RNuVHvTk'
VpnUrl='https://devops.bit-erp.ru'
OrgToken='5c11dc1e8192b6020be10eb3'
SsoType='Local'
def auth_request(method, path, headers=None, data=None):
    auth_timestamp = str(int(time.time()))
    auth_nonce = uuid.uuid4().hex
    auth_string = '&'.join([API_TOKEN, auth_timestamp, auth_nonce,
        method.upper(), path])
    auth_signature = base64.b64encode(hmac.new(
        API_SECRET, auth_string, hashlib.sha256).digest())
    auth_headers = {
        'Content-Type': 'application/json',
        'Auth-Token': API_TOKEN,
        'Auth-Timestamp': auth_timestamp,
        'Auth-Nonce': auth_nonce,
        'Auth-Signature': auth_signature,
    }
    if headers:
        auth_headers.update(headers)
    return getattr(requests, method.lower())(
        BASE_URL + path,
        headers=auth_headers,
        data=data,
    )


def FindUser(userName):
    response = auth_request('GET','/user/'+OrgToken)
    assert(response.status_code == 200)
    type = 'client'
    data = response.json()
    for value in data:
        if value['type'] == type and value['name'] == userName:
            response = auth_request('GET','/key/'+OrgToken+'/'+ value['id'])
            return response.json()

def ResetLink(userName):
    response = auth_request('GET','/user/'+OrgToken)
    assert(response.status_code == 200)
    type = 'client'
    data = response.json()
    for value in data:
        if value['type'] == type and value['name'] == userName:
            response = auth_request('GET','/key/'+OrgToken+'/'+ value['id'])
            parse_response=response.json()
            response=parse_response['view_url']
            print (VpnUrl+response)

def UserAdd (user, mail):
    response = auth_request('POST',
      '/user/'+OrgToken,
      headers={
          'Content-Type': 'application/json',
      },
      data=json.dumps({
        'name': user,
        'email': mail,
        'sso': SsoType,
        'disabled': False,
        'otp_auth': True,
        'auth_type': SsoType,
      }),
    )
    assert(response.status_code == 200)
    parse_response = response.json()
    response = auth_request('GET', '/key/'+OrgToken+'/'+ parse_response[0]['id'])
    parse_response=response.json()
    response=parse_response['view_url']
    print (VpnUrl+response)


def UserBlock (user):
    response = auth_request('GET','/user/'+OrgToken,)
    assert(response.status_code == 200)
    type = 'client'
    data = response.json()
    for value in data:
        if value['type'] == type and value['name'] == user:
                response = auth_request('PUT','/user/'+OrgToken+'/'+ value['id'],
                headers={ 'Content-Type': 'application/json',},
                data=json.dumps({'disabled': True,}),
                )
                resp = response.json()

if sys.argv[1] == 'add':
    if not FindUser(sys.argv[2]):
        UserAdd (sys.argv[2], sys.argv[3])
    else:
        print ('Duplicate username')
elif sys.argv[1] == 'resetlink':
    ResetLink (sys.argv[2])
elif sys.argv[1] == 'block':
        UserBlock (sys.argv[2])
else:
    print ('Action param error')
