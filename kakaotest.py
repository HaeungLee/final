import requests

url = "https://kauth.kakao.com/oauth/token"
data = {
    "grant_type": "authorization_code",
    "client_id": "58abcb4c758b2ac22d34496f4a506894",  # 카카오 디벨로퍼스의 REST API 키
    "redirect_uri": "https://localhost:3000",  # redirect_url → redirect_uri
    "code": "gqYsANmZosMP7H4RH11l1-rMpF1oF5bU3zsS-OW0zqqudnCC4lEdIwAAAAQKDQ0hAAABl4ryLdrmTYKY7N6ACw",  # 카카오 로그인 후 받은 authorization code
    "client_secret": "zWzdkvcAdZP0Se7WtJVMYE7GRa4pVToC"  # 필요한 경우만 (보안 탭에서 활성화했다면)
}
#https://kauth.kakao.com/oauth/authorize?client_id={REST_API_키}&redirect_uri=https://localhost:3000&response_type=code&scope=talk_message 실행
response = requests.post(url, data=data)
tokens = response.json()
print(tokens)
