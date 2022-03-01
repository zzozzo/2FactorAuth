#얼굴 영상에서 이미지파일 추출하기

# import the necessary packages
import numpy as np
import argparse
import cv2
import os

#firebase_connect.py에서 만든 txt파일을 읽어와 사용자 식별번호,날짜를 알아옴.
f=open("C:/Users/sksms/PycharmProjects/combine1/recogVideo/list.txt","r")
lines=f.read().splitlines()
f.close()
file_list=lines[1:] #번호+.mp4
date=lines[0]

#사용자 식별번호 갯수만틈 for문을 돌며 영상을 읽어 이미지 파일로 만든다.
for trainingFile in file_list:
	input_path="recogVideo/"+date+"/"+trainingFile
	output_path="data/data_faces_from_camera/"+date+"/"+trainingFile[0:-4]

	# 파라메터 구문 분석
	ap = argparse.ArgumentParser()
	ap.add_argument("-i", "--input", type=str, default=input_path, help="path to input video")
	ap.add_argument("-o", "--output", type=str, default=output_path, help="path to output directory of cropped faces")
	ap.add_argument("-d", "--detector", type=str,default="face_detector", help="path to OpenCV's deep learning face detector")
	ap.add_argument("-c", "--confidence", type=float, default=0.5, help="minimum probability to filter weak detections")
	ap.add_argument("-s", "--skip", type=int, default=3, help="# of frames to skip before applying face detection")
	args = vars(ap.parse_args())

	# 얼굴 탐지기 로딩
	print("[INFO] loading face detector...")
	protoPath = os.path.sep.join([args["detector"], "deploy.prototxt"])
	modelPath = os.path.sep.join([args["detector"], "res10_300x300_ssd_iter_140000.caffemodel"])
	net = cv2.dnn.readNetFromCaffe(protoPath, modelPath)

	# 비디오 파일 스트림 초기화
	vs = cv2.VideoCapture(args["input"])
	read = 0
	saved = 1

	# 비디오 파일 스트림 프레임 반복
	while True:
		# 파일에서 비디오 스트림 프레임 입력
		(grabbed, frame) = vs.read()

		# 더이상 프레임이 없으면 루프 탈출
		if not grabbed:
			break

		# 프레임수 증가
		read += 1

		# 프레임을 처리해야 하는지 확인
		if read % args["skip"] != 0:
			continue

		# 프레임에서 blob 구성
		(h, w) = frame.shape[:2]
		blob = cv2.dnn.blobFromImage(cv2.resize(frame, (300, 300)), 1.0, (300, 300), (104.0, 177.0, 123.0))

		# 입력된 이미지에서 얼굴을 인식하기 위해 OpenCV의 딥러닝 기반 얼굴 탐지기 이용
		net.setInput(blob)
		detections = net.forward()

		# 적어도 하나의 얼굴이 발견되었는지 확인
		if len(detections) > 0:
			# 각 이미지가 하나의 얼굴만을 가지고 있다고 가정하고, 가장 큰 확률을 가진 경계 상자를 찾음
			i = np.argmax(detections[0, 0, :, 2])
			confidence = detections[0, 0, i, 2]

			# 확률이 가장 큰 탐지는 최소 확률 테스트를 의미
			if confidence > args["confidence"]:
				# 얼굴 경계 상자의 (x,y) 좌표 계산하고 얼굴 ROI 추출
				box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
				(startX, startY, endX, endY) = box.astype("int")
				face = frame[startY:endY, startX:endX]

				# 프레임 쓰기
				p = os.path.sep.join([args["output"], "{}.png".format(saved)])
				# 반시계방향으로 회전시켜 이미지를 올바르게 저장
				img_rotate = cv2.rotate(face, cv2.ROTATE_90_COUNTERCLOCKWISE)
				img=cv2.imwrite(p, img_rotate)
				saved += 1
				print("[INFO] saved {} to disk".format(p))

	# cleaning
	vs.release()
	cv2.destroyAllWindows()