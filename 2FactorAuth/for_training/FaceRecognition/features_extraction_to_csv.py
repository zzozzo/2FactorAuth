# Extract features from images and save into "features_all.csv"

import os
import dlib
from skimage import io
import csv
import numpy as np
import glob
import gather_examples as gater


#csv디렉토리안에 파일 의미 : new는 새로 만든 csv파일, merge는 new와 merge를 합친 파일

# Path of cropped faces
path_images_from_camera = "data/data_faces_from_camera/"+gater.date+"/"

# Use frontal face detector of Dlib
detector = dlib.get_frontal_face_detector()

# Get face landmarks
predictor = dlib.shape_predictor('data/data_dlib/shape_predictor_68_face_landmarks.dat')

# Use Dlib resnet50 model to get 128D face descriptor
face_reco_model = dlib.face_recognition_model_v1("data/data_dlib/dlib_face_recognition_resnet_model_v1.dat")


# Return 128D features for single image
# Input:    path_img           <class 'str'>
# Output:   face_descriptor    <class 'dlib.vector'>
def return_128d_features(path_img):
    img_rd = io.imread(path_img)
    faces = detector(img_rd, 1)

    print("%-40s %-20s" % ("Image with faces detected:", path_img), '\n')

    # For photos of faces saved, we need to make sure that we can detect faces from the cropped images
    if len(faces) != 0:
        shape = predictor(img_rd, faces[0])
        face_descriptor = face_reco_model.compute_face_descriptor(img_rd, shape)
    else:
        face_descriptor = 0
        print("no face")
    return face_descriptor


#폴더에 있는 사진의 feature를 추출하여 CSV에 저장
def return_features_mean_personX(path_faces_personX):
    features_list_personX = []
    photos_list = os.listdir(path_faces_personX)
    if photos_list:
        for i in range(len(photos_list)):
            # return_128d_features()를 이용하여 128d feature 획득
            print("%-40s %-20s" % ("Reading image:", path_faces_personX + "/" + photos_list[i]))
            features_128d = return_128d_features(path_faces_personX + "/" + photos_list[i])
            # 이미지에 얼굴이 감지되지 않을 경우 해당 안됨
            if features_128d == 0:
                i += 1
            else:
                features_list_personX.append(features_128d)
    else:
        print("Warning: No images in " + path_faces_personX + '/', '\n')

    # 128d feature 벡터의 평균 계산
    if features_list_personX:
        features_mean_personX = np.array(features_list_personX).mean(axis=0)
    else:
        features_mean_personX = np.zeros(128, dtype=int, order='C')
    return features_mean_personX

person_list=[]
for person in gater.file_list:
    person_list.append(person[:-4])

#여러명 read
with open("data/csv/new.csv", "w", newline="") as csvfile:
    writer = csv.writer(csvfile)
    for person in person_list:
        # Get the mean/average features of face/personX, it will be a list with a length of 128D
        print(path_images_from_camera + person)
        features_mean_personX = return_features_mean_personX(path_images_from_camera + person)
        # 여기에 모든 1열에 사번추가하는 코드 추가
        list = []
        list.append(int(person))  # user_num을 숫자로 넣기
        list.extend(features_mean_personX)
        writer.writerow(list)
        del list[:]
    print("Save all the features of faces registered into: data/features_all.csv")

#한명만 read
# with open("data/csv/new7.csv", "w", newline="") as csvfile:
#     writer = csv.writer(csvfile)
#
#     features_mean_personX = return_features_mean_personX(path_images_from_camera +str(user_num)) #"person_3"에 사번넣기
#     #features_mean_personX = return_features_mean_personX(path_images_from_camera + "person_3")
#     #여기에 모든 1열에 사번추가하는 코드 추가
#     #list=[20172183] #사번
#     list=[]
#     list.append(int(user_num)) #user_num을 숫자로 넣기
#     list.extend(features_mean_personX)
#     writer.writerow(list)


path = 'data/csv/' #CSV 파일이 존재하는 경로
merge_path = 'data/csv/merge.csv' #최종Merge file

#여러 파일 읽을 때
#file_list = glob.glob(path + '*') #1. merge 대상 파일을 확인
# with open(merge_path, 'w') as f: #2-1.merge할 파일을 열고
#     writer = csv.writer(f)
#     for file in file_list:
#         with open(file ,'r') as f2:
#             while True:
#                 line = f2.readline() #2.merge 대상 파일의 row 1줄을 읽어서
#
#                 if not line: #row가 없으면 해당 csv 파일 읽기 끝
#                     break
#
#                 f.write(line) #3.읽은 row 1줄을 merge할 파일에 쓴다.
#             file_name = file.split('\\')[-1]
#             print(file.split('\\')[-1] + ' write complete...')
#
#     print('>>> All file merge complete...')

file='data/csv\\new.csv'
with open(merge_path, 'a') as f: #2-1.merge할 파일을 열고 쓰기,a는 파일에 있는 내용 이어쓰기
    writer = csv.writer(f)
    with open(file ,'r') as f2:
        while True:
            line = f2.readline() #2.merge 대상 파일의 row 1줄을 읽어서

            if not line: #row가 없으면 해당 csv 파일 읽기 끝
                break

            f.write(line) #3.읽은 row 1줄을 merge할 파일에 쓴다.
        file_name = file.split('\\')[-1]
        print(file.split('\\')[-1] + ' write complete...')

    print('>>> All file merge complete...')
