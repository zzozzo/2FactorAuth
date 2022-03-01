#데이터베이스인 Firebase의 realtime database와 storage에 접근하여 Face Recognition 기능에 필요한 사용자 얼굴 영상 가져오기

import pyrebase
import os
import json
from requests.packages.urllib3.packages.six.moves import urllib
import time

config={
    "apiKey": "AIzaSyB5r_ChQiHWSjUwuywo-31g6cmzxW-LT9o",
    "authDomain": "twofactor-d19ed.firebaseapp.com",
    "databaseURL": "https://twofactor-d19ed.firebaseio.com",
    "projectId": "twofactor-d19ed",
    "storageBucket": "twofactor-d19ed.appspot.com",
    "messagingSenderId": "415712167872",
    "appId": "1:415712167872:web:9369b71a20494aba992a25",
    "measurementId": "G-93466FBBFV"
}

firebase=pyrebase.initialize_app(config)

#Firebase의 realtime database 연결
db=firebase.database()

#Firebase의 storage 연결
storage=firebase.storage()

#1. realtime database에서 training이 N인 사용자 식별번호를 companyNum_list에 저장하기
number=db.child("user").get()
dict=number.val()

companyNum_list=[]

for k,v in dict.items():
    if v.get('training')=='N':
        companyNum_list.append(v.get('companyNum'))


#2. storage에서 "등록날짜+식별변호"로 저장된 사용자 얼굴 영상(.mp4) 가져오기
#2-1. storage에 저장된 모든 영상목록을 json파일로받아오기 위해 url불러오기
url=storage.get_url(None)

#2-2. url 내용 str타입으로 가져오기
url_str=urllib.request.urlopen(url).read().decode('utf-8')

#2-3. str to json
url_json=json.loads(url_str)

# items에 있는 값 모두 가져오기
jsonArray=url_json.get("items")

jsonList=[]

#name에 있는 값 list에 모두 append
for list in jsonArray:
    jsonList.append(list.get("name"))

#폴더구성을 위한 현재 날짜(사용자 얼굴 영상 다운받는 날짜) 구하기
date=time.strftime('%Y-%m-%d')

#영상이 저장될 경로
recogVideo_path="C:\\Users\\sksms\\PycharmProjects\\combine1\\recogVideo"

#파일 생성
try:
    if not(os.path.isdir(recogVideo_path+date)):
        os.makedirs(os.path.join(recogVideo_path,date))
except OSError as e:
    print("Error:Creating directory."+date)

#영상(.mp4)을 이미지(.png)로 바꿔 저장할 경로
dataset_path="C:\\Users\\sksms\\PycharmProjects\\combine1\\data\\data_faces_from_camera"

trainingFile_list=[]
path_list=[]

#realtime database에서 training여부(영상 다운 여부)를 N에서 Y로 바꿔주고 storage에서 영상을 다운받는다.
for file_name in jsonList:
    for companyNum in companyNum_list:
        if file_name[9:-4]==companyNum:
            db.child("user").child(str(companyNum)).update({"training": "Y"})
            trainingFile_list.append(companyNum)
            path_list.append(file_name)
            storage.child(file_name).download("recogVideo/" +date+"/"+ companyNum + ".mp4")
            try:
                if not (os.path.isdir(dataset_path + date+companyNum)):
                    os.makedirs(os.path.join(dataset_path,date,companyNum))
            except OSError as e:
                print("Error:Creating directory." + date+companyNum)


#날짜와 다운받은 사용자 식별번호를 txt파일에 기록한다.
f=open("C:/Users/sksms/PycharmProjects/combine1/recogVideo/list.txt",'w')
f.write(date+"\n")
for file in trainingFile_list:
    f.write(file+".mp4\n")

f.close()

#다음으로 gather_example.py파일 실행

