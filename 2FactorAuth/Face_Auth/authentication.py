#인증 구현 코드(raspberrypi) :Face Recognition,Liveness Detection, 눈깜빡임 탐지
import dlib
import pandas as pd
from tensorflow.keras.preprocessing.image import img_to_array
from tensorflow.keras.models import load_model
import numpy as np
import imutils
from imutils import face_utils
import pickle
import time
import cv2
import os
from PIL import Image, ImageDraw, ImageFont
from subprocess import call
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
from gpiozero import LED
from time import sleep

# Dlib 정방향 얼굴 인식 detector
detector = dlib.get_frontal_face_detector()

#Dlib 얼굴 landmark 검출기
predictor = dlib.shape_predictor('data/data_dlib/shape_predictor_68_face_landmarks.dat')

#Dlib Resnet 얼굴 인식 모델，128D 특징 벡터 추출
face_reco_model = dlib.face_recognition_model_v1("data/data_dlib/dlib_face_recognition_resnet_model_v1.dat")

#caffe 얼굴 탐지기 로딩
protoPath = os.path.join("face_detector", "deploy.prototxt")
modelPath = os.path.join("face_detector", "res10_300x300_ssd_iter_140000.caffemodel")
net = cv2.dnn.readNetFromCaffe(protoPath, modelPath)

# 진짜 얼굴 탐지 모델 및 레이블 로딩
model = load_model("liveness.model")
le = pickle.loads(open("le.pickle", "rb").read())

# load OpenCV's Haar cascade for face detection (which is faster than
# dlib's built-in HOG detector, but less accurate), then create the facial landmark predictor
print("[INFO] loading facial landmark predictor...")
detector_eye = cv2.CascadeClassifier("haarcascade_frontalface_default.xml")


# grab the indexes of the facial landmarks for the left and right eye, respectively
(lStart, lEnd) = face_utils.FACIAL_LANDMARKS_IDXS["left_eye"]
(rStart, rEnd) = face_utils.FACIAL_LANDMARKS_IDXS["right_eye"]

#Firebase DB connection
#cred = credentials.Certificate('/home/pi/raspberry_json/twofactor-d19ed-firebase-adminsdk-ml5l3-a57e510744.json')
cred = credentials.Certificate('/home/pi/raspberry_json/otfproject-308b5-firebase-adminsdk-8s0dj-eaa5edbbd2.json')
firebase_admin.initialize_app(cred)
db = firestore.client()
doc_ref = db.collection(u'TwoFactor').document(u'RaspberryPi')


def euclidean_dist(ptA, ptB):
    # compute and return the euclidean distance between the two points
    return np.linalg.norm(ptA - ptB)


def eye_aspect_ratio(eye):
    # compute the euclidean distances between the two sets of vertical eye landmarks (x, y)-coordinates
    A = euclidean_dist(eye[1], eye[5])
    B = euclidean_dist(eye[2], eye[4])

    # compute the euclidean distance between the horizontal eye landmark (x, y)-coordinates
    C = euclidean_dist(eye[0], eye[3])

    # compute the eye aspect ratio
    ear = (A + B) / (2.0 * C)

    # return the eye aspect ratio
    return ear

def firebase_store_set(company,faceResult):

    doc_ref.set({
        u'company': company,
        u'faceResult': faceResult,
        u'OTPResult':0 #fail
    })
    


def firebase_store_get():

    doc = doc_ref.get()
    print(u'Document data: {}'.format(doc.to_dict()))


    OTPResult = doc.to_dict()
    OTPValue = OTPResult['OTPResult']

    return OTPValue


def LEDResult(value):
    red_led = LED(15)
    green_led = LED(18)

    if value==1: #1==성공 GREEN
        for i in range(0,5):
            red_led.off()
            green_led.on()
            sleep(1)
    elif value==0: #0==실패 RED
        for i in range(0,5):
            red_led.on()
            green_led.off()
            sleep(1)

def view_notice(title,sec):
    img = cv2.imread(title, cv2.IMREAD_REDUCED_COLOR_4)
    resize_img=cv2.resize(img, (800, 400))
    cv2.imshow("notice", resize_img)
    cv2.waitKey(sec)
    cv2.destroyAllWindows()

class Face_Register:
    def __init__(self):
        self.font = cv2.FONT_ITALIC

        # FPS
        self.frame_time = 0
        self.frame_start_time = 0
        self.fps = 0

        # list to save centroid positions of ROI in frame N-1 and N
        self.last_frame_centroid_list = []
        self.current_frame_centroid_list = []

        # list to save names of ROI in frame N-1 and N
        self.last_frame_names_list = []
        self.current_frame_face_names_list = []

        # cnt for faces in frame N-1 and N
        self.last_frame_faces_cnt = 0
        self.current_frame_faces_cnt = 0

        # Save the features of faces in the database
        self.features_known_list = []

        self.e_distance_list = []

        # Save the name of faces known
        self.name_known_cnt = 0
        self.name_known_list = []

        # Save the positions and names of current faces captured
        self.current_frame_face_pos_list = []
        self.current_frame_face_features_list = []


        # Compute the e-distance between two 128D features

    def get_face_database(self):
        if os.path.exists("data/csv/merge.csv"):
            path_features_known_csv = "data/csv/merge.csv"
            csv_rd = pd.read_csv(path_features_known_csv, header=None)
            # 2. Print known faces
            for i in range(csv_rd.shape[0]):
                features_someone_arr = []
                for j in range(0, 129):
                    if (j == 0):
                        self.name_known_list.append(int(csv_rd.iloc[i][0]))
                    elif csv_rd.iloc[i][j] == '':
                        features_someone_arr.append('0')
                    else:
                        features_someone_arr.append(csv_rd.iloc[i][j])
                self.features_known_list.append(features_someone_arr)
                # self.name_known_list.append("Person_" + str(i + 1))
            name_known_cnt = len(self.name_known_list)
            print("Faces in Database：", len(self.features_known_list))
            return 1
        else:
            print('##### Warning #####', '\n')
            print("'features_all.csv' not found!")
            print(
                "Please run 'get_faces_from_camera.py' and 'features_extraction_to_csv.py' before 'face_reco_from_camera.py'",
                '\n')
            print('##### End Warning #####')
            return 0

    # Get the fps of video stream
    def update_fps(self):
        now = time.time()
        self.frame_time = now - self.frame_start_time
        self.fps = 1.0 / self.frame_time
        self.frame_start_time = now
        # Compute the e-distance between two 128D features

    @staticmethod
    def return_euclidean_distance(feature_1, feature_2):
        feature_1 = np.array(feature_1)
        feature_2 = np.array(feature_2)
        dist = np.sqrt(np.sum(np.square(feature_1 - feature_2)))
        return dist

    # cv2 window putText on cv2 window
    def draw_note(self, img_rd):
        # Add some statements
        cv2.putText(img_rd, "Loading...", (20, 40), self.font, 1, (255, 0, 0), 1, cv2.LINE_AA)



    def process(self, stream):
        # 1. read data of known faces from csv
        if self.get_face_database():
            while stream.isOpened():
                print(">>>>>>>>>>> Frame X starts...")
                flag, img_rd = stream.read()
                kk = cv2.waitKey(1)
                # 2. detect faces for frame X
                faces = detector(img_rd, 0)

                # 3. update cnt for faces in frames
                self.last_frame_faces_cnt = self.current_frame_faces_cnt
                self.current_frame_faces_cnt = len(faces)
                print("     >>>>>> current_frame_faces_cnt:                      ", self.current_frame_faces_cnt)

                # 4. if cnt not changes, 1->1 or 0->0
                if self.current_frame_faces_cnt == self.last_frame_faces_cnt:
                    print("     >>>>>> scene 1: no faces cnt changes in this frame!!!")
                    # one face in this frame
                    if self.current_frame_faces_cnt != 0:
                        # 4.1 get ROI positions
                        for k, d in enumerate(faces):
                            # Compute the size of rectangle box
                            height = (d.bottom() - d.top())
                            width = (d.right() - d.left())
                            hh = int(height / 2)
                            ww = int(width / 2)

                            cv2.rectangle(img_rd,
                                          tuple([d.left() - ww, d.top() - hh]),
                                          tuple([d.right() + ww, d.bottom() + hh]),
                                          (255, 255, 255), 2)

                            # self.current_frame_face_pos_list[k] = tuple(
                            #     [faces[k].left(), int(faces[k].bottom() + (faces[k].bottom() - faces[k].top()) / 4)])

                            print("     >>>>>> self.current_frame_face_names_list[k]:        ",
                                  self.current_frame_face_names_list[k])
                            print("     >>>>>> self.current_frame_face_pos_list[k]:          ",
                                  self.current_frame_face_pos_list[k])



                            if self.current_frame_face_names_list[k] in self.name_known_list:
                                # cv2.destroyAllWindows()
                                name_state = self.current_frame_face_names_list[k]

                                #Face Recognition Success
                                if name_state != "unknown":
                                    print("Face Recognition Success!")

                                    #Liveness 모델
                                    (h, w) = img_rd.shape[:2]
                                    blob = cv2.dnn.blobFromImage(cv2.resize(img_rd, (300, 300)), 1.0, (300, 300),
                                                                 (104.0, 177.0, 123.0))

                                    # blob 이미지를 통해 OpenCV의 딥러닝 기반 얼굴 탐지기 이용하여 탐지 및 예측 진행
                                    net.setInput(blob)
                                    detections = net.forward()

                                    # 탐지 반복
                                    for i in range(0, detections.shape[2]):
                                        # 예측과 관련된 신뢰도(확률)를 추출
                                        confidence = detections[0, 0, i, 2]

                                        # 최소 확률 감지 임계값과 비교하여 계산된 확률이 최소 확률보다 큰지 확인
                                        if confidence > 0.5:
                                            # 얼굴 경계 상자의 (x, y) 좌표를 계산하고 얼굴 ROI 추출
                                            box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
                                            (startX, startY, endX, endY) = box.astype("int")

                                            # 감지된 경계 상자가 프레임의 치수를 벗어나지 않도록 주의
                                            startX = max(0, startX)
                                            startY = max(0, startY)
                                            endX = min(w, endX)
                                            endY = min(h, endY)

                                            # 얼굴 ROI를 추출한 다음 훈련 데이터와 정확히 동일한 방식으로 선행 처리
                                            face = img_rd[startY:endY, startX:endX]
                                            face = cv2.resize(face, (32, 32))
                                            face = face.astype("float") / 255.0
                                            face = img_to_array(face)
                                            face = np.expand_dims(face, axis=0)

                                            # 훈련된 진짜 얼굴 탐지기 모델을 통해 얼굴 ROI를 전달하여 얼굴이 진짜인지 가짜인지 확인
                                            preds = model.predict(face)[0]
                                            j = np.argmax(preds)
                                            label = le.classes_[j]

                                            if label == "fake":
                                                print("Liveness Detection authentication fail")
                                                firebase_store_set(name_state,0)
                                                
                                                LEDResult(0)
                                            elif label == "real":
                                                print("Liveness Detection authentication success")

                                                #눈깜빡임 탐지 기능
                                                # draw an alarm on the frame
                                                #cv2.putText(img_rd, "눈을 깜빡여주세요!", (10, 30),cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)

                                                EYE_AR_THRESH = 0.3

                                                COUNTER = 0

                                                gray = cv2.cvtColor(img_rd, cv2.COLOR_BGR2GRAY)

                                                # detect faces in the grayscale frame
                                                rects = detector_eye .detectMultiScale(gray, scaleFactor=1.1,
                                                                                  minNeighbors=5, minSize=(30, 30),
                                                                                  flags=cv2.CASCADE_SCALE_IMAGE)

                                                # loop over the face detections
                                                for (x, y, w, h) in rects:
                                                    # construct a dlib rectangle object from the Haar cascade bounding box
                                                    rect = dlib.rectangle(int(x), int(y), int(x + w), int(y + h))

                                                    # determine the facial landmarks for the face region, then
                                                    # convert the facial landmark (x, y)-coordinates to a NumPy array
                                                    shape = predictor(gray, rect)
                                                    shape = face_utils.shape_to_np(shape)

                                                    # extract the left and right eye coordinates, then use the
                                                    # coordinates to compute the eye aspect ratio for both eyes
                                                    leftEye = shape[lStart:lEnd]
                                                    rightEye = shape[rStart:rEnd]
                                                    leftEAR = eye_aspect_ratio(leftEye)
                                                    rightEAR = eye_aspect_ratio(rightEye)

                                                    # average the eye aspect ratio together for both eyes
                                                    ear = (leftEAR + rightEAR) / 2.0

                                                    # compute the convex hull for the left and right eye, then visualize each of the eyes
                                                    leftEyeHull = cv2.convexHull(leftEye)
                                                    rightEyeHull = cv2.convexHull(rightEye)
                                                    
                                                    # check to see if the eye aspect ratio is below the blink
                                                    # threshold, and if so, increment the blink frame counter
                                                    if ear < EYE_AR_THRESH:

                                                        #눈깜빡인 횟수 세기
                                                        COUNTER += 1

                                                        print("Authentication success")
                                                        #recog->real->눈깜빡임 성공 store에 저장하기
                                                        firebase_store_set(str(name_state), 1)
                                                        
                                                        LEDResult(1)
                                                        
                        
                                                        view_notice("image_notice/notice2.png",30000)
                                                        #sleep(40)
                                                        if str(firebase_store_get())=="1":
                                                            LEDResult(1)
                                                        else:
                                                            LEDResult(0)


                                                    # otherwise, the eye aspect ratio is not below the blink threshold, so reset the counter and alarm
                                                    else:
                                                        COUNTER = 0
                                                        #recog->real->실패
                                                        firebase_store_set(str(name_state),0)
                                                        print("Authentication fail")
                                                        LEDResult(0)

                        else:
                            print("Face Recognition fail!")
                            LEDResult(0)
                            #break

                # 5. if cnt of faces changes, 0->1 or 1->0
                else:
                    print("     >>>>>> scene 2: faces cnt changes in this frame")
                    self.current_frame_face_pos_list = []
                    self.e_distance_list = []

                    # 5.1 face cnt: 1->0, no faces in this frame
                    if self.current_frame_faces_cnt == 0:
                        print("     >>>>>> scene 2.1 no guy in this frame!!!")
                        # clear list of names and
                        self.current_frame_face_names_list = []
                        self.current_frame_face_features_list = []

                    # 5.1 face cnt: 0->1, get the new face
                    elif self.current_frame_faces_cnt == 1:
                        print("     >>>>>> scene 2.2 first guy in this frame!!!")
                        self.current_frame_face_names_list = []

                        for i in range(len(faces)):
                            shape = predictor(img_rd, faces[i])
                            self.current_frame_face_features_list.append(
                                face_reco_model.compute_face_descriptor(img_rd, shape))

                        # 5.1.1 Traversal all the faces in the database
                        for k in range(len(faces)):
                            self.current_frame_face_names_list.append("unknown")

                            # Positions of faces captured
                            self.current_frame_face_pos_list.append(tuple(
                                [faces[k].left(), int(faces[k].bottom() + (faces[k].bottom() - faces[k].top()) / 4)]))
                            print("     >>>>>> self.current_frame_face_features_list:         ",
                                  self.current_frame_face_features_list)
                         
                            # For every faces detected, compare the faces in the database

                            for i in range(len(self.features_known_list)):
                                
                                if str(self.features_known_list[i][0]) != '0.0':
                                    print("     >>>>>> with person", str(i + 1), "the e distance:                   ",
                                          end='')
                                    e_distance_tmp = self.return_euclidean_distance(
                                        self.current_frame_face_features_list[k],
                                        self.features_known_list[i])
                                    print(e_distance_tmp)
                                    self.e_distance_list.append(e_distance_tmp)
                                else:
                                    
                                    self.e_distance_list.append(999999999)

                            # Find the one with minimum e distance
                            similar_person_num = self.e_distance_list.index(min(self.e_distance_list))

                            if min(self.e_distance_list) < 0.4:
                                self.current_frame_face_names_list[k] = self.name_known_list[similar_person_num]

                            else:
                                print("     >>>>>> Unknown person")

                # 9. Add note on cv2 window
                self.draw_note(img_rd)

                # 10.Press 'q' to exit
                if kk == ord('q'):
                    break

                self.update_fps()
                cv2.namedWindow("camera", 1)
                cv2.imshow("camera", img_rd)

                # print(">>>>>>>>>>> Frame X ends...\n\n")

    def run(self):
        # cap = cv2.VideoCapture("head-pose-face-detection-female-and-male.mp4")
        cap = cv2.VideoCapture(0)
        cap.set(3,800)
        cap.set(4,480)
        final_result=self.process(cap)

        cap.release()
        cv2.destroyAllWindows()
        return final_result


def main():
    view_notice("image_notice/notice1.png",5000)
    Face_Register_con = Face_Register()
    final_run_result=Face_Register_con.run()

if __name__ == '__main__':
    main()
